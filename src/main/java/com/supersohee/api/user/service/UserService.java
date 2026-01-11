package com.supersohee.api.user.service;

import com.supersohee.api.user.domain.User;
import com.supersohee.api.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

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
            // 기존 유저 정보 업데이트
            User user = existingUser.get();
            User updatedUser = user.updateProfile(email, nickname, profileImageUrl);
            return userRepository.save(updatedUser);
        }

        // 신규 유저 생성
        User newUser = User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl) // null이어도 괜찮음 (MongoDB에 null로 저장)
                .password(null)
                .points(0)
                .level(1)
                .build();

        return userRepository.save(newUser);
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
