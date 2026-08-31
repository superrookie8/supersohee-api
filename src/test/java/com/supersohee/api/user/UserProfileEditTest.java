package com.supersohee.api.user;

import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.error.InvalidNicknameException;
import com.supersohee.api.user.error.NicknameAlreadyUsedException;
import com.supersohee.api.user.repository.UserRepository;
import com.supersohee.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserProfileEditTest {

    private final Map<String, User> byId = new HashMap<>();
    private final Map<String, User> byNickname = new HashMap<>();
    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository, mock(PasswordEncoder.class));

        when(userRepository.findById(anyString()))
                .thenAnswer(call -> Optional.ofNullable(byId.get(call.getArgument(0, String.class))));
        when(userRepository.findByNickname(anyString()))
                .thenAnswer(call -> Optional.ofNullable(byNickname.get(call.getArgument(0, String.class))));
        when(userRepository.existsByNickname(anyString()))
                .thenAnswer(call -> byNickname.containsKey(call.getArgument(0, String.class)));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0, User.class));
    }

    private User store(String id, String nickname, String imageUrl) {
        User user = User.builder()
                .id(id)
                .provider("google")
                .providerId("sub-" + id)
                .email(id + "@example.com")
                .nickname(nickname)
                .profileImageUrl(imageUrl)
                .build();
        byId.put(id, user);
        if (nickname != null) byNickname.put(nickname, user);
        return user;
    }

    // 예전 구현은 로그인마다 updateProfile(email, 구글이름, 사진)을 호출해
    // 사용자가 정한 닉네임을 Google 계정 이름으로 되돌렸다.
    @Test
    void loginDoesNotOverwriteNicknameOrPhotoChosenByTheUser() {
        User user = store("u1", "직관요정", "https://cdn.example/mine.png");

        User synced = user.syncProviderProfile(
                "u1@example.com", "Google Display Name", "https://google/photo.png");

        assertThat(synced.getNickname()).isEqualTo("직관요정");
        assertThat(synced.getProfileImageUrl()).isEqualTo("https://cdn.example/mine.png");
    }

    // 랭킹·방명록이 닉네임을 공개하므로, 구글 표시이름(대개 본명)은 어떤 경로로도
    // 닉네임이 되지 않아야 한다. 사진은 아직 없을 때만 provider 값을 받는다.
    @Test
    void loginNeverAdoptsTheProviderDisplayNameAsNickname() {
        User user = store("u2", null, null);

        User synced = user.syncProviderProfile(
                "u2@example.com", "홍길동", "https://google/photo.png");

        assertThat(synced.getNickname()).isNull();
        assertThat(synced.getProfileImageUrl()).isEqualTo("https://google/photo.png");
    }

    @Test
    void updatingProfileChangesNicknameAndPhoto() {
        store("u3", "옛닉네임", null);

        User updated = userService.updateMyProfile("u3", "새닉네임", "https://cdn.example/new.png");

        assertThat(updated.getNickname()).isEqualTo("새닉네임");
        assertThat(updated.getProfileImageUrl()).isEqualTo("https://cdn.example/new.png");
    }

    @Test
    void nullMeansUnchangedAndBlankImageResetsToDefault() {
        store("u4", "그대로", "https://cdn.example/old.png");

        User untouched = userService.updateMyProfile("u4", null, null);
        assertThat(untouched.getNickname()).isEqualTo("그대로");
        assertThat(untouched.getProfileImageUrl()).isEqualTo("https://cdn.example/old.png");

        User cleared = userService.updateMyProfile("u4", null, "");
        assertThat(cleared.getProfileImageUrl()).isNull();
    }

    @Test
    void takenNicknameIsRejected() {
        store("owner", "인기닉네임", null);
        store("u5", "내닉네임", null);

        assertThatThrownBy(() -> userService.updateMyProfile("u5", "인기닉네임", null))
                .isInstanceOf(NicknameAlreadyUsedException.class);
    }

    @Test
    void keepingYourOwnNicknameIsAllowed() {
        store("u6", "내닉네임", null);

        assertThat(userService.updateMyProfile("u6", "내닉네임", null).getNickname())
                .isEqualTo("내닉네임");
        assertThat(userService.isNicknameAvailable("내닉네임", "u6")).isTrue();
        assertThat(userService.isNicknameAvailable("내닉네임", "someone-else")).isFalse();
    }

    @Test
    void nicknameRulesRejectTooShortTooLongAndDisallowedCharacters() {
        store("u7", "정상닉", null);

        for (String invalid : new String[] {"x", "가".repeat(21), "빈 칸", "이모지🎉", "슬래시/"}) {
            assertThatThrownBy(() -> userService.updateMyProfile("u7", invalid, null))
                    .as(invalid)
                    .isInstanceOf(InvalidNicknameException.class);
        }
    }

    // 가입 시점에도 본명이 새어나가면 안 된다. 구글 이름을 넘겨도 쓰이지 않아야 한다.
    @Test
    void signupGeneratesAnAnonymousNicknameInsteadOfTheRealName() {
        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        User created = userService.findOrCreateUser(
                "google", "sub-new", "new@example.com", "홍길동", null);

        assertThat(created.getNickname()).doesNotContain("홍길동");
        assertThat(created.getNickname()).startsWith("팬");
        assertThat(created.getNickname().length()).isBetween(2, 20);
    }

    // 생성된 닉네임이 이미 쓰이고 있어도 로그인이 막히지 않아야 한다.
    @Test
    void signupRetriesUntilTheGeneratedNicknameIsFree() {
        when(userRepository.findByProviderAndProviderId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNickname(anyString()))
                .thenReturn(true, true, false);

        User created = userService.findOrCreateUser(
                "google", "sub-retry", "retry@example.com", "홍길동", null);

        assertThat(created.getNickname()).startsWith("팬");
    }
}
