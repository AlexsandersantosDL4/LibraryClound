package com.livraria.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.livraria.model.Role;
import com.livraria.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Serviço responsável por emissão, validação e extração de dados de Tokens JWT (Auth0).
 */
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private static final String DEFAULT_SECRET = "libraryclound-super-secret-jwt-key-2026-auth-token";
    private static final String ISSUER = "LibraryClound";
    private static final long EXPIRATION_HOURS = 24;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = DEFAULT_SECRET;
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
    }

    /**
     * Gera um Token JWT para o usuário autenticado com validade de 24 horas.
     */
    public String gerarToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(EXPIRATION_HOURS, ChronoUnit.HOURS);

        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(usuario.getId()))
                .withClaim("email", usuario.getEmail())
                .withClaim("nomeFantasia", usuario.getNomeFantasia())
                .withClaim("role", usuario.getTipo())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    /**
     * Valida a assinatura e expiração do Token JWT e extrai o UserPrincipal.
     */
    public UserPrincipal validarToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String rawToken = token.trim();
        if (rawToken.startsWith("Bearer ") || rawToken.startsWith("bearer ")) {
            rawToken = rawToken.substring(7).trim();
        }

        try {
            DecodedJWT jwt = verifier.verify(rawToken);
            int id = Integer.parseInt(jwt.getSubject());
            String email = jwt.getClaim("email").asString();
            String nomeFantasia = jwt.getClaim("nomeFantasia").asString();
            String roleStr = jwt.getClaim("role").asString();
            Role role = Role.fromString(roleStr);

            return new UserPrincipal(id, email, nomeFantasia, role);
        } catch (JWTVerificationException e) {
            logger.warn("Falha na validação do Token JWT: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Erro inesperado ao decodificar token JWT: {}", e.getMessage());
            return null;
        }
    }
}
