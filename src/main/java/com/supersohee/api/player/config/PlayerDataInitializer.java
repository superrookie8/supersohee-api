package com.supersohee.api.player.config;

import com.supersohee.api.player.domain.Player;
import com.supersohee.api.player.repository.PlayerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class PlayerDataInitializer {

    private final PlayerRepository playerRepository;

    @PostConstruct
    public void init() {
        // 이미 데이터가 있으면 스킵
        if (playerRepository.count() > 0) {
            return;
        }

        // 이소희 선수 데이터 생성
        Player player = Player.builder()
                .name("이소희")
                .team("BNK SUM")
                .jerseyNumber(6)
                .position("가드 / 듀얼가드 (PG,SG)")
                .height("171cm")
                .nickname(Arrays.asList("슈퍼소닉", "소히힛", "발발이", "히쏘", "이파마"))
                .features("빠른 스피드와 폭발적인 득점력")
                .profileImageUrl(null) // 나중에 추가
                .build();

        Player savedPlayer = playerRepository.save(player);
        System.out.println("이소희 선수 프로필이 생성되었습니다. ID: " + savedPlayer.getId());
    }
}
