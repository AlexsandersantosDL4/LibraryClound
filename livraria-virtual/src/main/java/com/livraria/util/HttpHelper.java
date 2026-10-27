package com.livraria.util;

import com.livraria.dto.ErrorResponse;
import com.livraria.exception.AuthenticationException;
import com.livraria.exception.BusinessException;
import com.livraria.exception.ForbiddenException;
import com.livraria.exception.ResourceNotFoundException;
import com.livraria.exception.ValidationException;
import com.livraria.security.JwtService;
import com.livraria.security.UserPrincipal;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário centralizado para protocolo HTTP, CORS, serialização Jackson e tratamento de erros.
 */
public class HttpHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class);

    public static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public static <T> T readJsonBody(HttpExchange exchange, Class<T> clazz) throws IOException {
        String body = readRequestBody(exchange);
        if (body.isBlank()) {
            throw new ValidationException("Corpo da requisição está vazio.");
        }
        try {
            return JsonHelper.fromJson(body, clazz);
        } catch (Exception e) {
            throw new ValidationException("Formato JSON inválido: " + e.getMessage());
        }
    }

    public static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = (jsonResponse != null ? jsonResponse : "").getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if (bytes.length == 0) {
            exchange.sendResponseHeaders(statusCode, -1);
        } else {
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
                os.flush();
            }
        }
        exchange.close();
    }

    public static void sendObjectResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = JsonHelper.toJson(data);
        sendJsonResponse(exchange, statusCode, json);
    }

    public static void sendError(HttpExchange exchange, Exception ex) throws IOException {
        int status;
        String erro;
        String detalhes = ex.getMessage();

        if (ex instanceof ValidationException) {
            status = 400;
            erro = "Requisição Inválida";
        } else if (ex instanceof AuthenticationException) {
            status = 401;
            erro = "Não Autenticado";
        } else if (ex instanceof ForbiddenException) {
            status = 403;
            erro = "Acesso Proibido";
        } else if (ex instanceof ResourceNotFoundException) {
            status = 404;
            erro = "Não Encontrado";
        } else if (ex instanceof BusinessException) {
            status = 400;
            erro = "Regra de Negócio Violada";
        } else if (ex instanceof SQLException) {
            status = 500;
            erro = "Erro no Banco de Dados";
            logger.error("Erro SQL na requisição: {}", ex.getMessage(), ex);
        } else {
            status = 500;
            erro = "Erro Interno do Servidor";
            logger.error("Erro inesperado na requisição: {}", ex.getMessage(), ex);
        }

        ErrorResponse errorResponse = ErrorResponse.of(status, erro, detalhes);
        sendObjectResponse(exchange, status, errorResponse);
    }

    public static void handleCorsPreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    public static UserPrincipal getAuthenticatedUser(HttpExchange exchange, JwtService jwtService) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        return jwtService.validarToken(authHeader);
    }

    public static UserPrincipal requireAuthenticatedUser(HttpExchange exchange, JwtService jwtService) {
        UserPrincipal user = getAuthenticatedUser(exchange, jwtService);
        if (user == null) {
            throw new AuthenticationException("Autenticação necessária. Token JWT ausente ou inválido.");
        }
        return user;
    }

    public static UserPrincipal requireAdminUser(HttpExchange exchange, JwtService jwtService) {
        UserPrincipal user = requireAuthenticatedUser(exchange, jwtService);
        if (!user.isAdmin()) {
            throw new ForbiddenException("Acesso restrito a administradores do sistema.");
        }
        return user;
    }

    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = (idx < pair.length() - 1)
                        ? URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8)
                        : "";
                params.put(key, value);
            } else {
                params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }
}
