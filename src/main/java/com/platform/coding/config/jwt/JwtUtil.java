package com.platform.coding.config.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.platform.coding.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final JwtProperties jwtProperties;

    public String createAccessKey(User user) {
        return JWT.create()
                .withSubject("AccessToken")
                .withClaim("id", user.getId())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getUserType().name())
                .withExpiresAt(Instant.now().plusSeconds(jwtProperties.getAccessTokenValidityInSeconds()))
                .sign(Algorithm.HMAC256(jwtProperties.getSecretKey()));
    }

    public String createRefreshToken(User user) {
        return JWT.create()
                .withSubject("RefreshToken")
                .withClaim("id", user.getId())
                .withExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenValidityInSeconds()))
                .sign(Algorithm.HMAC256(jwtProperties.getSecretKey()));
    }

    public DecodedJWT verify(String token) {
        return JWT.require(Algorithm.HMAC256(jwtProperties.getSecretKey()))
                .build()
                .verify(token);
    }
}
