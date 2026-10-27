package com.livraria.dto;

/**
 * DTO para criação / edição de categoria.
 */
public record CategoriaRequest(
        String nome,
        String slug,
        String descricao
) {}
