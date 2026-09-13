package com.flashsale.inventory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        gatewayUserAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public OncePerRequestFilter gatewayUserAuthenticationFilter() {

        return new OncePerRequestFilter() {

            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, IOException {

                String userId = request.getHeader("X-User-Id");
                String rolesHeader = request.getHeader("X-User-Roles");

                if (userId != null
                        && !userId.isBlank()
                        && rolesHeader != null
                        && !rolesHeader.isBlank()) {

                    List<SimpleGrantedAuthority> authorities =
                            Arrays.stream(rolesHeader.split(","))
                                    .map(String::trim)
                                    .filter(role -> !role.isBlank())
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    authorities
                            );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }

                filterChain.doFilter(request, response);
            }
        };
    }
}