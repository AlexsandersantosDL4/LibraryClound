package com.livraria.controller;

import com.livraria.dao.LivroDAO;
import com.livraria.model.Livro;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável pelo catálogo e pesquisa de Livros da LibraryClound:
 * - GET /api/livros             : Lista todos os livros ou filtra por termo e/ou categoria
 * - GET /api/livros/categorias  : Retorna a lista de categorias disponíveis no acervo
 * - GET /api/livros/{id}        : Retorna detalhes de um livro específico
 */
public class LivroController implements HttpHandler {

    private final LivroDAO livroDAO = new LivroDAO();

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
        } catch (SQLException e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 500, "{\"erro\":\"Erro no SQLite: " + JsonHelper.escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Requisição inválida: " + JsonHelper.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleListarEPesquisar(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        String termo = params.get("termo");
        String categoria = params.get("categoria");

        List<Livro> livros;
        if ((termo != null && !termo.isBlank()) || (categoria != null && !categoria.isBlank() && !categoria.equalsIgnoreCase("Todas"))) {
            livros = livroDAO.pesquisar(termo, categoria);
        } else {
            livros = livroDAO.listarTodos();
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < livros.size(); i++) {
            sb.append(livros.get(i).toJson());
            if (i < livros.size() - 1) sb.append(",");
        }
        sb.append("]");

        HttpHelper.sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleCategorias(HttpExchange exchange) throws IOException, SQLException {
        List<String> categorias = livroDAO.listarCategorias();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < categorias.size(); i++) {
            sb.append("\"").append(JsonHelper.escape(categorias.get(i))).append("\"");
            if (i < categorias.size() - 1) sb.append(",");
        }
        sb.append("]");

        HttpHelper.sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleBuscarPorId(HttpExchange exchange, String path) throws IOException, SQLException {
        int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        Livro livro = livroDAO.buscarPorId(id);
        if (livro != null) {
            HttpHelper.sendJsonResponse(exchange, 200, livro.toJson());
        } else {
            HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Livro não encontrado.\"}");
        }
    }
}
