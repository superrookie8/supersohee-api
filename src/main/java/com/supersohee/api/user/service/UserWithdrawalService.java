package com.supersohee.api.user.service;

import com.supersohee.api.arcade.repository.ArcadeScoreRepository;
import com.supersohee.api.diary.repository.DiaryCompanionCleanupRepository;
import com.supersohee.api.diary.repository.DiaryRepository;
import com.supersohee.api.image.service.ImageUploadService;
import com.supersohee.api.user.error.AccountDeletionUnavailableException;
import com.supersohee.api.user.error.RecentAuthenticationRequiredException;
import com.supersohee.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserWithdrawalService {
    private static final Duration MAXIMUM_AUTHENTICATION_AGE = Duration.ofMinutes(10);
    private static final Object[] USER_LOCKS = createLocks();

    private final UserRepository userRepository;
    private final ArcadeScoreRepository arcadeScoreRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryCompanionCleanupRepository companionCleanupRepository;
    private final ImageUploadService imageUploadService;

    public void withdraw(String userId, Instant tokenIssuedAt) {
        validateRecentAuthentication(tokenIssuedAt, Instant.now());
        synchronized (lockFor(userId)) {
            withdrawLocked(userId);
        }
    }

    void validateRecentAuthentication(Instant tokenIssuedAt, Instant now) {
        if (tokenIssuedAt == null
                || tokenIssuedAt.isAfter(now)
                || Duration.between(tokenIssuedAt, now).compareTo(MAXIMUM_AUTHENTICATION_AGE) > 0) {
            throw new RecentAuthenticationRequiredException();
        }
    }

    private void withdrawLocked(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User was not found"));

        try {
            imageUploadService.deleteAllUserProfileImages(userId);
        } catch (RuntimeException storageFailure) {
            // Keep the user document so the same authenticated account can retry.
            // Do not attach the storage exception because it can contain an object key.
            throw new AccountDeletionUnavailableException();
        }

        try {
            arcadeScoreRepository.deleteAllByUserId(userId);
            diaryRepository.deleteAllByUserId(userId);
            companionCleanupRepository.removeUserFromOtherDiaries(userId);
            userRepository.deleteById(userId);
        } catch (RuntimeException databaseFailure) {
            // Return a stable response without exposing database details or user data.
            throw new AccountDeletionUnavailableException();
        }
    }

    private static Object lockFor(String userId) {
        return USER_LOCKS[Math.floorMod(userId.hashCode(), USER_LOCKS.length)];
    }

    private static Object[] createLocks() {
        Object[] locks = new Object[64];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new Object();
        }
        return locks;
    }
}
