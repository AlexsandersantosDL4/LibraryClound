package com.livraria.controller;

import com.livraria.dto.AvaliacaoRequest;
import com.livraria.dto.AvaliacaoResponse;
import com.livraria.security.JwtService;
import com.livraria.security.UserPrincipal;
import com.livraria.service.AvaliacaoService;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller REST responsável pelas avaliações de livros com estrelas (0.5 a 5.0) e comentários:
 * - POST /api/avaliacoes       : Salva ou altera a nota de meia estrela até 5 estrelas e comentário
 * - GET  /api/avaliacoes/livro : Retorna a média geral, total de votos, nota do usuário e comentários
 */
public class AvaliacaoController implements HttpHandler {

    private final AvaliacaoService avaliacaoService = new AvaliacaoService();
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
            if ("POST".equalsIgnoreCase(method)) {
                handleSalvarAvaliacao(exchange);
            } else if ("GET".equalsIgnoreCase(method)) {
                handleObterEstatisticas(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Endpoint de avaliações não encontrado\"}");
            }
        } catch (Exception e) {
            HttpHelper.sendError(exchange, e);
        }
    }

    private void handleSalvarAvaliacao(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, Object> dados = JsonHelper.parseJson(body);

        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(dados, "usuarioId", 0);
        int livroId = JsonHelper.getInt(dados, "livroId", 0);
        double nota = JsonHelper.getDouble(dados, "nota", 5.0);
        String comentario = JsonHelper.getString(dados, "comentario", "");

        AvaliacaoRequest request = new AvaliacaoRequest(livroId, nota, comentario);
        AvaliacaoResponse salva = avaliacaoService.avaliar(usuarioId, request);
        double[] stats = avaliacaoService.obterEstatisticas(livroId);

        Map<String, Object> resposta = Map.of(
                "mensagem", "Avaliação registrada com sucesso!",
                "notaSalva", salva.nota(),
                "comentario", salva.comentario(),
                "mediaAtualizada", Math.round(stats[0] * 10.0) / 10.0,
                "totalAvaliacoes", (int) stats[1]
        );
        HttpHelper.sendObjectResponse(exchange, 200, resposta);
    }

    private void handleObterEstatisticas(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);

        int livroId = JsonHelper.getInt(params, "livroId", 0);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(params, "usuarioId", 0);

        double[] stats = avaliacaoService.obterEstatisticas(livroId);
        AvaliacaoResponse avaliacaoUser = avaliacaoService.obterAvaliacaoDoUsuario(usuarioId, livroId);
        List<AvaliacaoResponse> listaComentarios = avaliacaoService.listarPorLivro(livroId);

        double notaUsuario = (avaliacaoUser != null) ? avaliacaoUser.nota() : 0.0;
        String comentarioUsuario = (avaliacaoUser != null) ? avaliacaoUser.comentario() : "";

        Map<String, Object> resposta = Map.of(
                "livroId", livroId,
                "media", Math.round(stats[0] * 10.0) / 10.0,
                "total", (int) stats[1],
                "notaUsuario", notaUsuario,
                "comentarioUsuario", comentarioUsuario,
                "avaliacoes", listaComentarios
        );
        HttpHelper.sendObjectResponse(exchange, 200, resposta);
    }
}
