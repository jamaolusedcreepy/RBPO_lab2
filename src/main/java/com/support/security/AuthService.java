package com.support.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(AppUserRepository userRepository,
                       UserSessionRepository sessionRepository,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Аутентифицирует пользователя и возвращает пару токенов.
     * Создаёт новую сессию в БД.
     */
    @Transactional
    public TokenPairResponse login(String username, String rawPassword) {
        // Валидация учётных данных через Spring Security
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, rawPassword)
        );

        AppUser user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        return createTokenPair(user);
    }

    /**
     * Обновляет пару токенов по refresh-токену.
     * Реализует rotation: старая сессия → REVOKED, новая сессия → ACTIVE.
     */
    @Transactional
    public TokenPairResponse refresh(String refreshToken) {
        // 1. Парсим и проверяем подпись
        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(refreshToken);
        } catch (JwtException e) {
            throw new SecurityException("Invalid or expired refresh token");
        }

        // 2. Проверяем что это именно refresh-токен
        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new SecurityException("Token is not a refresh token");
        }

        // 3. Находим сессию по jti
        String jti = claims.getId();
        UserSession session = sessionRepository.findByJti(jti)
                .orElseThrow(() -> new SecurityException("Session not found"));

        // 4. Проверяем статус сессии
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new SecurityException("Session is " + session.getStatus() + ". Please log in again.");
        }

        // 5. Проверяем что срок сессии ещё не истёк
        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            session.setStatus(SessionStatus.EXPIRED);
            sessionRepository.save(session);
            throw new SecurityException("Session expired. Please log in again.");
        }

        // 6. Проверяем хэш refresh-токена (защита от подмены)
        String receivedHash = hashToken(refreshToken);
        if (!receivedHash.equals(session.getRefreshTokenHash())) {
            throw new SecurityException("Refresh token hash mismatch");
        }

        // 7. Отзываем старую сессию (token rotation)
        session.setStatus(SessionStatus.REVOKED);
        session.setLastUsedAt(LocalDateTime.now());
        sessionRepository.save(session);

        // 8. Создаём новую пару
        AppUser user = session.getAppUser();
        return createTokenPair(user);
    }

    // --- вспомогательные методы ---

    private TokenPairResponse createTokenPair(AppUser user) {
        String jti = UUID.randomUUID().toString();

        String accessToken  = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername(), jti);

        UserSession session = new UserSession();
        session.setAppUser(user);
        session.setJti(jti);
        session.setRefreshTokenHash(hashToken(refreshToken));
        session.setStatus(SessionStatus.ACTIVE);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusSeconds(
                jwtTokenProvider.getRefreshTokenExpirationMs() / 1000));
        sessionRepository.save(session);

        return new TokenPairResponse(accessToken, refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
