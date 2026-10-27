package com.livraria.controller;

import com.livraria.dto.AuthResponse;
import com.livraria.dto.LoginRequest;
import com.livraria.dto.RegisterRequest;
import com.livraria.dto.UpdateProfileRequest;
import com.livraria.dto.UsuarioResponse;
import com.livraria.security.JwtService;
import com.livraria.security.UserPrincipal;
import com.livraria.service.UsuarioService;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Controller REST de Autenticação e Perfil de Usuário na LibraryClound:
 * - POST /api/auth/register : Cadastro com emissão de JWT
 * - POST /api/auth/login    : Login com validação BCrypt e emissão de JWT
 * - GET  /api/auth/me       : Consulta dados atualizados do perfil autenticado
 * - PUT  /api/auth/perfil   : Edição de perfil (nome, foto e descrição até 150 caracteres)
 */
public class AuthController implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UsuarioService usuarioService = new UsuarioService();
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
            if (path.endsWith("/register") && "POST".equalsIgnoreCase(method)) {
                handleRegister(exchange);
            } else if (path.endsWith("/login") && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else if (path.endsWith("/me") && "GET".equalsIgnoreCase(method)) {
                handleMe(exchange);
            } else if (path.endsWith("/perfil") && ("PUT".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method))) {
                handleAtualizarPerfil(exchange);
            } else if (path.endsWith("/usuarios") && "GET".equalsIgnoreCase(method)) {
                HttpHelper.sendObjectResponse(exchange, 200, usuarioService.listarTodos());
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Endpoint de autenticação não encontrado\"}");
            }
        } catch (Exception e) {
            HttpHelper.sendError(exchange, e);
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        RegisterRequest request = HttpHelper.readJsonBody(exchange, RegisterRequest.class);
        AuthResponse response = usuarioService.cadastrar(request);
        HttpHelper.sendObjectResponse(exchange, 201, response);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        LoginRequest request = HttpHelper.readJsonBody(exchange, LoginRequest.class);
        AuthResponse response = usuarioService.autenticar(request);
        HttpHelper.sendObjectResponse(exchange, 200, response);
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId;

        if (principal != null) {
            usuarioId = principal.id();
        } else {
            // Compatibilidade com parâmetro de query id
            Map<String, String> params = HttpHelper.parseQueryParams(exchange.getRequestURI().getQuery());
            usuarioId = JsonHelper.getInt(params, "id", 0);
        }

        UsuarioResponse response = usuarioService.buscarPorId(usuarioId);
        HttpHelper.sendObjectResponse(exchange, 200, response);
    }

    private void handleAtualizarPerfil(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, Object> map = JsonHelper.parseJson(body);

        UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
        int usuarioId = (principal != null) ? principal.id() : JsonHelper.getInt(map, "id", 0);

        String nome = JsonHelper.getString(map, "nomeFantasia", "");
        String foto = JsonHelper.getString(map, "fotoUrl", "");
        String desc = JsonHelper.getString(map, "descricao", "");

        UpdateProfileRequest request = new UpdateProfileRequest(nome, foto, desc);
        UsuarioResponse atualizado = usuarioService.atualizarPerfil(usuarioId, request);

        Map<String, Object> resposta = Map.of(
                "mensagem", "Perfil atualizado com sucesso!",
                "usuario", atualizado
        );
        HttpHelper.sendObjectResponse(exchange, 200, resposta);
    }
}
