package com.livraria.controller;

import com.livraria.dao.AvaliacaoDAO;
import com.livraria.model.Avaliacao;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Controller responsável pelas avaliações de livros com estrelas (0.5 a 5.0):
 * - POST /api/avaliacoes       : Salva ou altera a nota de meia estrela até 5 estrelas
 * - GET  /api/avaliacoes/livro : Retorna a média geral, total de votos e a nota do usuário logado
 */
public class AvaliacaoController implements HttpHandler {

    private final AvaliacaoDAO avaliacaoDAO = new AvaliacaoDAO();

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
        } catch (SQLException e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 500, "{\"erro\":\"Erro no SQLite: " + JsonHelper.escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Requisição inválida: " + JsonHelper.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleSalvarAvaliacao(HttpExchange exchange) throws IOException, SQLException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> dados = JsonHelper.parseJson(body);

        int usuarioId = JsonHelper.getInt(dados, "usuarioId", 0);
        int livroId = JsonHelper.getInt(dados, "livroId", 0);
        double nota = 5.0;
        try {
            if (dados.containsKey("nota") && dados.get("nota") != null) {
                nota = Double.parseDouble(dados.get("nota").trim());
            }
        } catch (NumberFormatException ignored) {
        }

        if (usuarioId <= 0 || livroId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Identificadores de usuário ou livro inválidos.\"}");
            return;
        }

        // Validação da regra: de meia estrela (0.5) até 5 estrelas (5.0)
        double notaValida = Avaliacao.validarNota(nota);

        Avaliacao salva = avaliacaoDAO.salvarOuAtualizar(usuarioId, livroId, notaValida);
        double[] stats = avaliacaoDAO.obterEstatisticas(livroId);

        String resposta = "{"
                + "\"mensagem\":\"Avaliação registrada com sucesso!\","
                + "\"notaSalva\":" + String.format(java.util.Locale.US, "%.1f", salva.getNota()) + ","
                + "\"mediaAtualizada\":" + String.format(java.util.Locale.US, "%.1f", stats[0]) + ","
                + "\"totalAvaliacoes\":" + (int) stats[1]
                + "}";
        HttpHelper.sendJsonResponse(exchange, 200, resposta);
    }

    private void handleObterEstatisticas(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        int livroId = JsonHelper.getInt(params, "livroId", 0);
        int usuarioId = JsonHelper.getInt(params, "usuarioId", 0);

        if (livroId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"livroId obrigatório.\"}");
            return;
        }

        double[] stats = avaliacaoDAO.obterEstatisticas(livroId);
        double notaUsuario = 0.0;
        if (usuarioId > 0) {
            notaUsuario = avaliacaoDAO.obterNotaDoUsuario(usuarioId, livroId);
        }

        String resposta = "{"
                + "\"livroId\":" + livroId + ","
                + "\"media\":" + String.format(java.util.Locale.US, "%.1f", stats[0]) + ","
                + "\"total\":" + (int) stats[1] + ","
                + "\"notaUsuario\":" + String.format(java.util.Locale.US, "%.1f", notaUsuario)
                + "}";
        HttpHelper.sendJsonResponse(exchange, 200, resposta);
    }
}
