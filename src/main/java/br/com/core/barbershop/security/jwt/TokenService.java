package br.com.core.barbershop.security.jwt;

import java.security.Key;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    @Value("${jwt-secret-key}")
    private String secret;

    private Key getSignedKey(String token) {
        return Keys.hmacShaKeyFor(token.getBytes());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSignedKey(secret))
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            logger.error("Token expired or not valid: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignedKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    /**
     * Monta o usuario autenticado a partir dos claims. name e phone so existem
     * em tokens emitidos apos a V2 do servico de autenticacao; nos antigos
     * vem nulos, e quem consome decide o fallback.
     */
    public AuthenticatedUser getAuthenticatedUser(String token) {
        Claims claims = parse(token);
        return new AuthenticatedUser(
                claims.getSubject(),
                claims.get("name", String.class),
                claims.get("phone", String.class));
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignedKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignedKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("roles", List.class);
    }
}

