package com.livraria.dto;

import com.livraria.model.Usuario;

/**
 * DTO de resposta para representação segura de dados de usuário (sem senha).
 */
public record UsuarioResponse(
        int id,
        String nomeFantasia,
        String email,
        String tipo,
        int livrosLidos,
        String fotoUrl,
        String descricao,
        String criadoEm
) {
    public static UsuarioResponse fromEntity(Usuario u) {
        if (u == null) return null;
        return new UsuarioResponse(
                u.getId(),
                u.getNomeFantasia(),
                u.getEmail(),
                u.getTipo(),
                u.getLivrosLidos(),
                u.getFotoUrl() != null ? u.getFotoUrl() : "",
                u.getDescricao() != null ? u.getDescricao() : "",
                u.getCriadoEm() != null ? u.getCriadoEm() : ""
        );
    }
}
