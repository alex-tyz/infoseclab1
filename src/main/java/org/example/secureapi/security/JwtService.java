package org.example.secureapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import io.jsonwebtoken.io.Decoders;
import org.springframework.stereotype.Service;

@Service
public final class JwtService {
    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length <32) {
            throw new IllegalArgumentException("Secret key must be at least 32 bytes long");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key,Jwts.SIG.HS256)
                .compact();
    }
    public Optional<String> validateAndGetUsername(String token) {
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        }catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }

    }
}
