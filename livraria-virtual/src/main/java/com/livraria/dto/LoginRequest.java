package com.livraria.dto;

/**
 * DTO para requisição de autenticação / login.
 */
public record LoginRequest(
        String email,
        String senha
) {}
