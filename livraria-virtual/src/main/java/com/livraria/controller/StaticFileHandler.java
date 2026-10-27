package com.livraria.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Manipulador para servir os arquivos estáticos do Frontend (HTML, CSS, JavaScript)
 * permitindo que a LibraryClound seja acessada diretamente pelo navegador em http://localhost:8080.
 */
public class StaticFileHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(StaticFileHandler.class);
    private final String basePath;

    public StaticFileHandler(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        if (rawPath == null || rawPath.isBlank() || rawPath.equals("/")) {
            rawPath = "/index.html";
        }

        // Remove barra inicial para evitar que o Windows interprete como caminho absoluto no drive raiz (C:\)
        String relativePath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;

        // Candidatos de busca de arquivos estáticos no disco
        List<File> candidatos = List.of(
                new File(basePath, relativePath),
                new File("frontend", relativePath),
                new File("../frontend", relativePath),
                new File("src/main/resources/public", relativePath),
                new File("bin/public", relativePath)
        );

        for (File f : candidatos) {
            if (f.exists() && !f.isDirectory()) {
                servirArquivo(exchange, new FileInputStream(f), relativePath, f.length());
                return;
            }
        }

        // Fallback para recursos empacotados no Classpath
        List<String> classpathCandidates = List.of(
                "/public/" + relativePath,
                "/" + relativePath
        );

        for (String cp : classpathCandidates) {
            InputStream is = getClass().getResourceAsStream(cp);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                servirBytes(exchange, bytes, relativePath);
                return;
            }
        }

        logger.warn("404 Arquivo estático não encontrado: rawPath={}, relativePath={}, basePath={}", rawPath, relativePath, basePath);
        String notFound = "<h1>404 - Arquivo não encontrado na LibraryClound</h1><p>Caminho: " + rawPath + "</p>";
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(404, notFound.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(notFound.getBytes());
        }
    }

    private void servirArquivo(HttpExchange exchange, InputStream is, String path, long length) throws IOException {
        String contentType = determinarContentType(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, must-revalidate");
        exchange.sendResponseHeaders(200, length);

        try (is; OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        }
    }

    private void servirBytes(HttpExchange exchange, byte[] bytes, String path) throws IOException {
        String contentType = determinarContentType(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, must-revalidate");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String determinarContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml; charset=UTF-8";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        return "text/plain; charset=UTF-8";
    }
}
