package com.livraria.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitário para hashing e verificação segura de senhas utilizando BCrypt (cost factor 12).
 * Fornece compatibilidade com senhas legadas e hashing automático.
 */
public class PasswordEncoder {

    private static final int LOG_ROUNDS = 12;

    /**
     * Gera o hash BCrypt de uma senha plana com salt automático.
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("A senha não pode ser nula ou em branco.");
        }
        return BCrypt.hashpw(rawPassword.trim(), BCrypt.gensalt(LOG_ROUNDS));
    }

    /**
     * Valida se uma senha plana corresponde ao hash armazenado.
     * Suporta graciosamente senhas legadas em texto plano para migração transparente.
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }

        String raw = rawPassword.trim();
        String stored = storedHash.trim();

        // Se o hash armazenado for um hash BCrypt válido ($2a$, $2b$, $2y$)
        if (isBCryptHash(stored)) {
            try {
                return BCrypt.checkpw(raw, stored);
            } catch (Exception e) {
                return false;
            }
        }

        // Compatibilidade com senhas legadas em texto puro (migração suave)
        return raw.equals(stored);
    }

    /**
     * Verifica se uma string possui o formato característico de um hash BCrypt de 60 caracteres.
     */
    public static boolean isBCryptHash(String value) {
        if (value == null || value.length() < 59) {
            return false;
        }
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
