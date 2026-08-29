package com.financebot.security.telegram;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class TelegramInternalAuthenticationInterceptor implements HandlerInterceptor {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final byte[] expectedToken;

    public TelegramInternalAuthenticationInterceptor(
            @Value("${telegram.internal-token:}") String expectedToken
    ) {
        this.expectedToken = expectedToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        String receivedToken = request.getHeader(INTERNAL_TOKEN_HEADER);

        if (expectedToken.length == 0
                || receivedToken == null
                || !MessageDigest.isEqual(
                expectedToken,
                receivedToken.getBytes(StandardCharsets.UTF_8)
        )) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }
}
