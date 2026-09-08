package com.supersohee.api.internationalresult.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "international_results")
public class InternationalResult {
    @Id
    private String id;

    @Indexed(name = "international_result_competition_key_unique", unique = true)
    private String competitionKey;
    private String competitionName;
    private String editionLabel;
    private InternationalResultCategory category;
    private InternationalResultStatus status;
    private ParticipationStatus participationStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private String teamName;
    private String teamResult;
    private String teamRecord;
    private Integer gamesPlayed;
    private String minutesPerGame;
    private Double pointsPerGame;
    private Double reboundsPerGame;
    private Double assistsPerGame;
    private Double stealsPerGame;
    private Double fieldGoalPercent;
    private Double threePointPercent;
    private Double freeThrowPercent;
    private String highlight;
    private LocalDate statsUpdatedThrough;
    private List<InternationalResultSource> sources;
    private boolean published;
    private int displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
