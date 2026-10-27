package com.livraria.dto;

/**
 * DTO para requisição de cadastro de novo usuário.
 */
public record RegisterRequest(
        String nomeFantasia,
        String email,
        String senha,
        String tipo
) {}
