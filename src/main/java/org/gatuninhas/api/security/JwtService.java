package org.gatuninhas.api.security;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.gatuninhas.api.model.User;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import lombok.RequiredArgsConstructor;

/**
 * Encapsula a geracao e validacao dos JWTs de access e refresh.
 * Access e refresh usam ALGORITMOS/SECRETS DIFERENTES de proposito: assim,
 * mesmo que o secret de um vaze, o outro tipo de token continua seguro.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ISSUER = "gatuninhas-api";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getEmail())
                .withClaim(CLAIM_ROLE, user.getRole().name())
                .withClaim(CLAIM_TYPE, TYPE_ACCESS)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(jwtProperties.accessExpirationMs()))
                .sign(accessAlgorithm());
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.getEmail())
                .withClaim(CLAIM_TYPE, TYPE_REFRESH)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(jwtProperties.refreshExpirationMs()))
                .sign(refreshAlgorithm());
    }

    /** Valida assinatura + expiracao + claim "type" de um access token e retorna o email (subject). */
    public String parseAndValidateAccessToken(String token) {
        DecodedJWT decoded = JWT.require(accessAlgorithm())
                .withIssuer(ISSUER)
                .withClaim(CLAIM_TYPE, TYPE_ACCESS)
                .build()
                .verify(token);
        return decoded.getSubject();
    }

    /** Valida assinatura + expiracao + claim "type" de um refresh token e retorna o email (subject). */
    public String parseAndValidateRefreshToken(String token) {
        DecodedJWT decoded = JWT.require(refreshAlgorithm())
                .withIssuer(ISSUER)
                .withClaim(CLAIM_TYPE, TYPE_REFRESH)
                .build()
                .verify(token);
        return decoded.getSubject();
    }

    public boolean isTokenValid(String token, boolean isRefreshToken) {
        try {
            if (isRefreshToken) {
                parseAndValidateRefreshToken(token);
            } else {
                parseAndValidateAccessToken(token);
            }
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    public long getRefreshExpirationMs() {
        return jwtProperties.refreshExpirationMs();
    }

    public Instant refreshExpiryFromNow() {
        return Instant.now().plusMillis(jwtProperties.refreshExpirationMs());
    }

    private Algorithm accessAlgorithm() {
        return Algorithm.HMAC256(jwtProperties.accessSecret());
    }

    private Algorithm refreshAlgorithm() {
        return Algorithm.HMAC256(jwtProperties.refreshSecret());
    }
}
