package com.livraria.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Manipulador para servir os arquivos estáticos do Frontend (HTML, CSS, JavaScript)
 * permitindo que a LibraryClound seja acessada diretamente pelo navegador em http://localhost:8080.
 */
public class StaticFileHandler implements HttpHandler {

    private final String basePath;

    public StaticFileHandler(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }

        File fileOnDisk = new File(basePath, path);
        if (fileOnDisk.exists() && !fileOnDisk.isDirectory()) {
            servirArquivo(exchange, new FileInputStream(fileOnDisk), path, fileOnDisk.length());
            return;
        }

        String resourcePath = "/public" + path;
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is != null) {
            byte[] bytes = is.readAllBytes();
            servirBytes(exchange, bytes, path);
            return;
        }

        String notFound = "<h1>404 - Arquivo não encontrado na LibraryClound</h1>";
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(404, notFound.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(notFound.getBytes());
        }
    }

    private void servirArquivo(HttpExchange exchange, InputStream is, String path, long length) throws IOException {
        String contentType = determinarContentType(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
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
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String determinarContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "text/plain; charset=UTF-8";
    }
}
