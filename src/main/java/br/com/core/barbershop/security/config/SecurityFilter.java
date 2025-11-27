package br.com.core.barbershop.security.config;


import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.core.barbershop.security.jwt.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SecurityFilter extends OncePerRequestFilter{

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    public TokenService tokenService;

    public SecurityFilter(TokenService tokenService){
        this.tokenService= tokenService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)throws ServletException, IOException{
        try{
             String token = parseJwt(req);

        if(token != null && tokenService.validateToken(token)){

            String username = tokenService.getUsernameFromToken(token);

            var authentication = new UsernamePasswordAuthenticationToken(username,null, Collections.emptyList());
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }catch(Exception e){
            logger.error("Erro ao autenticar o usuário: {}", e.getMessage());

            }
           filterChain.doFilter(req, res);
    }

    private String parseJwt(HttpServletRequest req){

        String header = req.getHeader("Authorization");

        if(header != null && header.startsWith("Bearer ")){
            return header.replace("Bearer ", "");
        }
        return null;
    
    }

}
