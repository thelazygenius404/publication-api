package com.smaservices.publication_api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Publication API",
                version = "1.0.0",
                description = """
                        API REST de gestion et de publication automatisée
                        de contenus vers LinkedIn et WordPress.

                        Fonctionnalités principales :
                        - authentification JWT ;
                        - gestion des contenus ;
                        - validation humaine ;
                        - planification des publications ;
                        - publication LinkedIn et WordPress via n8n ;
                        - assistance rédactionnelle avec Google Gemini.
                        """,
                contact = @Contact(
                        name = "SMA Services"
                )
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT obtenu via POST /auth/login"
)
public class OpenApiConfig {
}