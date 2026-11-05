package com.financebot.auth.controller;

import com.financebot.auth.dto.response.AuthResponse;
import com.financebot.auth.dto.request.LoginRequest;
import com.financebot.auth.dto.request.RegisterRequest;
import com.financebot.auth.service.AuthService;
import com.financebot.auth.session.AuthSessionService;
import com.financebot.auth.dto.request.RefreshRequest;
import com.financebot.auth.dto.response.SessionTokens;
import com.financebot.auth.dto.response.SessionActivity;
import com.financebot.security.jwt.JwtService;
import com.financebot.user.domain.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthSessionService sessions;
    private final JwtService jwt;

    public AuthController(AuthService authService, AuthSessionService sessions, JwtService jwt) {
        this.authService = authService;
        this.sessions = sessions;
        this.jwt = jwt;
    }

    @PostMapping("/refresh")
    public ResponseEntity<SessionTokens> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(sessions.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshRequest request) {
        sessions.logout(request.refreshToken());
        return ResponseEntity.noContent().cacheControl(org.springframework.http.CacheControl.noStore()).build();
    }

    @GetMapping("/session")
    public SessionActivity session(@AuthenticationPrincipal User user,
            @RequestHeader("Authorization") String authorization) {
        String sid = jwt.extractClaim(authorization.substring(7), claims -> claims.get("sid", String.class));
        return sessions.status(sid, user.getId());
    }

    @PostMapping("/activity")
    public SessionActivity activity(@AuthenticationPrincipal User user,
            @RequestHeader("Authorization") String authorization) {
        String sid = jwt.extractClaim(authorization.substring(7), claims -> claims.get("sid", String.class));
        return sessions.activity(sid, user.getId());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(org.springframework.http.CacheControl.noStore()).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(response);
    }
}