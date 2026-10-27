package com.livraria.dto;

/**
 * DTO de resposta para autenticação e cadastro, contendo o Token JWT.
 */
public record AuthResponse(
        String mensagem,
        String token,
        UsuarioResponse usuario
) {}
