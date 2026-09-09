import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Server {

    // ---- original calculation logic ----

    static double investmentAmount(double price, int lots) {
        return price * lots;
    }

    static double roi(double strikePrice, int lots, double amount) {
        return (strikePrice * lots) - amount;
    }

    static int multiplier(String indices) {
        switch (indices.toLowerCase()) {
            case "nifty":
                return 65;
            case "banknifty":
                return 30;
            case "sensex":
                return 20;
            default:
                return -1; // invalid
        }
    }

    // ---- HTTP layer ----

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/calculate", new CalculateHandler());
        server.createContext("/", new StaticFileHandler(Path.of("public")));
        server.setExecutor(null);
        server.start();

        System.out.println("PriceCalculator server running.");
        System.out.println("Open http://localhost:" + port + " in your browser.");
    }

    static class CalculateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"error\":\"Use GET\"}");
                return;
            }

            try {
                Map<String, String> q = parseQuery(exchange.getRequestURI());
                String indices = q.getOrDefault("indices", "");
                int m = multiplier(indices);

                if (m == -1) {
                    sendJson(exchange, 400, "{\"error\":\"Invalid Indices name\"}");
                    return;
                }

                double price = Double.parseDouble(q.get("price"));
                int lotSize = Integer.parseInt(q.get("lotSize"));
                double strikePrice = Double.parseDouble(q.get("strikePrice"));

                if (price < 0 || lotSize < 0 || strikePrice < 0) {
                    sendJson(exchange, 400, "{\"error\":\"Values must be non-negative\"}");
                    return;
                }

                int lots = lotSize * m;
                double amount = investmentAmount(price, lots);
                double roiValue = roi(strikePrice, lots, amount);

                String json = String.format(
                        "{\"quantity\":%d,\"investmentAmount\":%.2f,\"roi\":%.2f}",
                        lots, amount, roiValue);
                sendJson(exchange, 200, json);

            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "{\"error\":\"price, lotSize and strikePrice must be valid numbers\"}");
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    /** Serves static files (index.html, etc.) from the given directory. */
    static class StaticFileHandler implements HttpHandler {
        private final Path root;

        StaticFileHandler(Path root) {
            this.root = root;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requested = exchange.getRequestURI().getPath();
            if (requested.equals("/"))
                requested = "/index.html";

            Path filePath = root.resolve(requested.substring(1)).normalize();
            if (!filePath.startsWith(root) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                byte[] body = "404 Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
                return;
            }

            String contentType = guessContentType(filePath.toString());
            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String guessContentType(String name) {
            if (name.endsWith(".html"))
                return "text/html; charset=utf-8";
            if (name.endsWith(".css"))
                return "text/css; charset=utf-8";
            if (name.endsWith(".js"))
                return "application/javascript; charset=utf-8";
            return "application/octet-stream";
        }
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null)
            return params;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0)
                continue;
            String key = java.net.URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = java.net.URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }
}
