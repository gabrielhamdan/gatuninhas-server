package org.gatuninhas.api.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.gatuninhas.api.dto.LoginRequest;
import org.gatuninhas.api.dto.RegisterRequest;
import org.gatuninhas.api.dto.TokenResponse;
import org.gatuninhas.api.model.RefreshToken;
import org.gatuninhas.api.model.Role;
import org.gatuninhas.api.model.User;
import org.gatuninhas.api.repository.RefreshTokenRepository;
import org.gatuninhas.api.repository.UserRepository;
import org.gatuninhas.api.security.JwtService;

import com.auth0.jwt.exceptions.JWTVerificationException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe um usuario com este e-mail");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));

        return issueTokenPair(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        String email;
        try {
            email = jwtService.parseAndValidateRefreshToken(refreshTokenValue);
        } catch (JWTVerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido ou expirado");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token desconhecido"));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revogado ou expirado");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao encontrado"));

        // rotacao: revoga o token usado e emite um par novo
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokenPair(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private TokenResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiryDate(jwtService.refreshExpiryFromNow())
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return TokenResponse.of(accessToken, refreshTokenValue, jwtService.getRefreshExpirationMs());
    }
}
