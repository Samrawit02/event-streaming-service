package com.challenge.eventstream.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    @DisplayName("Should generate valid token and extract username and userId")
    void testTokenGenerationAndParsing() {
        String username = "testuser";
        Long userId = 42L;

        String token = tokenProvider.generateToken(username, userId);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
        assertEquals(username, tokenProvider.getUsernameFromToken(token));
        assertEquals(userId, tokenProvider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("Should reject invalid or malformed tokens")
    void testInvalidTokenValidation() {
        assertFalse(tokenProvider.validateToken("invalid.token.structure"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    @DisplayName("Should detect expired token")
    void testExpiredToken() {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(secret, -1000);
        String token = shortLivedProvider.generateToken("user", 1L);
        assertFalse(shortLivedProvider.validateToken(token));
    }
}
