package com.supersohee.api.admin.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Static administrator credentials are retired; use a member session and database role. */
@RestController
public class AdminAuthController {
    @PostMapping("/api/admin/login")
    public ResponseEntity<Map<String, Object>> disabledLogin() {
        return ResponseEntity.status(403).cacheControl(CacheControl.noStore()).body(Map.of(
                "status", 403,
                "code", "ADMIN_LEGACY_LOGIN_DISABLED",
                "message", "Use your member login with an assigned administrator role."));
    }
}
