package com.supersohee.api.player.dto;

import com.supersohee.api.player.domain.Player;

import java.util.List;

public record AdminProfileResponse(
        String id,
        String name,
        String team,
        String position,
        Integer jerseyNumber,
        Integer nationalTeamJerseyNumber,
        String height,
        List<String> nicknames,
        String features,
        String profileImageUrl) {
    public static AdminProfileResponse from(Player player) {
        return new AdminProfileResponse(
                player.getId(), player.getName(), player.getTeam(), player.getPosition(),
                player.getJerseyNumber(), player.getNationalTeamJerseyNumber(), player.getHeight(),
                player.getNickname() != null ? player.getNickname() : List.of(),
                player.getFeatures(), player.getProfileImageUrl());
    }
}
