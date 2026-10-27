package com.livraria.controller;

import com.livraria.dto.LivroRequest;
import com.livraria.dto.LivroResponse;
import com.livraria.dto.UsuarioResponse;
import com.livraria.security.JwtService;
import com.livraria.security.UserPrincipal;
import com.livraria.service.LivroService;
import com.livraria.service.UsuarioService;
import com.livraria.util.HttpHelper;
import com.livraria.util.JsonHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller com privilégios de Administrador/Donos do site LibraryClound:
 * Permite adicionar, modificar e excluir livros livremente,
 * incluindo o conteúdo completo do livro (independente de quantas páginas),
 * além de auditar todos os usuários cadastrados.
 */
public class AdminController implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final LivroService livroService = new LivroService();
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
            // Verificação opcional de privilégios de Administrador caso token seja fornecido
            UserPrincipal principal = HttpHelper.getAuthenticatedUser(exchange, jwtService);
            if (principal != null && !principal.isAdmin()) {
                HttpHelper.sendJsonResponse(exchange, 403, "{\"erro\":\"Acesso restrito ao Administrador/Dono da LibraryClound.\"}");
                return;
            }

            if (path.startsWith("/api/admin/livros")) {
                handleLivrosCrud(exchange, method, path);
            } else if (path.startsWith("/api/admin/usuarios") && "GET".equalsIgnoreCase(method)) {
                handleListarUsuarios(exchange);
            } else {
                HttpHelper.sendJsonResponse(exchange, 404, "{\"erro\":\"Endpoint administrativo não encontrado\"}");
            }
        } catch (Exception e) {
            HttpHelper.sendError(exchange, e);
        }
    }

    private void handleLivrosCrud(HttpExchange exchange, String method, String path) throws IOException {
        if ("POST".equalsIgnoreCase(method)) {
            LivroRequest request = HttpHelper.readJsonBody(exchange, LivroRequest.class);
            LivroResponse salvo = livroService.salvar(request);

            Map<String, Object> resposta = Map.of(
                    "mensagem", "Livro adicionado com sucesso pelo administrador!",
                    "livro", salvo
            );
            HttpHelper.sendObjectResponse(exchange, 201, resposta);

        } else if ("PUT".equalsIgnoreCase(method)) {
            int id = extrairIdDaUrl(path);
            LivroRequest request = HttpHelper.readJsonBody(exchange, LivroRequest.class);
            LivroResponse atualizado = livroService.atualizar(id, request);

            Map<String, Object> resposta = Map.of(
                    "mensagem", "Livro atualizado com sucesso!",
                    "livro", atualizado
            );
            HttpHelper.sendObjectResponse(exchange, 200, resposta);

        } else if ("DELETE".equalsIgnoreCase(method)) {
            int id = extrairIdDaUrl(path);
            livroService.excluir(id);

            Map<String, Object> resposta = Map.of(
                    "mensagem", "Livro excluído com sucesso do catálogo!",
                    "sucesso", true
            );
            HttpHelper.sendObjectResponse(exchange, 200, resposta);
        } else {
            HttpHelper.sendJsonResponse(exchange, 405, "{\"erro\":\"Método não suportado para /api/admin/livros\"}");
        }
    }

    private void handleListarUsuarios(HttpExchange exchange) throws IOException {
        List<UsuarioResponse> usuarios = usuarioService.listarTodos();
        HttpHelper.sendObjectResponse(exchange, 200, usuarios);
    }

    private int extrairIdDaUrl(String path) {
        try {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                return Integer.parseInt(path.substring(lastSlash + 1));
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }
}
