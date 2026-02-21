package com.auctor.definition.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Instant;
import java.util.List;

public final class JwtTokenGenerator {

    private JwtTokenGenerator() {}

    public static String generate(String subject, List<String> roles) {
        String secret = getEnvOrDefault("DEFINITION_JWT_SECRET", "dev-secret-change-later-dev-secret-change-later");
        String issuer = getEnvOrDefault("DEFINITION_JWT_ISSUER", "auctor-auth");
        String audience = getEnvOrDefault("DEFINITION_JWT_AUDIENCE", "definition-service");

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(subject)
            .withClaim("roles", roles)
            .withIssuedAt(Instant.now())
            .withExpiresAt(Instant.now().plusSeconds(3600))
            .sign(Algorithm.HMAC256(secret));
    }

    private static String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
