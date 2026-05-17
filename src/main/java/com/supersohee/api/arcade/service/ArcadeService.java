package com.supersohee.api.arcade.service;

import com.supersohee.api.arcade.domain.ArcadeScore;
import com.supersohee.api.arcade.dto.RankingResponse;
import com.supersohee.api.arcade.dto.ScoreResponse;
import com.supersohee.api.arcade.repository.ArcadeScoreRepository;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ArcadeService {

        private final ArcadeScoreRepository arcadeScoreRepository;
        private final UserRepository userRepository;

        /**
         * 점수 제출 및 업데이트
         * 기존 최고 점수보다 높은 경우에만 업데이트
         */
        @Transactional
        public ScoreResponse submitScore(String userId, Integer score) {
                Optional<ArcadeScore> existingScore = arcadeScoreRepository.findByUserId(userId);

                ArcadeScore arcadeScore;
                if (existingScore.isPresent()) {
                        // 기존 점수가 더 높으면 업데이트하지 않음
                        if (existingScore.get().getBestScore() >= score) {
                                arcadeScore = existingScore.get();
                        } else {
                                // 새로운 최고 점수로 업데이트
                                arcadeScore = ArcadeScore.builder()
                                                .id(existingScore.get().getId())
                                                .userId(userId)
                                                .bestScore(score)
                                                .build();
                                arcadeScore = arcadeScoreRepository.save(arcadeScore);
                        }
                } else {
                        // 새로운 점수 기록
                        arcadeScore = ArcadeScore.builder()
                                        .userId(userId)
                                        .bestScore(score)
                                        .build();
                        arcadeScore = arcadeScoreRepository.save(arcadeScore);
                }

                // 사용자 정보 조회
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

                // 랭킹 계산
                List<ArcadeScore> allScores = arcadeScoreRepository.findAllByOrderByBestScoreDesc();
                int rank = IntStream.range(0, allScores.size())
                                .filter(i -> allScores.get(i).getUserId().equals(userId))
                                .findFirst()
                                .orElse(-1) + 1;

                return ScoreResponse.builder()
                                .userId(userId)
                                .nickname(user.getNickname())
                                .profileImageUrl(user.getProfileImageUrl())
                                .bestScore(arcadeScore.getBestScore())
                                .rank(rank > 0 ? rank : null)
                                .build();
        }

        /**
         * 내 최고 점수 조회
         */
        public ScoreResponse getMyScore(String userId) {
                ArcadeScore arcadeScore = arcadeScoreRepository.findByUserId(userId)
                                .orElse(null);

                if (arcadeScore == null) {
                        // 점수가 없으면 null 반환
                        User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

                        return ScoreResponse.builder()
                                        .userId(userId)
                                        .nickname(user.getNickname())
                                        .profileImageUrl(user.getProfileImageUrl())
                                        .bestScore(null)
                                        .rank(null)
                                        .build();
                }

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

                // 랭킹 계산
                List<ArcadeScore> allScores = arcadeScoreRepository.findAllByOrderByBestScoreDesc();
                int rank = IntStream.range(0, allScores.size())
                                .filter(i -> allScores.get(i).getUserId().equals(userId))
                                .findFirst()
                                .orElse(-1) + 1;

                return ScoreResponse.builder()
                                .userId(userId)
                                .nickname(user.getNickname())
                                .profileImageUrl(user.getProfileImageUrl())
                                .bestScore(arcadeScore.getBestScore())
                                .rank(rank > 0 ? rank : null)
                                .build();
        }

        /**
         * 랭킹 조회 (상위 N명)
         */
        public RankingResponse getRanking(Integer limit, String currentUserId) {
                List<ArcadeScore> allScores = arcadeScoreRepository.findAllByOrderByBestScoreDesc();

                // limit이 지정되면 상위 N명만
                List<ArcadeScore> scores = (limit != null && limit > 0)
                                ? allScores.stream().limit(limit).collect(Collectors.toList())
                                : allScores;

                // 랭킹 엔트리 생성
                List<RankingResponse.RankingEntry> rankings = IntStream.range(0, scores.size())
                                .mapToObj(i -> {
                                        ArcadeScore score = scores.get(i);
                                        User user = userRepository.findById(score.getUserId())
                                                        .orElse(null);

                                        if (user == null) {
                                                return null;
                                        }

                                        return RankingResponse.RankingEntry.builder()
                                                        .rank(i + 1)
                                                        .userId(score.getUserId())
                                                        .nickname(user.getNickname())
                                                        .profileImageUrl(user.getProfileImageUrl())
                                                        .bestScore(score.getBestScore())
                                                        .build();
                                })
                                .filter(entry -> entry != null)
                                .collect(Collectors.toList());

                // 현재 사용자의 랭킹 계산
                Integer myRank = null;
                if (currentUserId != null) {
                        int rank = IntStream.range(0, allScores.size())
                                        .filter(i -> allScores.get(i).getUserId().equals(currentUserId))
                                        .findFirst()
                                        .orElse(-1) + 1;
                        myRank = rank > 0 ? rank : null;
                }

                return RankingResponse.builder()
                                .rankings(rankings)
                                .totalCount(allScores.size())
                                .myRank(myRank)
                                .build();
        }
}
