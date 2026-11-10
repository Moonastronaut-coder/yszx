package com.yunshu.zhixue.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public final class RealmSessionFilter extends OncePerRequestFilter {
    public static final String REALM_ATTRIBUTE = "yunshu.realm";
    private final SecurityJsonWriter writer;
    public RealmSessionFilter(SecurityJsonWriter writer) { this.writer = writer; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var realm = Realm.fromPath(request.getRequestURI());
        var session = request.getSession(false);
        if (realm != null && session != null && !realm.name().equals(session.getAttribute(REALM_ATTRIBUTE))) {
            writer.write(response, 401, "SESSION_EXPIRED", "会话不适用于当前入口");
            return;
        }
        chain.doFilter(request, response);
    }
}
