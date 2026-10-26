package com.livraria.controller;

import com.livraria.dao.FavoritoDAO;
import com.livraria.model.Favorito;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável pelas operações de Favoritos na LibraryClound:
 * - POST /api/favoritos/toggle  : Adiciona ou remove o livro dos favoritos
 * - GET  /api/favoritos/usuario : Lista os livros favoritados pelo usuário (nome, categoria, capa)
 * - GET  /api/favoritos/check   : Verifica se um livro específico está favoritado
 */
public class FavoritoController implements HttpHandler {

    private final FavoritoDAO favoritoDAO = new FavoritoDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            HttpHelper.handleCorsPreflight(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.endsWith("/toggle") && "POST".equalsIgnoreCase(method)) {
                handleToggle(exchange);
            } else if (path.endsWith("/usuario") && "GET".equalsIgnoreCase(method)) {
                handleListarPorUsuario(exchange);
            } else if (path.endsWith("/check") && "GET".equalsIgnoreCase(method)) {
                handleCheck(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Endpoint de favoritos não encontrado\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 500, "{\"erro\":\"Erro no SQLite: " + JsonHelper.escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Requisição inválida: " + JsonHelper.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleToggle(HttpExchange exchange) throws IOException, SQLException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> dados = JsonHelper.parseJson(body);

        int usuarioId = JsonHelper.getInt(dados, "usuarioId", 0);
        int livroId = JsonHelper.getInt(dados, "livroId", 0);

        if (usuarioId <= 0 || livroId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Identificadores de usuário ou livro inválidos.\"}");
            return;
        }

        boolean favoritado = favoritoDAO.toggleFavorito(usuarioId, livroId);

        String resposta = "{"
                + "\"sucesso\":true,"
                + "\"favoritado\":" + favoritado + ","
                + "\"mensagem\":\"" + (favoritado ? "Livro adicionado aos seus favoritos!" : "Livro removido dos favoritos.") + "\""
                + "}";
        HttpHelper.sendJsonResponse(exchange, 200, resposta);
    }

    private void handleListarPorUsuario(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        int usuarioId = JsonHelper.getInt(params, "usuarioId", 0);

        if (usuarioId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Parâmetro usuarioId obrigatório.\"}");
            return;
        }

        List<Favorito> lista = favoritoDAO.listarPorUsuario(usuarioId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            sb.append(lista.get(i).toJson());
            if (i < lista.size() - 1) sb.append(",");
        }
        sb.append("]");

        HttpHelper.sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleCheck(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        int usuarioId = JsonHelper.getInt(params, "usuarioId", 0);
        int livroId = JsonHelper.getInt(params, "livroId", 0);

        boolean fav = favoritoDAO.isFavorito(usuarioId, livroId);
        HttpHelper.sendJsonResponse(exchange, 200, "{\"favorito\":" + fav + "}");
    }
}
