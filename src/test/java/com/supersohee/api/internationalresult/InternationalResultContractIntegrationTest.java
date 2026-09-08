package com.supersohee.api.internationalresult;

import com.supersohee.api.internationalresult.domain.InternationalResult;
import com.supersohee.api.internationalresult.domain.InternationalResultCategory;
import com.supersohee.api.internationalresult.domain.InternationalResultSource;
import com.supersohee.api.internationalresult.domain.InternationalResultSourceType;
import com.supersohee.api.internationalresult.domain.InternationalResultStatus;
import com.supersohee.api.internationalresult.domain.ParticipationStatus;
import com.supersohee.api.internationalresult.service.InternationalResultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternationalResultContractIntegrationTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean InternationalResultService service;

    @Test
    void publicListIsAccessibleAndExposesSourceAndStatusContract() throws Exception {
        InternationalResult result = InternationalResult.builder()
                .id("wbl-1").competitionKey("2025-wbl-asia").competitionName("WBL Asia").editionLabel("2025")
                .category(InternationalResultCategory.CLUB).status(InternationalResultStatus.FINAL)
                .participationStatus(ParticipationStatus.CONFIRMED)
                .startDate(LocalDate.of(2025, 9, 23)).endDate(LocalDate.of(2025, 9, 28))
                .location("중국 둥관").teamName("BNK 썸").teamResult("3위").gamesPlayed(4)
                .pointsPerGame(18.5).reboundsPerGame(2.5).assistsPerGame(2.0).stealsPerGame(1.8)
                .sources(List.of(InternationalResultSource.builder().label("FIBA")
                        .url("https://www.fiba.basketball/event").type(InternationalResultSourceType.OFFICIAL).build()))
                .published(true).displayOrder(10).build();
        when(service.findPublished()).thenReturn(List.of(result));

        mockMvc.perform(get("/api/international-results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].competitionKey").value("2025-wbl-asia"))
                .andExpect(jsonPath("$[0].category").value("CLUB"))
                .andExpect(jsonPath("$[0].status").value("FINAL"))
                .andExpect(jsonPath("$[0].pointsPerGame").value(18.5))
                .andExpect(jsonPath("$[0].reboundsPerGame").value(2.5))
                .andExpect(jsonPath("$[0].assistsPerGame").value(2.0))
                .andExpect(jsonPath("$[0].stealsPerGame").value(1.8))
                .andExpect(jsonPath("$[0].ppg").doesNotExist())
                .andExpect(jsonPath("$[0].sources[0].type").value("OFFICIAL"));
    }

    @Test
    void adminListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/international-results"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
    }
}
