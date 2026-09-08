package com.supersohee.api.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminSecurityController {
    private final Environment environment;

    @GetMapping("/api/admin/security/status")
    public ResponseEntity<Status> status(Authentication authentication) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        var checks = List.of(
                new Check("administrator-authorization", admin ? "pass" : "fail",
                        admin ? "This request has administrator permission; member tokens are checked against the current database role on every admin request." : "Administrator permission is missing."),
                new Check("production-profile", production ? "pass" : "warn",
                        production ? "Production profile is active." : "Production profile is not active."),
                new Check("legacy-administrator-login", "warn",
                        "Code policy disables legacy administrator credentials; this status request did not exercise that boundary."),
                new Check("jwt-validation", "warn", "Code policy validates JWT issuer, audience and role/type. Consult authentication fixtures; this is not an active invalid-token test."),
                new Check("crawler-import-key", environment.getProperty("article.import.key", environment.getProperty("SUPERSOHEE_ARTICLE_IMPORT_KEY", "")).getBytes(java.nio.charset.StandardCharsets.UTF_8).length >= 32 ? "pass" : "fail", "Checked dedicated import key presence and minimum length; no request was sent."),
                new Check("public-frontend-https", environment.getProperty("app.frontend.url", "").startsWith("https://") ? "pass" : "warn",
                        "Checks whether the configured public frontend URL uses HTTPS."),
                new Check("login-rate-limit", "warn", "Apply a login rate limit at the trusted ingress; this endpoint cannot verify ingress configuration."),
                new Check("deployment-review", "warn", "TLS, DNS, proxy trust, secret rotation and external exposure require deployment review. This is not a penetration test."));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Status(Instant.now(), checks));
    }

    public record Check(String id, String status, String message) {}
    public record Status(Instant checkedAt, List<Check> checks) {}
}
