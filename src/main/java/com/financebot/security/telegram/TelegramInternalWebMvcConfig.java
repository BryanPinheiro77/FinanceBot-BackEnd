package com.financebot.security.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class TelegramInternalWebMvcConfig implements WebMvcConfigurer {

    private final TelegramInternalAuthenticationInterceptor telegramInternalAuthenticationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(telegramInternalAuthenticationInterceptor)
                .addPathPatterns("/telegram/**");
    }
}
