package com.livraria.exception;

/**
 * Exceção lançada quando o usuário autenticado não possui privilégios para a ação (HTTP 403).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
