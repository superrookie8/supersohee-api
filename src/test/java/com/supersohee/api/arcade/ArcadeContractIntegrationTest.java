package com.supersohee.api.arcade;

import com.supersohee.api.arcade.dto.RankingResponse;
import com.supersohee.api.arcade.dto.ScoreResponse;
import com.supersohee.api.arcade.error.ArcadeApiException;
import com.supersohee.api.arcade.service.ArcadeService;
import com.supersohee.api.config.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArcadeContractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean ArcadeService arcadeService;

    @Test
    void rankingIsPublicAndUsesNullPrincipalForAnonymousRequests() throws Exception {
        when(arcadeService.getRanking(10, null)).thenReturn(ranking(null));

        mockMvc.perform(get("/api/arcade/ranking").queryParam("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rankings").isArray())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.myRank").doesNotExist());

        verify(arcadeService).getRanking(10, null);
    }

    @Test
    void rankingReceivesTheStringUserIdPrincipalAndAllowsAdminToo() throws Exception {
        when(arcadeService.getRanking(10, "user-1")).thenReturn(ranking(2));
        when(arcadeService.getRanking(10, "admin")).thenReturn(ranking(null));

        mockMvc.perform(get("/api/arcade/ranking").queryParam("limit", "10")
                        .header(HttpHeaders.AUTHORIZATION, userBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myRank").value(2));
        mockMvc.perform(get("/api/arcade/ranking").queryParam("limit", "10")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk());

        verify(arcadeService).getRanking(10, "user-1");
        verify(arcadeService).getRanking(10, "admin");
    }

    @Test
    void scoreSubmitAndMyScoreReceiveTheStringUserIdPrincipal() throws Exception {
        ScoreResponse score = score(120, 1);
        when(arcadeService.submitScore("user-1", 120)).thenReturn(score);
        when(arcadeService.getMyScore("user-1")).thenReturn(score);

        mockMvc.perform(post("/api/arcade/score")
                        .header(HttpHeaders.AUTHORIZATION, userBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":120}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.bestScore").value(120));
        mockMvc.perform(get("/api/arcade/my-score")
                        .header(HttpHeaders.AUTHORIZATION, userBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(1));

        verify(arcadeService).submitScore("user-1", 120);
        verify(arcadeService).getMyScore("user-1");
    }

    @Test
    void scoreRoutesRejectAnonymousAndAdminTokens() throws Exception {
        mockMvc.perform(post("/api/arcade/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":120}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ARCADE_AUTHENTICATION_REQUIRED"));
        mockMvc.perform(get("/api/arcade/my-score")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ARCADE_ACCESS_DENIED"));
    }

    @Test
    void invalidScoreLimitAndMissingUserUseSafeErrors() throws Exception {
        mockMvc.perform(post("/api/arcade/score")
                        .header(HttpHeaders.AUTHORIZATION, userBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARCADE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.score").exists())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mockMvc.perform(get("/api/arcade/ranking").queryParam("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARCADE_INVALID_REQUEST"));

        when(arcadeService.getMyScore("user-1")).thenThrow(ArcadeApiException.userNotFound());
        mockMvc.perform(get("/api/arcade/my-score")
                        .header(HttpHeaders.AUTHORIZATION, userBearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARCADE_USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User was not found."));
    }

    private RankingResponse ranking(Integer myRank) {
        return RankingResponse.builder().rankings(List.of()).totalCount(0).myRank(myRank).build();
    }

    private ScoreResponse score(int bestScore, int rank) {
        return ScoreResponse.builder()
                .userId("user-1")
                .nickname("팬")
                .bestScore(bestScore)
                .rank(rank)
                .build();
    }

    private String userBearer() {
        return "Bearer " + jwtUtil.generateUserToken("user-1");
    }

    private String adminBearer() {
        return "Bearer " + jwtUtil.generateAdminToken("admin");
    }
}
