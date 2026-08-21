package org.gatuninhas.api.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresInMs) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInMs);
    }
}
