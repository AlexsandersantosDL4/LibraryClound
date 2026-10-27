package com.livraria.security;

import com.livraria.model.Role;

/**
 * Representa o usuário autenticado extraído do Token JWT.
 */
public record UserPrincipal(
        int id,
        String email,
        String nomeFantasia,
        Role role
) {
    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }
}
