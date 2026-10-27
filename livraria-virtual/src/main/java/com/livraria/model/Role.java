package com.livraria.model;

/**
 * Papéis de acesso (Roles) do sistema LibraryClound.
 */
public enum Role {
    CLIENTE,
    ADMIN;

    public static Role fromString(String role) {
        if (role == null || role.isBlank()) {
            return CLIENTE;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CLIENTE;
        }
    }
}
