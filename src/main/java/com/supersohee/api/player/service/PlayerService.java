package com.supersohee.api.player.service;

import com.supersohee.api.player.domain.Player;
import com.supersohee.api.player.repository.PlayerRepository;
import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.player.dto.AdminProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    // 이소희 선수 조회 (playerId 불필요, 첫 번째 선수 반환)
    public Optional<Player> findSohee() {
        return playerRepository.findAll().stream().findFirst();
    }

    // 선수 프로필 조회 (ID로, 기존 호환성 유지)
    public Optional<Player> findById(String playerId) {
        return playerRepository.findById(playerId);
    }

    public Player updateSohee(AdminProfileRequest request) {
        Player existing = findSohee().orElseThrow(() -> AdminApiException.notFound("Player profile"));
        Player updated = Player.builder()
                .id(existing.getId())
                .name(request.name())
                .team(request.team())
                .position(request.position())
                .jerseyNumber(request.jerseyNumber())
                .height(request.height())
                .nickname(List.copyOf(request.nicknames()))
                .features(request.features())
                .profileImageUrl(request.profileImageUrl())
                .build();
        return playerRepository.save(updated);
    }
}
