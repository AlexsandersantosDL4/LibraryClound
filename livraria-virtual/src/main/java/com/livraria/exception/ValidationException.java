package com.livraria.exception;

/**
 * Exceção lançada quando dados de entrada violam regras de validação (HTTP 400).
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
