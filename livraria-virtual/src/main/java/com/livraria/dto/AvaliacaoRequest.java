package com.livraria.dto;

/**
 * DTO para submissão de avaliação de um livro (0.5 até 5.0 estrelas) com comentário opcional.
 */
public record AvaliacaoRequest(
        int livroId,
        double nota,
        String comentario
) {}
