package com.livraria.controller;

import com.livraria.dao.MarcaPaginaDAO;
import com.livraria.model.MarcaPagina;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Controller responsável pela experiência de leitura na LibraryClound:
 * - POST /api/leitura/marcapagina : Salva a página atual do livro onde o usuário parou
 * - GET  /api/leitura/status      : Retorna o progresso do usuário no livro
 * - GET  /api/leitura/usuario     : Retorna todos os marca-páginas do usuário logado
 * - POST /api/leitura/finalizar   : Confirma que finalizou o livro e incrementa o contador no perfil
 */
public class LeituraController implements HttpHandler {

    private final MarcaPaginaDAO marcaPaginaDAO = new MarcaPaginaDAO();

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
        } catch (SQLException e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 500, "{\"erro\":\"Erro no SQLite: " + JsonHelper.escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Requisição inválida: " + JsonHelper.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleSalvarMarcaPagina(HttpExchange exchange) throws IOException, SQLException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> dados = JsonHelper.parseJson(body);

        int usuarioId = JsonHelper.getInt(dados, "usuarioId", 0);
        int livroId = JsonHelper.getInt(dados, "livroId", 0);
        int paginaAtual = JsonHelper.getInt(dados, "paginaAtual", 1);

        if (usuarioId <= 0 || livroId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Identificadores de usuário ou livro inválidos.\"}");
            return;
        }

        MarcaPagina salvo = marcaPaginaDAO.salvarMarcaPagina(usuarioId, livroId, paginaAtual);

        String resposta = "{"
                + "\"mensagem\":\"Marca-páginas atualizado na página " + paginaAtual + "!\","
                + "\"marcaPagina\":" + (salvo != null ? salvo.toJson() : "{}")
                + "}";
        HttpHelper.sendJsonResponse(exchange, 200, resposta);
    }

    private void handleStatus(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        int usuarioId = JsonHelper.getInt(params, "usuarioId", 0);
        int livroId = JsonHelper.getInt(params, "livroId", 0);

        if (usuarioId <= 0 || livroId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Parâmetros usuarioId e livroId são obrigatórios.\"}");
            return;
        }

        MarcaPagina mp = marcaPaginaDAO.buscarPorUsuarioELivro(usuarioId, livroId);
        if (mp != null) {
            HttpHelper.sendJsonResponse(exchange, 200, mp.toJson());
        } else {
            String defaultJson = "{\"paginaAtual\":1,\"finalizado\":false}";
            HttpHelper.sendJsonResponse(exchange, 200, defaultJson);
        }
    }

    private void handleListarPorUsuario(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        int usuarioId = JsonHelper.getInt(params, "usuarioId", 0);

        if (usuarioId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Parâmetro usuarioId é obrigatório.\"}");
            return;
        }

        List<MarcaPagina> lista = marcaPaginaDAO.listarPorUsuario(usuarioId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            sb.append(lista.get(i).toJson());
            if (i < lista.size() - 1) sb.append(",");
        }
        sb.append("]");

        HttpHelper.sendJsonResponse(exchange, 200, sb.toString());
    }

    private void handleFinalizarLivro(HttpExchange exchange) throws IOException, SQLException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> dados = JsonHelper.parseJson(body);

        int usuarioId = JsonHelper.getInt(dados, "usuarioId", 0);
        int livroId = JsonHelper.getInt(dados, "livroId", 0);

        if (usuarioId <= 0 || livroId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Identificadores de usuário ou livro inválidos.\"}");
            return;
        }

        int totalLivrosLidosAtualizado = marcaPaginaDAO.confirmarFinalizacao(usuarioId, livroId);

        String resposta = "{"
                + "\"sucesso\":true,"
                + "\"mensagem\":\"Parabéns! Você concluiu a leitura deste livro com sucesso!\","
                + "\"livrosLidos\":" + totalLivrosLidosAtualizado
                + "}";

        HttpHelper.sendJsonResponse(exchange, 200, resposta);
    }
}
