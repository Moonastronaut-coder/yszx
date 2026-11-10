package com.yunshu.zhixue.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

/** 每次请求只解析本端 Cookie；跨端重放另由 RealmSessionFilter 拒绝。 */
public final class RealmSessionIdResolver implements HttpSessionIdResolver {
    private final Map<Realm, CookieHttpSessionIdResolver> resolvers = new EnumMap<>(Realm.class);

    public RealmSessionIdResolver(boolean secure) {
        for (var realm : Realm.values()) {
            var serializer = new DefaultCookieSerializer();
            serializer.setCookieName(realm.name() + "_SESSION");
            serializer.setCookiePath("/api/v1/" + realm.name().toLowerCase(java.util.Locale.ROOT));
            serializer.setUseHttpOnlyCookie(true);
            serializer.setUseSecureCookie(secure);
            serializer.setSameSite("Lax");
            var resolver = new CookieHttpSessionIdResolver();
            resolver.setCookieSerializer(serializer);
            resolvers.put(realm, resolver);
        }
    }

    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        var resolver = resolvers.get(Realm.fromPath(request.getRequestURI()));
        return resolver == null ? List.of() : resolver.resolveSessionIds(request);
    }

    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String id) {
        var resolver = resolvers.get(Realm.fromPath(request.getRequestURI()));
        if (resolver != null) resolver.setSessionId(request, response, id);
    }

    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        var resolver = resolvers.get(Realm.fromPath(request.getRequestURI()));
        if (resolver != null) resolver.expireSession(request, response);
    }
}
