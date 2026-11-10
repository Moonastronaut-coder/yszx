package com.yunshu.zhixue.platform.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.session.web.http.HttpSessionIdResolver;

@Configuration
public class SecurityConfiguration {
    @Bean
    HttpSessionIdResolver sessionIdResolver(@Value("${yunshu.security.secure-cookies}") boolean secure) {
        return new RealmSessionIdResolver(secure);
    }

    @Bean
    UserDetailsService noDefaultUser() {
        return username -> { throw new UsernameNotFoundException("Authentication module not implemented"); };
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, SecurityJsonWriter writer) throws Exception {
        return http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/api/v1/public/**",
                                "/api/v1/portal/auth/csrf", "/api/v1/admin/auth/csrf").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) ->
                                writer.write(response, 401, "AUTH_REQUIRED", "请先登录"))
                        .accessDeniedHandler((request, response, error) -> writer.write(response, 403,
                                error instanceof CsrfException ? "CSRF_INVALID" : "PERMISSION_DENIED",
                                "请求未获授权")))
                .addFilterBefore(new RealmSessionFilter(writer), SecurityContextHolderFilter.class)
                .build();
    }
}
