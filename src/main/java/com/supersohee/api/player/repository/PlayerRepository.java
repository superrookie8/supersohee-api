package com.supersohee.api.player.repository;

import com.supersohee.api.player.domain.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlayerRepository extends MongoRepository<Player, String> {
    
}
