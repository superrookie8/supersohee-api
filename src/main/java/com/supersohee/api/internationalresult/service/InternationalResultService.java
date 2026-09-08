package com.supersohee.api.internationalresult.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultStatus;
import com.supersohee.api.internationalresult.dto.InternationalResultRequest;
import com.supersohee.api.internationalresult.repository.InternationalResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternationalResultService {
    private final InternationalResultRepository repository;

    public List<InternationalResult> findPublished() {
        return repository.findByPublishedTrueOrderByDisplayOrderAscStartDateDesc();
    }

    public List<InternationalResult> findAllForAdmin() {
        return repository.findAllByOrderByDisplayOrderAscStartDateDesc();
    }

    public InternationalResult create(InternationalResultRequest request) {
        validateBusinessRules(request);
        if (repository.findByCompetitionKey(request.competitionKey()).isPresent()) {
            throw AdminApiException.conflict("An international result already exists for this competition key.");
        }
        LocalDateTime now = LocalDateTime.now();
        return repository.save(toDocument(null, request, now, now));
    }

    public InternationalResult update(String id, InternationalResultRequest request) {
        validateBusinessRules(request);
        InternationalResult existing = repository.findById(id)
                .orElseThrow(() -> AdminApiException.notFound("International result"));
        repository.findByCompetitionKey(request.competitionKey())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw AdminApiException.conflict(
                            "An international result already exists for this competition key.");
                });
        return repository.save(toDocument(id, request, existing.getCreatedAt(), LocalDateTime.now()));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw AdminApiException.notFound("International result");
        }
        repository.deleteById(id);
    }

    void validateBusinessRules(InternationalResultRequest request) {
        var fields = new LinkedHashMap<String, String>();
        if (request.startDate() != null && request.endDate() != null
                && request.startDate().isAfter(request.endDate())) {
            fields.put("endDate", "must be on or after startDate");
        }
        if (request.status() == InternationalResultStatus.FINAL) {
            if (request.gamesPlayed() == null) {
                fields.put("gamesPlayed", "is required when status is FINAL");
            }
            if (request.teamResult() == null || request.teamResult().isBlank()) {
                fields.put("teamResult", "is required when status is FINAL");
            }
        }
        if (request.status() == InternationalResultStatus.IN_PROGRESS
                && request.statsUpdatedThrough() == null) {
            fields.put("statsUpdatedThrough", "is required when status is IN_PROGRESS");
        }
        if (!fields.isEmpty()) {
            throw AdminApiException.unprocessable("International result validation failed.", fields);
        }
    }

    private InternationalResult toDocument(
            String id,
            InternationalResultRequest request,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return InternationalResult.builder()
                .id(id)
                .competitionKey(request.competitionKey().trim())
                .competitionName(request.competitionName().trim())
                .editionLabel(request.editionLabel().trim())
                .category(request.category())
                .status(request.status())
                .participationStatus(request.participationStatus())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .location(request.location().trim())
                .teamName(request.teamName().trim())
                .teamResult(trimNullable(request.teamResult()))
                .teamRecord(trimNullable(request.teamRecord()))
                .gamesPlayed(request.gamesPlayed())
                .minutesPerGame(trimNullable(request.minutesPerGame()))
                .pointsPerGame(request.pointsPerGame())
                .reboundsPerGame(request.reboundsPerGame())
                .assistsPerGame(request.assistsPerGame())
                .stealsPerGame(request.stealsPerGame())
                .fieldGoalPercent(request.fieldGoalPercent())
                .threePointPercent(request.threePointPercent())
                .freeThrowPercent(request.freeThrowPercent())
                .highlight(trimNullable(request.highlight()))
                .statsUpdatedThrough(request.statsUpdatedThrough())
                .sources(request.sources().stream().map(source -> source.toDomain()).toList())
                .published(request.published())
                .displayOrder(request.displayOrder())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private static String trimNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
