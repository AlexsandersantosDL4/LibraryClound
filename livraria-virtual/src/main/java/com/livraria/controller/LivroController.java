package com.livraria.controller;

import com.livraria.dto.LivroResponse;
import com.livraria.service.LivroService;
import com.livraria.util.HttpHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller REST responsável pelo catálogo e pesquisa de Livros da LibraryClound:
 * - GET /api/livros             : Lista todos os livros ou filtra por termo e/ou categoria
 * - GET /api/livros/categorias  : Retorna a lista de categorias disponíveis no acervo
 * - GET /api/livros/{id}        : Retorna detalhes de um livro específico
 */
public class LivroController implements HttpHandler {

    private final LivroService livroService = new LivroService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            HttpHelper.handleCorsPreflight(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (path.endsWith("/categorias")) {
                    handleCategorias(exchange);
                } else if (path.matches(".*/api/livros/\\d+$")) {
                    handleBuscarPorId(exchange, path);
                } else {
                    handleListarEPesquisar(exchange);
                }
            } else {
                HttpHelper.sendJsonResponse(exchange, 405, "{\"erro\":\"Método não permitido neste endpoint\"}");
            }
        } catch (Exception e) {
            HttpHelper.sendError(exchange, e);
        }
    }

    private void handleListarEPesquisar(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String termo = params.get("termo");
        String categoria = params.get("categoria");

        List<LivroResponse> livros = livroService.listarTodos(termo, categoria);
        HttpHelper.sendObjectResponse(exchange, 200, livros);
    }

    private void handleCategorias(HttpExchange exchange) throws IOException {
        List<String> categorias = livroService.listarCategoriasExistentes();
        HttpHelper.sendObjectResponse(exchange, 200, categorias);
    }

    private void handleBuscarPorId(HttpExchange exchange, String path) throws IOException {
        int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        LivroResponse response = livroService.buscarPorId(id);
        HttpHelper.sendObjectResponse(exchange, 200, response);
    }
}
