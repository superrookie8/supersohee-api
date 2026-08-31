package com.supersohee.api.user.service;

import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import com.supersohee.api.user.error.InvalidNicknameException;
import com.supersohee.api.user.error.NicknameAlreadyUsedException;

@Service
@RequiredArgsConstructor

public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 일반 회원가입
    @Transactional
    public User signup(String email, String password, String passwordConfirm, String nickname) {
        // 비밀번호 확인
        if (!password.equals(passwordConfirm)) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        // 이메일 중복 확인
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // 신규 유저 생성
        User newUser = User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .provider(null) // 일반 회원가입은 provider 없음
                .providerId(null)
                .points(0)
                .level(1)
                .build();

        return userRepository.save(newUser);
    }

    // 일반 로그인 (이메일/비밀번호)
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // OAuth 유저는 일반 로그인 불가
        if (user.getProvider() != null) {
            throw new RuntimeException("OAuth 로그인을 사용해주세요");
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        return user;
    }

    // OAuth 로그인/회원가입 처리
    @Transactional
    public User findOrCreateUser(String provider, String providerId,
            String email, String nickname, String profileImageUrl) {
        // 기존 유저 조회
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);

        if (existingUser.isPresent()) {
            // 사용자가 마이페이지에서 정한 닉네임/사진은 유지한다.
            User user = existingUser.get();
            User updatedUser = user.syncProviderProfile(email, nickname, profileImageUrl);
            return userRepository.save(updatedUser);
        }

        // 신규 유저 생성
        User newUser = User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                // 구글 표시이름은 대개 본명이라 닉네임 기본값으로 쓰지 않는다.
                // 랭킹·방명록은 닉네임을 공개하므로, 사용자가 마이페이지에서 직접
                // 정하기 전까지는 신원을 드러내지 않는 이름을 준다.
                .nickname(generatedNickname())
                .profileImageUrl(profileImageUrl) // null이어도 괜찮음 (MongoDB에 null로 저장)
                .password(null)
                .points(0)
                .level(1)
                .build();

        try {
            return userRepository.save(newUser);
        } catch (DuplicateKeyException duplicateKeyException) {
            // A concurrent exchange may have inserted the same provider subject.
            return userRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseThrow(() -> duplicateKeyException);
        }
    }

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 20;
    private static final Pattern NICKNAME_PATTERN =
            Pattern.compile("^[\\p{IsHangul}a-zA-Z0-9._-]+$");

    /** 마이페이지 프로필 수정. null은 "변경하지 않음"을 뜻한다. */
    @Transactional
    public User updateMyProfile(String userId, String nickname, String profileImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        String requested = nickname == null ? null : nickname.trim();
        if (requested != null && !requested.equals(user.getNickname())) {
            validateNickname(requested);
            if (userRepository.existsByNickname(requested)) {
                throw new NicknameAlreadyUsedException();
            }
        }

        try {
            return userRepository.save(user.withProfileEdits(requested, profileImageUrl));
        } catch (DuplicateKeyException duplicateKeyException) {
            // 동시에 같은 닉네임을 저장한 경우 유니크 인덱스가 최종 방어선이다.
            throw new NicknameAlreadyUsedException();
        }
    }

    /** 본인이 이미 쓰고 있는 닉네임은 사용 가능으로 본다. */
    public boolean isNicknameAvailable(String nickname, String currentUserId) {
        String candidate = nickname == null ? "" : nickname.trim();
        validateNickname(candidate);
        return userRepository.findByNickname(candidate)
                .map(owner -> owner.getId().equals(currentUserId))
                .orElse(true);
    }

    private void validateNickname(String nickname) {
        if (nickname == null
                || nickname.length() < NICKNAME_MIN_LENGTH
                || nickname.length() > NICKNAME_MAX_LENGTH
                || !NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new InvalidNicknameException();
        }
    }

    /**
     * 아직 비어 있는 임의의 닉네임을 만든다.
     *
     * 닉네임은 유일해야 하므로 이미 쓰이는 값이면 다시 뽑는다. 충돌이 이어져도
     * 로그인이 막히지 않도록 시도 횟수를 제한하고 마지막에는 접미사를 붙인다.
     */
    private String generatedNickname() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "팬" + ThreadLocalRandom.current().nextInt(100000, 1000000);
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
        String base = "팬" + ThreadLocalRandom.current().nextInt(100000, 1000000);
        for (int suffix = 1; ; suffix++) {
            String candidate = base + "-" + suffix;
            if (candidate.length() <= NICKNAME_MAX_LENGTH
                    && !userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }
    }

    // 유저 조회
    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    // 현재 로그인한 유저 조회 (인증 정보에서)
    public Optional<User> getCurrentUser(String userId) {
        return userRepository.findById(userId);
    }

    // 이메일 중복 확인
    public boolean isEmailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

}
