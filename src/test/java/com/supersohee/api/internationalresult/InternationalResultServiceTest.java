package com.supersohee.api.internationalresult;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultCategory;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
import com.supersohee.api.internationalresult.domain.InternationalResultStatus;
import com.supersohee.api.internationalresult.domain.ParticipationStatus;
import com.supersohee.api.internationalresult.dto.InternationalResultRequest;
import com.supersohee.api.internationalresult.dto.InternationalResultSourceRequest;
import com.supersohee.api.internationalresult.repository.InternationalResultRepository;
import com.supersohee.api.internationalresult.service.InternationalResultService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternationalResultServiceTest {
    private final InternationalResultRepository repository = mock(InternationalResultRepository.class);
    private final InternationalResultService service = new InternationalResultService(repository);

    @Test
    void createPersistsNormalizedFieldsAndTimestamps() {
        when(repository.findByCompetitionKey("2025-wbl-asia")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InternationalResult created = service.create(validRequest(InternationalResultStatus.FINAL));

        assertThat(created.getCompetitionKey()).isEqualTo("2025-wbl-asia");
        assertThat(created.getTeamResult()).isEqualTo("3위");
        assertThat(created.getTeamRecord()).isNull();
        assertThat(created.getSources()).singleElement().satisfies(source -> {
            assertThat(source.getLabel()).isEqualTo("FIBA 기록");
            assertThat(source.getUrl()).isEqualTo("https://www.fiba.basketball/event");
        });
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isEqualTo(created.getCreatedAt());
        verify(repository).save(created);
    }

    @Test
    void enforcesStatusSpecificAndChronologicalRulesBeforeWriting() {
        InternationalResultRequest finalWithoutResult = copy(validRequest(InternationalResultStatus.FINAL),
                InternationalResultStatus.FINAL, null, null,
                LocalDate.of(2025, 9, 29), LocalDate.of(2025, 9, 28));

        assertThatThrownBy(() -> service.create(finalWithoutResult))
                .isInstanceOf(AdminApiException.class)
                .satisfies(error -> assertThat(((AdminApiException) error).fieldErrors())
                        .containsKeys("endDate", "gamesPlayed", "teamResult"));

        InternationalResultRequest inProgressWithoutCutoff = copy(validRequest(InternationalResultStatus.IN_PROGRESS),
                InternationalResultStatus.IN_PROGRESS, null, 3,
                LocalDate.of(2025, 9, 23), LocalDate.of(2025, 9, 28));
        assertThatThrownBy(() -> service.create(inProgressWithoutCutoff))
                .isInstanceOf(AdminApiException.class)
                .satisfies(error -> assertThat(((AdminApiException) error).fieldErrors())
                        .containsKey("statsUpdatedThrough"));
    }

    @Test
    void updatePreservesCreationTimeAndRejectsAnotherStableKeyOwner() {
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 1, 12, 0);
        InternationalResult existing = InternationalResult.builder()
                .id("result-1").competitionKey("old-key").createdAt(createdAt).build();
        when(repository.findById("result-1")).thenReturn(Optional.of(existing));
        when(repository.findByCompetitionKey("2025-wbl-asia")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InternationalResult updated = service.update("result-1", validRequest(InternationalResultStatus.FINAL));
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(createdAt);

        when(repository.findByCompetitionKey("2025-wbl-asia")).thenReturn(Optional.of(
                InternationalResult.builder().id("result-2").competitionKey("2025-wbl-asia").build()));
        assertThatThrownBy(() -> service.update("result-1", validRequest(InternationalResultStatus.FINAL)))
                .isInstanceOf(AdminApiException.class)
                .satisfies(error -> assertThat(((AdminApiException) error).status().value()).isEqualTo(409));
    }

    private static InternationalResultRequest validRequest(InternationalResultStatus status) {
        return new InternationalResultRequest(
                "2025-wbl-asia", "WBL Asia", "2025", InternationalResultCategory.CLUB, status,
                ParticipationStatus.CONFIRMED, LocalDate.of(2025, 9, 23), LocalDate.of(2025, 9, 28),
                "중국 둥관", "BNK 썸", status == InternationalResultStatus.FINAL ? " 3위 " : null, " ",
                status == InternationalResultStatus.FINAL ? 4 : 3, "25:30", 18.5, 2.5, 2.0, 1.8,
                45.6, 42.5, 83.3, "득점 4위", status == InternationalResultStatus.IN_PROGRESS
                        ? LocalDate.of(2025, 9, 25) : null,
                List.of(new InternationalResultSourceRequest(
                        " FIBA 기록 ", "https://www.fiba.basketball/event", InternationalResultSourceType.OFFICIAL)),
                true, 10);
    }

    private static InternationalResultRequest copy(
            InternationalResultRequest request,
            InternationalResultStatus status,
            String teamResult,
            Integer games,
            LocalDate start,
            LocalDate end) {
        return new InternationalResultRequest(
                request.competitionKey(), request.competitionName(), request.editionLabel(), request.category(), status,
                request.participationStatus(), start, end, request.location(), request.teamName(), teamResult,
                request.teamRecord(), games, request.minutesPerGame(), request.pointsPerGame(),
                request.reboundsPerGame(), request.assistsPerGame(), request.stealsPerGame(),
                request.fieldGoalPercent(), request.threePointPercent(), request.freeThrowPercent(),
                request.highlight(), null, request.sources(), request.published(), request.displayOrder());
    }
}
