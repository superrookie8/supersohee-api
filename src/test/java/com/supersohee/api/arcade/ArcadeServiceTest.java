package com.supersohee.api.arcade;

import com.supersohee.api.arcade.domain.ArcadeScore;
import com.supersohee.api.arcade.dto.RankingResponse;
import com.supersohee.api.arcade.dto.ScoreResponse;
import com.supersohee.api.arcade.error.ArcadeApiException;
import com.supersohee.api.arcade.repository.ArcadeScoreRepository;
import com.supersohee.api.arcade.service.ArcadeService;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArcadeServiceTest {
    private final ArcadeScoreRepository scoreRepository = mock(ArcadeScoreRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ArcadeService service = new ArcadeService(scoreRepository, userRepository);

    @Test
    void lowerScoreNeverReplacesTheExistingBestScore() {
        User user = user("user-1", "팬1");
        ArcadeScore existing = score("score-1", "user-1", 100);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(scoreRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));
        when(scoreRepository.findAllByOrderByBestScoreDesc()).thenReturn(List.of(existing));

        ScoreResponse response = service.submitScore("user-1", 80);

        assertThat(response.getBestScore()).isEqualTo(100);
        assertThat(response.getRank()).isEqualTo(1);
        verify(scoreRepository, never()).save(any());
    }

    @Test
    void higherScoreReplacesTheExistingBestScore() {
        User user = user("user-1", "팬1");
        ArcadeScore existing = score("score-1", "user-1", 100);
        ArcadeScore updated = score("score-1", "user-1", 120);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(scoreRepository.findByUserId("user-1")).thenReturn(Optional.of(existing));
        when(scoreRepository.save(any(ArcadeScore.class))).thenReturn(updated);
        when(scoreRepository.findAllByOrderByBestScoreDesc()).thenReturn(List.of(updated));

        ScoreResponse response = service.submitScore("user-1", 120);

        assertThat(response.getBestScore()).isEqualTo(120);
        verify(scoreRepository).save(any(ArcadeScore.class));
    }

    @Test
    void rankingLimitDoesNotHideTheCurrentUsersOverallRank() {
        ArcadeScore first = score("score-1", "user-1", 100);
        ArcadeScore second = score("score-2", "user-2", 80);
        when(scoreRepository.findAllByOrderByBestScoreDesc()).thenReturn(List.of(first, second));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1", "팬1")));

        RankingResponse response = service.getRanking(1, "user-2");

        assertThat(response.getRankings()).hasSize(1);
        assertThat(response.getRankings().get(0).getRank()).isEqualTo(1);
        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getMyRank()).isEqualTo(2);
    }

    @Test
    void missingUserIsRejectedBeforeAnOrphanScoreCanBeWritten() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ArcadeApiException.class)
                .isThrownBy(() -> service.submitScore("missing", 100))
                .satisfies(exception -> assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(scoreRepository, never()).save(any());
    }

    private User user(String id, String nickname) {
        return User.builder().id(id).nickname(nickname).build();
    }

    private ArcadeScore score(String id, String userId, int bestScore) {
        return ArcadeScore.builder().id(id).userId(userId).bestScore(bestScore).build();
    }
}
