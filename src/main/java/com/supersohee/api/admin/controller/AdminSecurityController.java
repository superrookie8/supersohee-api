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
                new Check("legacy-administrator-login", "pass",
                        "Static administrator login and previously issued administrator tokens are disabled. Member sessions require the current database ADMIN role."),
                new Check("jwt-validation", "pass", "Startup validates the signing key; access tokens require issuer, audience and role/type checks."),
                new Check("crawler-import-key", "pass", "Startup requires a dedicated import key; the import route checks it separately."),
                new Check("public-frontend-https", environment.getProperty("app.frontend.url", "").startsWith("https://") ? "pass" : "warn",
                        "Checks whether the configured public frontend URL uses HTTPS."),
                new Check("login-rate-limit", "warn", "Apply a login rate limit at the trusted ingress; this endpoint cannot verify ingress configuration."),
                new Check("deployment-review", "warn", "TLS, DNS, proxy trust, secret rotation and external exposure require deployment review. This is not a penetration test."));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Status(Instant.now(), checks));
    }

    public record Check(String id, String status, String message) {}
    public record Status(Instant checkedAt, List<Check> checks) {}
}
