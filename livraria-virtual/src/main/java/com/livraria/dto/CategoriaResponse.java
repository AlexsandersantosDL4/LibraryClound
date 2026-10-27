package com.livraria.dto;

import com.livraria.model.Categoria;

/**
 * DTO de retorno para categorias de livros.
 */
public record CategoriaResponse(
        int id,
        String nome,
        String slug,
        String descricao,
        int totalLivros
) {
    public static CategoriaResponse fromEntity(Categoria c) {
        if (c == null) return null;
        return new CategoriaResponse(
                c.getId(),
                c.getNome(),
                c.getSlug(),
                c.getDescricao() != null ? c.getDescricao() : "",
                c.getTotalLivros()
        );
    }
}
