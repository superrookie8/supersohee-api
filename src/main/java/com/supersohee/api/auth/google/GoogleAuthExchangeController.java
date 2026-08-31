package com.supersohee.api.auth.google;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleAuthExchangeController {

    private final GoogleExchangeService exchangeService;

    @PostMapping("/exchange")
    public ResponseEntity<GoogleExchangeResponse> exchange(
            @RequestHeader(value = "X-Supersohee-Exchange-Key", required = false) String exchangeKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody GoogleExchangeRequest request) {
        return ResponseEntity.ok(exchangeService.exchange(exchangeKey, idempotencyKey, request.idToken()));
    }

    public record GoogleExchangeRequest(
            @NotBlank
            @Size(max = 16_384)
            String idToken) {
    }
}
