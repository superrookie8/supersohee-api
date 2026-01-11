package com.supersohee.api.game.service;

import com.supersohee.api.game.domain.Game;
import com.supersohee.api.game.repository.GameRepository;
import com.supersohee.api.stadium.domain.Stadium;
import com.supersohee.api.stadium.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final StadiumRepository stadiumRepository;

    // 사직실내체육관에서 열리는 경기인지 확인
    public boolean isHomeGame(Game game) {
        if (game.getStadiumId() == null) {
            return false;
        }
        Optional<Stadium> stadium = stadiumRepository.findById(game.getStadiumId());
        if (stadium.isEmpty()) {
            return false;
        }
        // 경기장 이름이 "부산 사직실내체육관"이면 홈경기
        return "부산 사직실내체육관".equals(stadium.get().getName());
    }

    // 시즌별 경기 목록 조회
    public List<Game> findBySeason(String season) {
        return gameRepository.findBySeasonOrderByGameDateTimeAsc(season);
    }

    // 특정 경기 조회
    public Optional<Game> findById(String gameId) {
        return gameRepository.findById(gameId);
    }

    // 경기장별 경기 조회
    public List<Game> findByStadiumId(String stadiumId) {
        return gameRepository.findByStadiumIdOrderByGameDateTimeAsc(stadiumId);
    }
}