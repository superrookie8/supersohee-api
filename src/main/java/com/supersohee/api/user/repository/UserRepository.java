package com.supersohee.api.user.repository;


import com.supersohee.api.user.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;


public interface UserRepository extends MongoRepository<User, String> {
    // OAuth
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    
    // 이메일로 조회
    Optional<User> findByEmail(String email);
}
