package com.auctor.definition.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableConfigurationProperties(JwtConfig.JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        JwtDecoder decoder;
        if (properties.jwksUri() != null && !properties.jwksUri().isBlank()) {
            decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUri()).build();
        } else {
            SecretKey key = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            decoder = NimbusJwtDecoder.withSecretKey(key).build();
        }

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(properties.audience());
        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);

        ((NimbusJwtDecoder) decoder).setJwtValidator(validators);
        return decoder;
    }

    @ConfigurationProperties(prefix = "security.jwt")
    public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        String jwksUri
    ) {
    }
}
