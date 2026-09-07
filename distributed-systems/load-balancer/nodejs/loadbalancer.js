import express from "express";
import http from "http";

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
setInterval(() => {
  (servers.forEach(checkHealth), 2000);
});
servers.forEach(checkHealth);

// round robin algorithm
const getNextServer = (candidates) => {
  // return candidates[Math.floor(Math.random() * candidates.length)]; // random pick

  const target = candidates[currentServerIndex % candidates.length];
  currentServerIndex = (currentServerIndex + 1) % candidates.length;
  return target;
};

// routing
/*
  - filter servers
  - forwards/sends requests to the active servers
  - streams response back
  - handles faliure
*/

/*
 Piping in NodeJS is the process by which byte data from one stream is sent to another stream

 allows you to connect a readable stream directly into a writable stream, automatically managing data flow and backpressure. Since an HTTP request (http.IncomingMessage) is a readable stream and an HTTP response (http.ServerResponse) is a writable stream, you can easily pipe data between them.

 readableStream.pipe(writableStream);

*/
const requestHandler = (req, res, tried = new Set()) => {
  const candidates = servers.filter((item) => item.healthy && !tried.has(item.port));

  if (candidates.length === 0) return res.status(503).send("no healthy backend server available");

  const targetServer = getNextServer(candidates);
  tried.add(targetServer.port);
  console.log(`${req.method} ${req.originalUrl} -> ${target.port}`);

  // outgoing request from LB to backend server. Writable stream.
  const upstream = http.request(
    {
      host: target.host,
      port: target.port,
      path: req.originalUrl,
      method: req.method,
      headers: { ...req.headers, host: `${target.host}:${target.port}` },
    },
    (serverResponse) => {
      res.writeHead(serverResponse.statusCode, serverResponse.headers);
      serverResponse.pipe(res);
    },
  );

  upstream.on("error", () => {
    setHealth(target, false);
    if (!res.headersSent) requestHandler(req, res, tried);
  });

  if (req.readableEnded) upstream.end();
  else req.pipe(upstream);
};

app.use((req, res) => handleRequest(req, res));
app.listen(8080, () => console.log("LoadBalancer running on port 8080"));
