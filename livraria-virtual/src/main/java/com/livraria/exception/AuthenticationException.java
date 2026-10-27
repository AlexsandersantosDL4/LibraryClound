package com.livraria.exception;

/**
 * Exceção lançada quando credenciais são inválidas ou o token está ausente/expirado (HTTP 401).
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
