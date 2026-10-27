package com.livraria.controller;

import com.livraria.dto.MarcaPaginaRequest;
import com.livraria.dto.MarcaPaginaResponse;
import com.livraria.security.JwtService;
import com.livraria.security.UserPrincipal;
import com.livraria.service.LeituraService;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller REST responsável pela experiência de leitura na LibraryClound:
 * - POST /api/leitura/marcapagina : Salva a página atual do livro onde o usuário parou
 * - GET  /api/leitura/status      : Retorna o progresso do usuário no livro
 * - GET  /api/leitura/usuario     : Retorna todos os marca-páginas do usuário logado
 * - POST /api/leitura/finalizar   : Confirma que finalizou o livro e incrementa o contador no perfil
 */
public class LeituraController implements HttpHandler {

    private final LeituraService leituraService = new LeituraService();
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
            if (path.endsWith("/marcapagina") && "POST".equalsIgnoreCase(method)) {
                handleSalvarMarcaPagina(exchange);
            } else if (path.endsWith("/status") && "GET".equalsIgnoreCase(method)) {
                handleStatus(exchange);
            } else if (path.endsWith("/usuario") && "GET".equalsIgnoreCase(method)) {
                handleListarPorUsuario(exchange);
            } else if (path.endsWith("/finalizar") && "POST".equalsIgnoreCase(method)) {
                handleFinalizarLivro(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Endpoint de leitura não encontrado\"}");
            }
        } catch (Exception e) {
            HttpHelper.sendError(exchange, e);
        }
    }

    private void handleSalvarMarcaPagina(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, Object> dados = JsonHelper.parseJson(body);

        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(dados, "usuarioId", 0);
        int livroId = JsonHelper.getInt(dados, "livroId", 0);
        int paginaAtual = JsonHelper.getInt(dados, "paginaAtual", 1);

        MarcaPaginaRequest request = new MarcaPaginaRequest(livroId, paginaAtual);
        MarcaPaginaResponse salvo = leituraService.salvarMarcaPagina(usuarioId, request);

        Map<String, Object> resposta = Map.of(
                "mensagem", "Marca-páginas atualizado na página " + salvo.paginaAtual() + "!",
                "marcaPagina", salvo
        );
        HttpHelper.sendObjectResponse(exchange, 200, resposta);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(params, "usuarioId", 0);
        int livroId = JsonHelper.getInt(params, "livroId", 0);

        MarcaPaginaResponse response = leituraService.consultarStatus(usuarioId, livroId);
        HttpHelper.sendObjectResponse(exchange, 200, response);
    }

    private void handleListarPorUsuario(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(params, "usuarioId", 0);

        List<MarcaPaginaResponse> lista = leituraService.listarPorUsuario(usuarioId);
        HttpHelper.sendObjectResponse(exchange, 200, lista);
    }

    private void handleFinalizarLivro(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, Object> dados = JsonHelper.parseJson(body);

        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(dados, "usuarioId", 0);
        int livroId = JsonHelper.getInt(dados, "livroId", 0);

        int totalLivrosLidosAtualizado = leituraService.confirmarFinalizacao(usuarioId, livroId);

        Map<String, Object> resposta = Map.of(
                "sucesso", true,
                "mensagem", "Parabéns! Você concluiu a leitura deste livro com sucesso!",
                "livrosLidos", totalLivrosLidosAtualizado
        );
        HttpHelper.sendObjectResponse(exchange, 200, resposta);
    }
}
