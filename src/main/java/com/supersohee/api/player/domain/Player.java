package com.supersohee.api.player.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "players")
public class Player {

    @Id
    private String id;

    private String name;
    private String team;
    private Integer jerseyNumber;
    private String position;
    private String height; // 171cm
    private List<String> nickname; // ["슈퍼소닉", "소히힛", "발발이", "히쏘", "이파마"]
    private String features; // 빠른 스피드와 폭발적인 득점력
    private String profileImageUrl;

}
