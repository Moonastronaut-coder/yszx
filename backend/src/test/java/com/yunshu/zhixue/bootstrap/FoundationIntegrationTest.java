package com.yunshu.zhixue.bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import com.yunshu.zhixue.platform.security.CsrfController.CsrfView;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FoundationIntegrationTest {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    @Autowired ObjectMapper applicationJson;

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void readinessIncludesLiveMysqlRedisAndMinio() throws Exception {
        var response = get("/actuator/health/readiness");
        assertEquals(200, response.statusCode());
        assertEquals("UP", json.readTree(response.body()).get("status").asText());
    }

    @Test
    void applicationJsonUsesFixedMillisecondUtcTimestamps() {
        assertEquals("\"2026-10-30T16:00:00.000Z\"", applicationJson.writeValueAsString(Instant.parse("2026-10-30T16:00:00Z")));
        assertEquals("\"2026-10-30T16:00:00.123Z\"", applicationJson.writeValueAsString(Instant.parse("2026-10-30T16:00:00.123456Z")));
        assertFalse(new CsrfView("sensitive-token-value", "X-CSRF-TOKEN").toString().contains("sensitive-token-value"));
    }

    @Test
    void realmSessionsUseSeparateCookiesAndRejectCrossRealmReplay() throws Exception {
        var portal = get("/api/v1/portal/auth/csrf");
        var admin = get("/api/v1/admin/auth/csrf");
        assertEquals(200, portal.statusCode());
        assertEquals(200, admin.statusCode());
        var portalCookie = portal.headers().firstValue("set-cookie").orElseThrow();
        var adminCookie = admin.headers().firstValue("set-cookie").orElseThrow();
        assertTrue(portalCookie.startsWith("PORTAL_SESSION="));
        assertTrue(adminCookie.startsWith("ADMIN_SESSION="));
        assertTrue(portalCookie.contains("HttpOnly"));
        assertTrue(portalCookie.contains("SameSite=Lax"));
        assertTrue(portalCookie.contains("Path=/api/v1/portal"));
        assertNotEquals(json.readTree(portal.body()).at("/data/token").asText(), json.readTree(admin.body()).at("/data/token").asText());
        var cookie = portalCookie.split(";", 2)[0].replace("PORTAL_SESSION=", "ADMIN_SESSION=");
        var replay = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/admin/auth/csrf"))
                .header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, replay.statusCode());
        assertEquals("SESSION_EXPIRED", json.readTree(replay.body()).get("code").asText());

        var adminToken = json.readTree(admin.body()).at("/data/token").asText();
        var portalToken = json.readTree(portal.body()).at("/data/token").asText();
        for (var attempt : Map.of(adminToken, 401, portalToken, 403).entrySet()) {
            var write = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/admin/auth/password-sessions"))
                    .header("Cookie", adminCookie.split(";", 2)[0])
                    .header("X-CSRF-TOKEN", attempt.getKey())
                    .POST(HttpRequest.BodyPublishers.ofString("{}")).header("Content-Type", "application/json").build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(attempt.getValue().intValue(), write.statusCode());
        }
    }

    @Test
    void protectedRequestsAndUnsafeRequestsAreRejectedWithTraceableEnvelopes() throws Exception {
        var response = get("/api/v1/admin/auth/session");
        assertEquals(401, response.statusCode());
        var body = json.readTree(response.body());
        assertEquals("AUTH_REQUIRED", body.get("code").asText());
        assertTrue(body.get("data").isNull());
        assertEquals(response.headers().firstValue("X-Request-ID").orElseThrow(), body.get("request_id").asText());
        var write = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/portal/auth/password-sessions"))
                .POST(HttpRequest.BodyPublishers.ofString("{}")).header("Content-Type", "application/json").build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, write.statusCode());
        assertEquals("CSRF_INVALID", json.readTree(write.body()).get("code").asText());
    }

    @Test
    void missingPublicResourcesHaveJsonErrorsAndUntrustedTraceIdsAreReplaced() throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/public/missing"))
                .header("X-Request-ID", "untrusted-client-value").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertEquals("RESOURCE_NOT_FOUND", json.readTree(response.body()).get("code").asText());
        assertNotEquals("untrusted-client-value", response.headers().firstValue("X-Request-ID").orElseThrow());
    }
}
