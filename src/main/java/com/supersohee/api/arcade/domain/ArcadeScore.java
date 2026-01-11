package com.supersohee.api.arcade.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "arcade_scores")
public class ArcadeScore {

    @Id
    private String id;

    private String userId;
    private Integer bestScore;
}

