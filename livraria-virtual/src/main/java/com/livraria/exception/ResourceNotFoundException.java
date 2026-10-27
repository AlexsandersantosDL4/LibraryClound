package com.livraria.exception;

/**
 * Exceção lançada quando um recurso solicitado não é encontrado no banco de dados (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
