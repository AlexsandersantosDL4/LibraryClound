package com.livraria.dto;

import com.livraria.model.Livro;

/**
 * DTO para retorno de livros no catálogo, leitor e painel do administrador.
 */
public record LivroResponse(
        int id,
        String titulo,
        String autor,
        int categoriaId,
        String categoria,
        int totalPaginas,
        String sinopse,
        String capaUrl,
        String conteudo,
        double mediaAvaliacao,
        int totalAvaliacoes,
        String isbn,
        String dataPublicacao,
        String criadoEm
) {
    public static LivroResponse fromEntity(Livro l) {
        if (l == null) return null;
        return new LivroResponse(
                l.getId(),
                l.getTitulo(),
                l.getAutor(),
                l.getCategoriaId(),
                l.getCategoria() != null ? l.getCategoria() : "",
                l.getTotalPaginas(),
                l.getSinopse() != null ? l.getSinopse() : "",
                l.getCapaUrl() != null ? l.getCapaUrl() : "",
                l.getConteudo() != null ? l.getConteudo() : "",
                l.getMediaAvaliacao(),
                l.getTotalAvaliacoes(),
                l.getIsbn() != null ? l.getIsbn() : "",
                l.getDataPublicacao() != null ? l.getDataPublicacao() : "",
                l.getCriadoEm() != null ? l.getCriadoEm() : ""
        );
    }
}
