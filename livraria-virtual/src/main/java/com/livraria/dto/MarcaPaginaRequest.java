package com.livraria.dto;

/**
 * DTO para salvar o progresso de leitura / marca-páginas.
 */
public record MarcaPaginaRequest(
        int livroId,
        int paginaAtual
) {}
