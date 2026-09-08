import express from "express";
import http from "http";

/*
  LOAD BALANCER - client -> LB (:8080) -> backend (:3001/:3002/:3003)
  Two jobs: background health checks, and per-request routing.
*/

const app = express();
const servers = [
  { host: "localhost", port: 3001, healthy: true },
  { host: "localhost", port: 3002, healthy: true },
  { host: "localhost", port: 3003, healthy: true },
];

let currentServerIndex = 0;

// health check
const checkHealth = async (server) => {
  try {
    const response = await fetch(`http://${server.host}:${server.port}/health`, { signal: AbortSignal.timeout(1000) }); // default timeout for fetch, otherwise it'll hung
    setHealth(server, response.ok);
  } catch {
    setHealth(server, false);
  }
};

function setHealth(server, health) {
  if (server.healthy === health) return;

  server.healthy = health;
  console.log(`${server.host}:${server.port} is ${health ? "UP" : "DOWN"}`);
}

// health check runs every 2 secs
setInterval(() => servers.forEach(checkHealth), 2000);
servers.forEach(checkHealth); // run once now, don't wait 2s for the first probe

// round robin algorithm
const getNextServer = (candidates) => {
  // return candidates[Math.floor(Math.random() * candidates.length)]; // random pick

  const target = candidates[currentServerIndex % candidates.length];
  currentServerIndex = (currentServerIndex + 1) % candidates.length;
  return target;
};

/*
  - filter servers
  - forwards/sends requests to the active servers
  - streams response back
  - handles faliure
*/
// runs once per request + once per retry.
// attemptedPorts = backends the request already tried. `healthy` is a stale 2s hint (as health check runs every 2s)that can flip back; attemptedPorts only grows, so retries always terminate.
const requestHandler = (req, res, attemptedPorts = new Set()) => {
  const candidates = servers.filter((item) => item.healthy && !attemptedPorts.has(item.port));

  if (candidates.length === 0) return res.status(503).send("no healthy backend server available");

  const targetServer = getNextServer(candidates);
  attemptedPorts.add(targetServer.port); // recorded at pick time, before the outcome is known
  console.log(`${req.method} ${req.originalUrl} -> ${targetServer.port}`);

  // outgoing request from LB to backend server. Writable stream.
  const upstream = http.request(
    {
      host: targetServer.host,
      port: targetServer.port,
      path: req.originalUrl,
      method: req.method,
      headers: { ...req.headers, host: `${targetServer.host}:${targetServer.port}` }, // only host rewritten
    },

    // Error before writeHead (connection refused, DNS fail) → headersSent === false → safe to retry, client saw nothing
    // Error after writeHead (died mid-body) → headersSent === true → skip the retry, connection just drops, client sees a truncated response
    (serverResponse) => {
      res.writeHead(serverResponse.statusCode, serverResponse.headers); // headersSent -> true | fires when backend HEADERS arrive, not the full body
      serverResponse.pipe(res);
    },
  );

  // err handling when LB encounter error for a server when its down(not healthy)
  upstream.on("error", () => {
    setHealth(targetServer, false);
    if (!res.headersSent) requestHandler(req, res, attemptedPorts);
  });

  if (req.readableEnded) upstream.end();
  else req.pipe(upstream);
};

// arrow wrapper required - app.use(requestHandler) passes express's `next` as the 3rd arg
app.use((req, res) => requestHandler(req, res));
app.listen(8080, () => console.log("LoadBalancer running on port 8080"));

/*
Run in terminal:
node server.js 3001
node server.js 3002
node server.js 3003
node loadbalancer.js

curl -s localhost:8080/health

// continuous traffic
while true; do curl -s localhost:8080/; echo; sleep 0.5; done

Demo: with traffic flowing, shutdown a backend server -> DOWN within 2s, rotation drops to survivors. Restart -> back in. Stop all three -> 503.

 Piping sends byte data from a readable stream straight into a writable one, handling flow and backpressure. req is readable, res is writable.
   readableStream.pipe(writableStream);
*/
