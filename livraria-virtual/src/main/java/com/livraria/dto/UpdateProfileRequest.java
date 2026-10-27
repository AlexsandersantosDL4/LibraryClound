package com.livraria.dto;

/**
 * DTO para atualização dos dados de perfil do usuário.
 */
public record UpdateProfileRequest(
        String nomeFantasia,
        String fotoUrl,
        String descricao
) {}
