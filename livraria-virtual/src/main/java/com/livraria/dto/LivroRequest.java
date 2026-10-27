package com.livraria.dto;

/**
 * DTO para cadastro e atualização de livros (Painel do Administrador).
 */
public record LivroRequest(
        String titulo,
        String autor,
        int categoriaId,
        String categoria,
        int totalPaginas,
        String sinopse,
        String capaUrl,
        String conteudo,
        String isbn,
        String dataPublicacao
) {}
