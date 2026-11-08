package com.team5.onlineshopping.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_KEY_BYTES = 32; // 256 bits for HMAC-SHA256

    @Value("${security.jwt.secret:}")
    private String secretKey;

    @Value("${security.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @PostConstruct
    public void validateConfiguration() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT secret key is missing. Set the 'JWT_SECRET' environment variable with at least 256 bits (32 bytes)."
            );
        }

        byte[] keyBytes = resolveKeyBytes(secretKey);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT secret key is too short. Expected at least " + MIN_KEY_BYTES + " bytes (256 bits), but got "
                            + keyBytes.length + " bytes."
            );
        }

        log.info("JWT service initialized successfully with a valid {}-bit signing key.", keyBytes.length * 8);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof CustomUserDetails customUser) {
            extraClaims.put("userId", customUser.getId());
            extraClaims.put("role", customUser.getRole().name());
        }
        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSignInKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = resolveKeyBytes(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveKeyBytes(String rawSecret) {
        try {
            return Decoders.BASE64.decode(rawSecret);
        } catch (Exception e) {
            return rawSecret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
