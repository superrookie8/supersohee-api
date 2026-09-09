package com.supersohee.api.user.service;

import com.supersohee.api.arcade.repository.ArcadeScoreRepository;
import com.supersohee.api.diary.repository.DiaryCompanionCleanupRepository;
import com.supersohee.api.diary.repository.DiaryRepository;
import com.supersohee.api.image.service.ImageUploadService;
import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.error.AccountDeletionUnavailableException;
import com.supersohee.api.user.error.RecentAuthenticationRequiredException;
import com.supersohee.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserWithdrawalServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final ArcadeScoreRepository scores = mock(ArcadeScoreRepository.class);
    private final DiaryRepository diaries = mock(DiaryRepository.class);
    private final DiaryCompanionCleanupRepository companions = mock(DiaryCompanionCleanupRepository.class);
    private final ImageUploadService images = mock(ImageUploadService.class);
    private final UserWithdrawalService service = new UserWithdrawalService(users, scores, diaries, companions, images);

    @Test
    void deletesOwnedDataAndUserLast() {
        User user = User.builder().id("user-1").profileImageUrl("profile/user-1/photo.webp").build();
        when(users.findById("user-1")).thenReturn(Optional.of(user));

        service.withdraw("user-1", Instant.now().minus(1, ChronoUnit.MINUTES));

        var ordered = inOrder(images, scores, diaries, companions, users);
        ordered.verify(images).deleteAllUserProfileImages("user-1");
        ordered.verify(scores).deleteAllByUserId("user-1");
        ordered.verify(diaries).deleteAllByUserId("user-1");
        ordered.verify(companions).removeUserFromOtherDiaries("user-1");
        ordered.verify(users).deleteById("user-1");
    }

    @Test
    void storageFailureKeepsAllDatabaseRecordsForRetry() {
        when(users.findById("user-1")).thenReturn(Optional.of(
                User.builder().id("user-1").profileImageUrl("profile/user-1/photo.webp").build()));
        org.mockito.Mockito.doThrow(new IllegalStateException("key must not escape"))
                .when(images).deleteAllUserProfileImages("user-1");

        assertThatThrownBy(() -> service.withdraw("user-1", Instant.now()))
                .isInstanceOf(AccountDeletionUnavailableException.class)
                .hasMessageNotContaining("key must not escape");
        verify(scores, never()).deleteAllByUserId("user-1");
        verify(diaries, never()).deleteAllByUserId("user-1");
        verify(companions, never()).removeUserFromOtherDiaries("user-1");
        verify(users, never()).deleteById("user-1");
    }

    @Test
    void databaseFailureReturnsSafeRetryableErrorAndDoesNotDeleteUserEarly() {
        when(users.findById("user-1")).thenReturn(Optional.of(User.builder().id("user-1").build()));
        org.mockito.Mockito.doThrow(new IllegalStateException("collection and user detail"))
                .when(diaries).deleteAllByUserId("user-1");

        assertThatThrownBy(() -> service.withdraw("user-1", Instant.now()))
                .isInstanceOf(AccountDeletionUnavailableException.class)
                .hasMessageNotContaining("collection and user detail");

        verify(scores).deleteAllByUserId("user-1");
        verify(diaries).deleteAllByUserId("user-1");
        verify(companions, never()).removeUserFromOtherDiaries("user-1");
        verify(users, never()).deleteById("user-1");
    }

    @Test
    void acceptsTenMinutesExactlyAndRejectsOlderOrFutureTokens() {
        Instant now = Instant.parse("2026-09-09T12:00:00Z");
        assertThatCode(() -> service.validateRecentAuthentication(now.minus(10, ChronoUnit.MINUTES), now))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateRecentAuthentication(
                now.minus(10, ChronoUnit.MINUTES).minusMillis(1), now))
                .isInstanceOf(RecentAuthenticationRequiredException.class);
        assertThatThrownBy(() -> service.validateRecentAuthentication(now.plusMillis(1), now))
                .isInstanceOf(RecentAuthenticationRequiredException.class);
    }
}
