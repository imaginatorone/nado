package com.solarl.nado.config;

import com.solarl.nado.security.JwtAuthenticationFilter;
import com.solarl.nado.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * dual-mode: AUTH_MODE=legacy (переходный) | keycloak (целевой).
 * после миграции legacy mode будет удалён.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthModeProperties authModeProperties;
    private final KeycloakProperties keycloakProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"Требуется авторизация\"}");
                })
            .and()
            .authorizeRequests()
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/auth-config").permitAll()
                // phone reveal — только авторизованные, до wildcard /ads/**
                .antMatchers(HttpMethod.GET, "/ads/*/phone").authenticated()
                .antMatchers(HttpMethod.GET, "/ads/**").permitAll()
                .antMatchers(HttpMethod.GET, "/categories/**").permitAll()
                .antMatchers(HttpMethod.GET, "/images/**").permitAll()
                .antMatchers(HttpMethod.GET, "/ratings/**").permitAll()
                .antMatchers(HttpMethod.GET, "/auctions/**").permitAll()
                .antMatchers(HttpMethod.GET, "/trust/**").permitAll()
                .antMatchers(HttpMethod.GET, "/captcha").permitAll()
                .antMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .antMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .antMatchers("/phone-verification/**").authenticated()
                .antMatchers("/moderation/**").hasAnyRole("MODERATOR", "ADMIN")
                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated();

        if (authModeProperties.isKeycloak()) {
            log.info("auth mode: KEYCLOAK");
            http.oauth2ResourceServer()
                    .jwt()
                    .jwtAuthenticationConverter(keycloakJwtConverter());
        } else {
            log.info("auth mode: LEGACY (transitional)");
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    /** маппинг realm_access.roles → Spring ROLE_* authorities */
    private Converter<Jwt, AbstractAuthenticationToken> keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object roles = realmAccess.get("roles");
                if (roles instanceof Collection) {
                    ((Collection<?>) roles).forEach(role ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()))
                    );
                }
            }

            List<String> realmRoles = jwt.getClaim("realm_roles");
            if (realmRoles != null) {
                realmRoles.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );
            }

            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return authorities;
        });
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (authModeProperties.isKeycloak()) {
            return JwtDecoders.fromIssuerLocation(keycloakProperties.getIssuerUri());
        }
        // в legacy mode decoder не используется, но Spring требует bean
        return token -> { throw new UnsupportedOperationException("legacy auth mode"); };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
