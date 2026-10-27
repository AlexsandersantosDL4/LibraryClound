package com.livraria.controller;

import com.livraria.model.Favorito;
import com.livraria.security.JwtService;
import com.livraria.security.UserPrincipal;
import com.livraria.service.FavoritoService;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller REST responsável pelas operações de Favoritos na LibraryClound:
 * - POST /api/favoritos/toggle  : Adiciona ou remove o livro dos favoritos
 * - GET  /api/favoritos/usuario : Lista os livros favoritados pelo usuário (nome, categoria, capa)
 * - GET  /api/favoritos/check   : Verifica se um livro específico está favoritado
 */
public class FavoritoController implements HttpHandler {

    private final FavoritoService favoritoService = new FavoritoService();
    private final JwtService jwtService = new JwtService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            HttpHelper.handleCorsPreflight(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if ((path.endsWith("/toggle") || path.endsWith("/api/favoritos")) && "POST".equalsIgnoreCase(method)) {
                handleToggle(exchange);
            } else if (path.endsWith("/usuario") && "GET".equalsIgnoreCase(method)) {
                handleListarPorUsuario(exchange);
            } else if (path.endsWith("/check") && "GET".equalsIgnoreCase(method)) {
                handleCheck(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Endpoint de favoritos não encontrado\"}");
            }
        } catch (Exception e) {
            HttpHelper.sendError(exchange, e);
        }
    }

    private void handleToggle(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, Object> dados = JsonHelper.parseJson(body);

        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(dados, "usuarioId", 0);
        int livroId = JsonHelper.getInt(dados, "livroId", 0);

        boolean favoritado = favoritoService.toggleFavorito(usuarioId, livroId);

        Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "favoritado", favoritado,
                "mensagem", favoritado ? "Livro adicionado aos seus favoritos!" : "Livro removido dos favoritos."
        );
        HttpHelper.sendObjectResponse(exchange, 200, resposta);
    }

    private void handleListarPorUsuario(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(params, "usuarioId", 0);

        List<Favorito> lista = favoritoService.listarPorUsuario(usuarioId);
        HttpHelper.sendObjectResponse(exchange, 200, lista);
    }

    private void handleCheck(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(params, "usuarioId", 0);
        int livroId = JsonHelper.getInt(params, "livroId", 0);

        boolean fav = favoritoService.isFavorito(usuarioId, livroId);
        Map<String, Object> resposta = Map.of("favorito", fav);
        HttpHelper.sendObjectResponse(exchange, 200, resposta);
    }
}
