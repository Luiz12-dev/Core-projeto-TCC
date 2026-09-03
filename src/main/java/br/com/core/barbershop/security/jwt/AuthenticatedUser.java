package br.com.core.barbershop.security.jwt;

import java.security.Principal;

/**
 * Quem esta autenticado, montado a partir dos claims do JWT emitido pelo
 * servico de autenticacao.
 *
 * Implementa Principal e devolve o e-mail em getName() de proposito: os
 * controllers que ja usavam principal.getName() continuam recebendo o e-mail,
 * enquanto quem precisa de nome e telefone pode ler os campos.
 *
 * name e phone podem ser nulos — usuarios cadastrados antes da migration V2
 * do servico de autenticacao nao tem telefone.
 */
public record AuthenticatedUser(String email, String name, String phone) implements Principal {

    @Override
    public String getName() {
        return email;
    }

    /**
     * Nem todo Authentication carrega um AuthenticatedUser: em teste, a
     * anotacao WithMockUser coloca uma String como principal, e tokens
     * antigos nao trazem os claims. Nesses casos so o e-mail e conhecido.
     */
    public static AuthenticatedUser de(org.springframework.security.core.Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser usuario) {
            return usuario;
        }
        return new AuthenticatedUser(authentication.getName(), null, null);
    }
}
