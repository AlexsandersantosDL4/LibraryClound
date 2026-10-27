package com.livraria.dto;

import com.livraria.model.Avaliacao;

/**
 * DTO para retorno de avaliação em estrelas com comentário.
 */
public record AvaliacaoResponse(
        int id,
        int usuarioId,
        String usuarioNome,
        int livroId,
        double nota,
        String comentario,
        String dataAvaliacao
) {
    public static AvaliacaoResponse fromEntity(Avaliacao a) {
        if (a == null) return null;
        return new AvaliacaoResponse(
                a.getId(),
                a.getUsuarioId(),
                a.getUsuarioNome() != null ? a.getUsuarioNome() : "",
                a.getLivroId(),
                a.getNota(),
                a.getComentario() != null ? a.getComentario() : "",
                a.getDataAvaliacao() != null ? a.getDataAvaliacao() : ""
        );
    }
}
