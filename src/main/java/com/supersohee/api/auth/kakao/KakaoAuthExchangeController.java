package com.supersohee.api.auth.kakao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
@ConditionalOnExpression("!'${kakao.auth.client-id:}'.trim().isEmpty()")
public class KakaoAuthExchangeController {

    private final KakaoExchangeService exchangeService;

    @PostMapping("/exchange")
    public ResponseEntity<KakaoExchangeResponse> exchange(
            @RequestHeader(value = "X-Supersohee-Exchange-Key", required = false) String exchangeKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody KakaoExchangeRequest request) {
        return ResponseEntity.ok(exchangeService.exchange(exchangeKey, idempotencyKey, request.idToken()));
    }

    public record KakaoExchangeRequest(
            @NotBlank
            @Size(max = 16_384)
            String idToken) {
    }
}
