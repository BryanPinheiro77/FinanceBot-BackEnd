package com.financebot.auth.session;

import com.financebot.auth.dto.response.SessionActivity;
import com.financebot.auth.dto.response.SessionTokens;
import com.financebot.auth.session.persistence.*;
import com.financebot.common.exception.UnauthorizedException;
import com.financebot.security.jwt.JwtService;
import com.financebot.user.domain.User;
import com.financebot.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthSessionService {
    public static final Duration IDLE_TIMEOUT = Duration.ofHours(1);
    public static final Duration WARNING_DURATION = Duration.ofSeconds(60);
    public static final Duration MAX_SESSION = Duration.ofHours(8);
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final UserRepository users;
    private final JwtService jwt;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AuthSessionService(AuthSessionRepository sessions, RefreshTokenRepository refreshTokens,
                       UserRepository users, JwtService jwt, Clock clock) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.jwt = jwt;
        this.clock = clock;
    }

    @Transactional
    public SessionTokens create(User user) {
        Instant now = clock.instant();
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID());
        session.setUserId(user.getId());
        session.setCreatedAt(now);
        session.setLastActivityAt(now);
        session.setExpiresAt(now.plus(MAX_SESSION));
        sessions.save(session);
        return issue(session, user);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public SessionTokens refresh(String rawToken) {
        String hash = hash(rawToken);
        UUID sessionId = refreshTokens.findSessionId(hash).orElseThrow(this::unauthorized);
        AuthSession session = sessions.findLocked(sessionId).orElseThrow(this::unauthorized);
        // Primeira leitura da entidade somente depois do lock da sessão.
        RefreshToken known = refreshTokens.findById(hash).orElseThrow(this::unauthorized);
        if (!active(session)) throw unauthorized();
        if (known.getConsumedAt() != null) {
            session.setRevokedAt(clock.instant());
            throw unauthorized();
        }
        known.setConsumedAt(clock.instant());
        User user = users.findById(session.getUserId()).orElseThrow(this::unauthorized);
        return issue(session, user);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findById(hash(rawToken)).ifPresent(token ->
                sessions.findLocked(token.getSessionId()).ifPresent(session -> {
                    if (session.getRevokedAt() == null) session.setRevokedAt(clock.instant());
                }));
    }

    @Transactional(readOnly = true)
    public boolean isActive(String sessionId, Long userId) {
        try {
            Instant now = clock.instant();
            return sessions.countActive(UUID.fromString(sessionId), userId, now,
                    now.minus(IDLE_TIMEOUT).minus(WARNING_DURATION)) == 1;
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public SessionActivity status(String sessionId, Long userId) {
        AuthSession session = sessions.findById(UUID.fromString(sessionId)).orElseThrow(this::unauthorized);
        if (!session.getUserId().equals(userId) || !active(session)) throw unauthorized();
        return describe(session);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public SessionActivity activity(String sessionId, Long userId) {
        AuthSession session = sessions.findLocked(UUID.fromString(sessionId)).orElseThrow(this::unauthorized);
        if (!session.getUserId().equals(userId) || !active(session)) throw unauthorized();
        session.setLastActivityAt(clock.instant());
        return describe(session);
    }

    private SessionActivity describe(AuthSession session) {
        return new SessionActivity(session.getLastActivityAt(),
                session.getLastActivityAt().plus(IDLE_TIMEOUT),
                session.getLastActivityAt().plus(IDLE_TIMEOUT).plus(WARNING_DURATION), session.getExpiresAt());
    }

    @Scheduled(fixedDelayString = "${app.auth-session.cleanup-interval:86400000}")
    @Transactional
    public void cleanup() {
        sessions.deleteExpired(clock.instant());
    }

    private boolean active(AuthSession session) {
        Instant now = clock.instant();
        return session.getRevokedAt() == null && now.isBefore(session.getExpiresAt())
                && now.isBefore(session.getLastActivityAt().plus(IDLE_TIMEOUT).plus(WARNING_DURATION));
    }

    private SessionTokens issue(AuthSession session, User user) {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(raw));
        token.setSessionId(session.getId());
        refreshTokens.save(token);
        String access = jwt.generateToken(Map.of("sid", session.getId().toString()), user);
        return new SessionTokens(access, raw, session.getExpiresAt(), session.getLastActivityAt());
    }

    private String hash(String raw) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) throw unauthorized();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private UnauthorizedException unauthorized() {
        return new UnauthorizedException("Sessão encerrada. Entre novamente para continuar.");
    }
}
