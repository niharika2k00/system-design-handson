import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class Server {
  private static final int PORT = 8001;

  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

    server.createContext("/", Server::rootHandler);
    server.createContext("/api/hello", Server::helloHandler);
    server.setExecutor(null);
    server.start();

    System.out.println("listening on http://localhost:" + PORT);
  }

  private static void rootHandler(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestURI().getPath().equals("/")) {
      send(exchange, 404, "not found");
      return;
    }

    send(exchange, 200, "server has started...");
  }

  private static void helloHandler(HttpExchange exchange) throws IOException {
    if (!exchange.getRequestURI().getPath().equals("/api/hello")) {
      send(exchange, 404, "not found");
      return;
    }

    send(exchange, 200, "Great! routes working");
  }

  private static void send(HttpExchange exchange, int statusCode, String message) throws IOException {
    byte[] body = message.getBytes(StandardCharsets.UTF_8);

    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
    exchange.sendResponseHeaders(statusCode, body.length);

    OutputStream os = exchange.getResponseBody();
    os.write(body);
    os.close();
  }
}
