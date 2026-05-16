package br.com.core.barbershop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Core BarberShop API")
                        .version("1.0.0")
                        .description("""
                                API RESTful do sistema de gerenciamento de barbearia.

                                **Autenticação:** Faça login no Auth API (porta 8081) para obter o token JWT. \
                                Depois clique no botão **Authorize** acima e cole o token no formato: `seu_token_aqui`

                                **Roles disponíveis:**
                                - `ROLE_OWNER` — Dono da barbearia (gerencia serviços, horários, agenda e receita)
                                - `ROLE_CLIENT` — Cliente (agenda cortes, vê histórico, cancela agendamentos)

                                **Endpoints públicos (sem token):**
                                - GET /api/services — Listar serviços
                                - GET /api/business-hours — Horários de funcionamento
                                - GET /api/blocked-periods — Períodos bloqueados
                                - GET /api/appointments/available-slots — Horários disponíveis
                                """)
                        .contact(new Contact()
                                .name("Luiz Otávio")
                                .url("https://github.com/Luiz12-dev"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Cole aqui o token JWT obtido no login do Auth API. Não precisa colocar 'Bearer ' na frente — o Swagger adiciona automaticamente.")));
    }
}
