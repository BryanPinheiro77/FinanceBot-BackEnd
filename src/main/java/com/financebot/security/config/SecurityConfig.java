package com.financebot.security.config;

import com.financebot.security.jwt.JwtAuthenticationEntryPoint;
import com.financebot.security.jwt.JwtAuthenticationFilter;
import com.financebot.security.userdetails.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.DispatcherTypeRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;

import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          CustomUserDetailsService customUserDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(new DispatcherTypeRequestMatcher(DispatcherType.ERROR)).permitAll()
                        .requestMatchers(
                                "/auth/**",
                                "/error",
                                "/api/health",
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/users/telegram/confirm-link"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/telegram/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/telegram/installments/summary").permitAll()
                        .requestMatchers(HttpMethod.GET, "/telegram/installments/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/telegram/accounts/default").permitAll()
                        .requestMatchers(HttpMethod.GET, "/telegram/users/me").permitAll()
                        .requestMatchers(HttpMethod.GET, "/telegram/financial-analysis").permitAll()
                        .requestMatchers(HttpMethod.GET, "/telegram/expenses/current-month").permitAll()
                        .requestMatchers(HttpMethod.GET, "/telegram/income/current-month").permitAll()
                        .requestMatchers(HttpMethod.POST, "/telegram/transactions").permitAll()
                        .requestMatchers(HttpMethod.POST, "/telegram/transactions/installments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/telegram/transactions/summary").permitAll()
                        .requestMatchers(HttpMethod.POST, "/telegram/installments/count").permitAll()
                        .requestMatchers(HttpMethod.POST, "/telegram/installments/purchase-capacity").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/telegram/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/telegram/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
