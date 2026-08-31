package com.supersohee.api.player.controller;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.player.dto.AdminProfileRequest;
import com.supersohee.api.player.dto.AdminProfileResponse;
import com.supersohee.api.player.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {
    private final PlayerService playerService;

    @GetMapping
    public AdminProfileResponse getProfile() {
        return playerService.findSohee()
                .map(AdminProfileResponse::from)
                .orElseThrow(() -> AdminApiException.notFound("Player profile"));
    }

    @PutMapping
    public AdminProfileResponse updateProfile(@Valid @RequestBody AdminProfileRequest request) {
        return AdminProfileResponse.from(playerService.updateSohee(request));
    }
}
