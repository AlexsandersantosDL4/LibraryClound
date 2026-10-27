package com.livraria.dto;

import com.livraria.model.MarcaPagina;

/**
 * DTO para retorno do progresso de leitura.
 */
public record MarcaPaginaResponse(
        int id,
        int usuarioId,
        int livroId,
        String livroTitulo,
        int paginaAtual,
        int totalPaginas,
        int progressoPercentual,
        boolean finalizado,
        String dataAtualizacao
) {
    public static MarcaPaginaResponse fromEntity(MarcaPagina mp) {
        if (mp == null) return null;
        return new MarcaPaginaResponse(
                mp.getId(),
                mp.getUsuarioId(),
                mp.getLivroId(),
                mp.getLivroTitulo() != null ? mp.getLivroTitulo() : "",
                mp.getPaginaAtual(),
                mp.getTotalPaginas(),
                mp.getProgressoPercentual(),
                mp.isFinalizado(),
                mp.getDataAtualizacao() != null ? mp.getDataAtualizacao() : ""
        );
    }
}
