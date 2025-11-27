package br.com.core.barbershop.security.jwt;

import java.security.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class tokenService {

    private static final Logger logger = LoggerFactory.getLogger(tokenService.class);

    @Value("${api.security.token.secret}")
    private String secret;


    public Key getSignedKey(String token){
        return Keys.hmacShaKeyFor(token.getBytes());
    }


    public boolean validateToken(String token){
        try{

            Jwts.parserBuilder()
        .setSigningKey(getSignedKey(secret))
        .build()
        .parseClaimsJws(token);
        return true;

        }catch(Exception e){
            logger.error("Token expired or not valid: {}", e.getMessage());
            return false;
        }
        
    }


    public String getUsernameFromToken(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignedKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

}
