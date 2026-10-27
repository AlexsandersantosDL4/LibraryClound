package com.livraria.exception;

/**
 * Exceção de negócio genérica para violação de regras da LibraryClound (HTTP 400/422).
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
