package com.supersohee.api.player.service;

import com.supersohee.api.player.domain.Player;
import com.supersohee.api.player.dto.AdminProfileRequest;
import com.supersohee.api.player.repository.PlayerRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerServiceAdminTest {

    @Test
    void updatesExistingSingletonWithoutCreatingAnotherPlayer() {
        PlayerRepository repository = mock(PlayerRepository.class);
        PlayerService service = new PlayerService(repository);
        when(repository.findAll()).thenReturn(List.of(Player.builder().id("player-1").build()));
        when(repository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0, Player.class));

        Player result = service.updateSohee(new AdminProfileRequest(
                "이소희", "BNK 썸", "G", 6, "171cm", List.of("소히"), "빠른 가드", null));

        assertThat(result.getId()).isEqualTo("player-1");
        assertThat(result.getJerseyNumber()).isEqualTo(6);
        assertThat(result.getNickname()).containsExactly("소히");
    }
}
