package com.livraria.controller;

import com.livraria.dto.CategoriaRequest;
import com.livraria.dto.CategoriaResponse;
import com.livraria.security.JwtService;
import com.livraria.security.UserPrincipal;
import com.livraria.service.CategoriaService;
import com.livraria.util.HttpHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

/**
 * Controller REST para Categorias de Livros da LibraryClound:
 * - GET  /api/categorias : Lista todas as categorias com quantidade de livros
 * - POST /api/categorias : Cadastro de nova categoria
 */
public class CategoriaController implements HttpHandler {

    private final CategoriaService categoriaService = new CategoriaService();
    private final JwtService jwtService = new JwtService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            HttpHelper.handleCorsPreflight(exchange);
            return;
        }

        try {
            if ("GET".equalsIgnoreCase(method)) {
                List<CategoriaResponse> lista = categoriaService.listarTodas();
                HttpHelper.sendObjectResponse(exchange, 200, lista);
            } else if ("POST".equalsIgnoreCase(method)) {
                UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
                if (principal != null && !principal.isAdmin()) {
                    HttpHelper.sendJsonResponse(exchange, 403, "{\"erro\":\"Apenas administradores podem cadastrar categorias.\"}");
                    return;
                }
                CategoriaRequest request = HttpHelper.readJsonBody(exchange, CategoriaRequest.class);
                CategoriaResponse salva = categoriaService.salvar(request);
                HttpHelper.sendObjectResponse(exchange, 201, salva);
            } else {
                HttpHelper.sendJsonResponse(exchange, 405, "{\"erro\":\"Método não suportado para /api/categorias\"}");
            }
        } catch (Exception e) {
            HttpHelper.sendError(exchange, e);
        }
    }
}
