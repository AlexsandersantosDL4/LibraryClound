package com.livraria.dto;

import java.time.Instant;

/**
 * DTO para respostas padronizadas de erro na API REST.
 */
public record ErrorResponse(
        int status,
        String erro,
        String detalhes,
        String timestamp
) {
    public static ErrorResponse of(int status, String erro, String detalhes) {
        return new ErrorResponse(status, erro, detalhes, Instant.now().toString());
    }

    public static ErrorResponse of(int status, String erro) {
        return of(status, erro, null);
    }
}
