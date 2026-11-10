package com.yunshu.zhixue.platform.security;

import com.yunshu.zhixue.platform.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {
    @GetMapping({"/api/v1/portal/auth/csrf", "/api/v1/admin/auth/csrf"})
    public ApiResponse<CsrfView> csrf(HttpServletRequest request, HttpServletResponse response, CsrfToken token) {
        request.getSession().setAttribute(RealmSessionFilter.REALM_ATTRIBUTE,
                Realm.fromPath(request.getRequestURI()).name());
        response.setHeader("Cache-Control", "no-store");
        return ApiResponse.ok(new CsrfView(token.getToken(), token.getHeaderName()));
    }

    public record CsrfView(String token, String header_name) {
        @Override
        public String toString() { return "CsrfView[token=REDACTED]"; }
    }
}
