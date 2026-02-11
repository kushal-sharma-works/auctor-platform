package com.auctor.definition.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {

    @Test
    void shouldCreateDecoderWithSharedSecretWhenNoJwksUri() {
        JwtConfig config = new JwtConfig();
        JwtConfig.JwtProperties properties = new JwtConfig.JwtProperties(
            "test-secret",
            "issuer",
            "audience",
            ""
        );

        JwtDecoder decoder = config.jwtDecoder(properties);

        assertNotNull(decoder);
        assertInstanceOf(NimbusJwtDecoder.class, decoder);
    }

    @Test
    void shouldCreateDecoderWithJwksUriWhenProvided() {
        JwtConfig config = new JwtConfig();
        JwtConfig.JwtProperties properties = new JwtConfig.JwtProperties(
            "ignored",
            "issuer",
            "audience",
            "https://example.com/.well-known/jwks.json"
        );

        JwtDecoder decoder = config.jwtDecoder(properties);

        assertNotNull(decoder);
        assertInstanceOf(NimbusJwtDecoder.class, decoder);
    }
}
