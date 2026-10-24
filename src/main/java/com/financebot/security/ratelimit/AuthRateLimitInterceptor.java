package com.financebot.security.ratelimit;

import com.financebot.auth.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthRateLimitInterceptor implements HandlerInterceptor {
    private final AuthRateLimitService rateLimit;
    private final AuthClientIpResolver clientIp;
    private final ObjectMapper json;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod method) || !"POST".equals(request.getMethod())
                || !AuthController.class.isAssignableFrom(method.getBeanType())) return true;
        String operation = method.getMethod().getName();
        if (!operation.equals("login") && !operation.equals("register")) return true;

        long retryAfter;
        try {
            retryAfter = rateLimit.acquire(operation, clientIp.resolve(request));
        } catch (DataAccessException | IllegalStateException exception) {
            log.warn("Contador de tentativas de autenticação indisponível");
            reject(request, response, 503, "Service unavailable",
                    "Autenticação temporariamente indisponível. Tente novamente em instantes.", 5);
            return false;
        }
        if (retryAfter == 0) return true;
        reject(request, response, 429, "Too many requests",
                "Limite de tentativas de autenticação excedido. Aguarde antes de tentar novamente.", retryAfter);
        return false;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, int status,
                        String title, String detail, long retryAfter) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", Long.toString(retryAfter));
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(response.getOutputStream(), Map.of("type", "about:blank", "title", title,
                "status", status, "detail", detail, "instance", request.getRequestURI()));
    }
}
