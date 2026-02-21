package com.auctor.definition.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AudienceValidatorTest {

    @Test
    void shouldAcceptTokenWithExpectedAudience() {
        AudienceValidator validator = new AudienceValidator("definition-service");
        Jwt token = Jwt.withTokenValue("token")
            .header("alg", "none")
            .audience(List.of("definition-service", "other"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();

        OAuth2TokenValidatorResult result = validator.validate(token);

        assertFalse(result.hasErrors());
    }

    @Test
    void shouldRejectTokenMissingAudience() {
        AudienceValidator validator = new AudienceValidator("definition-service");
        Jwt token = Jwt.withTokenValue("token")
            .header("alg", "none")
            .audience(List.of("other"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();

        OAuth2TokenValidatorResult result = validator.validate(token);

        assertTrue(result.hasErrors());
    }
}
