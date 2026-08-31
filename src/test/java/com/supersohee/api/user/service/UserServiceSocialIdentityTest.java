package com.supersohee.api.user.service;

import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceSocialIdentityTest {

    @Test
    void sameGoogleSubjectReturnsSameUser() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));
        User persisted = googleUser("user-1", "google-subject", "fan@example.test");

        when(repository.findByProviderAndProviderId("google", "google-subject"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(persisted));
        when(repository.save(any(User.class))).thenReturn(persisted);

        User first = service.findOrCreateUser(
                "google", "google-subject", "fan@example.test", "Fan", null);
        User second = service.findOrCreateUser(
                "google", "google-subject", "fan@example.test", "Fan", null);

        assertThat(first.getId()).isEqualTo("user-1");
        assertThat(second.getId()).isEqualTo("user-1");
    }

    @Test
    void differentGoogleSubjectsWithSameEmailAreNotMerged() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));

        when(repository.findByProviderAndProviderId("google", "subject-1"))
                .thenReturn(Optional.empty());
        when(repository.findByProviderAndProviderId("google", "subject-2"))
                .thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User candidate = invocation.getArgument(0, User.class);
            return googleUser(
                    "subject-1".equals(candidate.getProviderId()) ? "user-1" : "user-2",
                    candidate.getProviderId(),
                    candidate.getEmail());
        });

        User first = service.findOrCreateUser(
                "google", "subject-1", "shared@example.test", "First", null);
        User second = service.findOrCreateUser(
                "google", "subject-2", "shared@example.test", "Second", null);

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getProviderId()).isEqualTo("subject-1");
        assertThat(second.getProviderId()).isEqualTo("subject-2");
        verify(repository, never()).findByEmail("shared@example.test");
    }

    @Test
    void missingUnverifiedEmailDoesNotErasePreviouslyVerifiedEmail() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));
        User persisted = googleUser("user-1", "google-subject", "verified@example.test");

        when(repository.findByProviderAndProviderId("google", "google-subject"))
                .thenReturn(Optional.of(persisted));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        User result = service.findOrCreateUser(
                "google", "google-subject", null, "Fan", null);

        assertThat(result.getEmail()).isEqualTo("verified@example.test");
    }

    @Test
    void newGoogleSubjectCanBeCreatedWithoutEmail() {
        UserRepository repository = mock(UserRepository.class);
        UserService service = new UserService(repository, mock(PasswordEncoder.class));

        when(repository.findByProviderAndProviderId("google", "google-subject"))
                .thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));

        User result = service.findOrCreateUser(
                "google", "google-subject", null, "Fan", null);

        assertThat(result.getProviderId()).isEqualTo("google-subject");
        assertThat(result.getEmail()).isNull();
    }

    private User googleUser(String id, String subject, String email) {
        return User.builder()
                .id(id)
                .provider("google")
                .providerId(subject)
                .email(email)
                .nickname("Fan")
                .points(0)
                .level(1)
                .build();
    }
}
