package com.livraria.controller;

import com.livraria.dao.UsuarioDAO;
import com.livraria.model.Usuario;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Controller de Autenticação e Perfil de Usuário na LibraryClound:
 * - POST /api/auth/register : Cadastro com nome fantasia, email e senha (máx 8 números)
 * - POST /api/auth/login    : Login
 * - GET  /api/auth/me       : Consulta dados atualizados do perfil
 * - PUT  /api/auth/perfil   : Edição de perfil (nome, foto e descrição até 150 caracteres)
 */
public class AuthController implements HttpHandler {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            HttpHelper.handleCorsPreflight(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            if (path.endsWith("/register") && "POST".equalsIgnoreCase(method)) {
                handleRegister(exchange);
            } else if (path.endsWith("/login") && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else if (path.endsWith("/me") && "GET".equalsIgnoreCase(method)) {
                handleMe(exchange);
            } else if (path.endsWith("/perfil") && ("PUT".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method))) {
                handleAtualizarPerfil(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Endpoint de autenticação não encontrado\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 500, "{\"erro\":\"Erro no banco de dados SQLite: " + JsonHelper.escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Requisição inválida: " + JsonHelper.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException, SQLException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> dados = JsonHelper.parseJson(body);

        String nomeFantasia = JsonHelper.getString(dados, "nomeFantasia", "");
        String email = JsonHelper.getString(dados, "email", "");
        String senha = JsonHelper.getString(dados, "senha", "");
        String tipo = JsonHelper.getString(dados, "tipo", "CLIENTE");

        if (nomeFantasia.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Preencha todos os campos obrigatórios.\"}");
            return;
        }

        // Validação da senha: no máximo 8 números
        if (!Usuario.isSenhaValida(senha)) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"A senha deve conter exclusivamente números e no máximo 8 dígitos (ex: 12345678).\"}");
            return;
        }

        if (usuarioDAO.existeEmail(email)) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Este email já está cadastrado na LibraryClound.\"}");
            return;
        }

        Usuario novoUsuario = new Usuario(nomeFantasia, email, senha, tipo);
        novoUsuario.setFotoUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150");
        novoUsuario.setDescricao("Novo leitor da LibraryClound!");

        Usuario salvo = usuarioDAO.cadastrar(novoUsuario);

        String resposta = "{"
                + "\"mensagem\":\"Cadastro realizado com sucesso na LibraryClound!\","
                + "\"usuario\":" + salvo.toJson()
                + "}";
        HttpHelper.sendJsonResponse(exchange, 201, resposta);
    }

    private void handleLogin(HttpExchange exchange) throws IOException, SQLException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> dados = JsonHelper.parseJson(body);

        String email = JsonHelper.getString(dados, "email", "");
        String senha = JsonHelper.getString(dados, "senha", "");

        if (email.isEmpty() || senha.isEmpty()) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"Informe o email e a senha para efetuar o login.\"}");
            return;
        }

        Usuario usuario = usuarioDAO.autenticar(email, senha);

        if (usuario != null) {
            String resposta = "{"
                    + "\"mensagem\":\"Login realizado com sucesso!\","
                    + "\"usuario\":" + usuario.toJson()
                    + "}";
            HttpHelper.sendJsonResponse(exchange, 200, resposta);
        } else {
            HttpHelper.sendJsonResponse(exchange, 401, "{\"erro\":\"Email ou senha incorretos. Verifique suas credenciais.\"}");
        }
    }

    private void handleMe(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
        int usuarioId = JsonHelper.getInt(params, "id", 0);

        if (usuarioId <= 0) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"ID do usuário não fornecido.\"}");
            return;
        }

        Usuario usuario = usuarioDAO.buscarPorId(usuarioId);
        if (usuario != null) {
            HttpHelper.sendJsonResponse(exchange, 200, usuario.toJson());
        } else {
            HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Usuário não encontrado.\"}");
        }
    }

    /**
     * Requisito: "aba para editar o próprio perfil, podendo editar seu nome, por uma foto, criar uma descrição sobre si de até 150 caracteres"
     */
    private void handleAtualizarPerfil(HttpExchange exchange) throws IOException, SQLException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> dados = JsonHelper.parseJson(body);

        int id = JsonHelper.getInt(dados, "id", 0);
        String nomeFantasia = JsonHelper.getString(dados, "nomeFantasia", "");
        String fotoUrl = JsonHelper.getString(dados, "fotoUrl", "");
        String descricao = JsonHelper.getString(dados, "descricao", "");

        if (id <= 0 || nomeFantasia.isBlank()) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"ID de usuário e Nome Fantasia são obrigatórios.\"}");
            return;
        }

        // Validação da regra: descrição de até 150 caracteres
        if (descricao != null && descricao.length() > 150) {
            HttpHelper.sendJsonResponse(exchange, 400, "{\"erro\":\"A descrição de perfil não pode ultrapassar 150 caracteres (atual: " + descricao.length() + ").\"}");
            return;
        }

        Usuario atualizado = usuarioDAO.atualizarPerfil(id, nomeFantasia, fotoUrl, descricao);
        if (atualizado != null) {
            String resposta = "{"
                    + "\"mensagem\":\"Perfil atualizado com sucesso!\","
                    + "\"usuario\":" + atualizado.toJson()
                    + "}";
            HttpHelper.sendJsonResponse(exchange, 200, resposta);
        } else {
            HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Usuário não encontrado para atualização.\"}");
        }
    }
}
