package com.supersohee.api.diary.service;

import com.supersohee.api.diary.domain.Diary;
import com.supersohee.api.diary.dto.DiaryRequest;
import com.supersohee.api.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;

    // 직관일지 작성
    @Transactional
    public Diary createDiary(String userId, DiaryRequest request) {
        // gameId가 있으면 같은 경기에 대한 일지가 이미 있는지 확인
        if (request.getGameId() != null && !request.getGameId().isEmpty()) {
            Optional<Diary> existingDiary = diaryRepository.findByUserIdAndGameId(userId, request.getGameId());
            if (existingDiary.isPresent()) {
                throw new RuntimeException("이미 해당 경기에 대한 일지가 존재합니다");
            }
        }

        // PlayerStats 변환
        List<Diary.PlayerStatRecord> playerStats = null;
        if (request.getPlayerStats() != null) {
            playerStats = request.getPlayerStats().stream()
                    .map(dto -> Diary.PlayerStatRecord.builder()
                            .playerName(dto.getPlayerName())
                            .team(dto.getTeam())
                            .stats(dto.getStats() != null ? Diary.PlayerStats.builder()
                                    .pts(dto.getStats().getPts())
                                    .fg2Made(dto.getStats().getFg2Made())
                                    .fg2Att(dto.getStats().getFg2Att())
                                    .fg3Made(dto.getStats().getFg3Made())
                                    .fg3Att(dto.getStats().getFg3Att())
                                    .rebOff(dto.getStats().getRebOff())
                                    .rebDef(dto.getStats().getRebDef())
                                    .ast(dto.getStats().getAst())
                                    .stl(dto.getStats().getStl())
                                    .blk(dto.getStats().getBlk())
                                    .to(dto.getStats().getTo())
                                    .build() : null)
                            .build())
                    .collect(Collectors.toList());
        }

        // Highlights 변환
        Diary.DiaryHighlights highlights = null;
        if (request.getHighlights() != null) {
            highlights = Diary.DiaryHighlights.builder()
                    .overtime(request.getHighlights().getOvertime())
                    .injury(request.getHighlights().getInjury())
                    .referee(request.getHighlights().getReferee())
                    .bestMood(request.getHighlights().getBestMood())
                    .worstMood(request.getHighlights().getWorstMood())
                    .custom(request.getHighlights().getCustom())
                    .build();
        }

        Diary diary = Diary.builder()
                .userId(userId)
                .gameId(request.getGameId())
                .date(request.getDate())
                .time(request.getTime())
                .location(request.getLocation())
                .watchType(request.getWatchType())
                .mvpPlayerName(request.getMvpPlayerName())
                .mvpReason(request.getMvpReason())
                .playerStats(playerStats)
                .highlights(highlights)
                .gameResult(request.getGameResult())
                .gameHomeScore(request.getGameHomeScore())
                .gameAwayScore(request.getGameAwayScore())
                .gameWinner(request.getGameWinner())
                .companions(request.getCompanions())
                .companion(request.getCompanion())
                .seat(request.getSeat())
                .seatId(request.getSeatId())
                .photoUrls(request.getPhotoUrls())
                .memo(request.getMemo())
                .content(request.getContent())
                .cheeredPlayerName(request.getCheeredPlayerName())
                .cheeredPlayerPoints(request.getCheeredPlayerPoints())
                .cheeredPlayerAssists(request.getCheeredPlayerAssists())
                .cheeredPlayerRebounds(request.getCheeredPlayerRebounds())
                .cheeredPlayerTwoPointMade(request.getCheeredPlayerTwoPointMade())
                .cheeredPlayerTwoPointPercent(request.getCheeredPlayerTwoPointPercent())
                .cheeredPlayerThreePointMade(request.getCheeredPlayerThreePointMade())
                .cheeredPlayerThreePointPercent(request.getCheeredPlayerThreePointPercent())
                .cheeredPlayerFreeThrowMade(request.getCheeredPlayerFreeThrowMade())
                .cheeredPlayerFreeThrowPercent(request.getCheeredPlayerFreeThrowPercent())
                .cheeredPlayerFouls(request.getCheeredPlayerFouls())
                .cheeredPlayerBlocks(request.getCheeredPlayerBlocks())
                .cheeredPlayerTurnovers(request.getCheeredPlayerTurnovers())
                .cheeredPlayerMemo(request.getCheeredPlayerMemo())
                .build();

        return diaryRepository.save(diary);
    }

    // 내가 쓴 직관일지 목록 조회
    public List<Diary> findMyDiaries(String userId) {
        return diaryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 특정 직관일지 조회
    public Optional<Diary> findById(String diaryId) {
        return diaryRepository.findById(diaryId);
    }

    // 특정 경기의 내 일지 조회
    public Optional<Diary> findByUserIdAndGameId(String userId, String gameId) {
        return diaryRepository.findByUserIdAndGameId(userId, gameId);
    }

    // 직관일지 수정 (부분 업데이트 지원)
    @Transactional
    public Diary updateDiary(String userId, String diaryId, DiaryRequest request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일지를 찾을 수 없습니다"));

        // 본인 일지인지 확인
        if (!diary.getUserId().equals(userId)) {
            throw new RuntimeException("본인의 일지만 수정할 수 있습니다");
        }

        // 부분 업데이트: request에 있는 필드만 업데이트
        Diary.DiaryBuilder builder = diary.toBuilder();

        // 경기 정보 (gameId는 변경 불가)
        if (request.getDate() != null) builder.date(request.getDate());
        if (request.getTime() != null) builder.time(request.getTime());
        if (request.getLocation() != null) builder.location(request.getLocation());
        if (request.getWatchType() != null) builder.watchType(request.getWatchType());

        // MVP
        if (request.getMvpPlayerName() != null) builder.mvpPlayerName(request.getMvpPlayerName());
        if (request.getMvpReason() != null) builder.mvpReason(request.getMvpReason());

        // PlayerStats (null이면 업데이트 안 함, 빈 배열이면 빈 배열로 설정)
        if (request.getPlayerStats() != null) {
            List<Diary.PlayerStatRecord> playerStats = request.getPlayerStats().stream()
                    .map(dto -> Diary.PlayerStatRecord.builder()
                            .playerName(dto.getPlayerName())
                            .team(dto.getTeam())
                            .stats(dto.getStats() != null ? Diary.PlayerStats.builder()
                                    .pts(dto.getStats().getPts())
                                    .fg2Made(dto.getStats().getFg2Made())
                                    .fg2Att(dto.getStats().getFg2Att())
                                    .fg3Made(dto.getStats().getFg3Made())
                                    .fg3Att(dto.getStats().getFg3Att())
                                    .rebOff(dto.getStats().getRebOff())
                                    .rebDef(dto.getStats().getRebDef())
                                    .ast(dto.getStats().getAst())
                                    .stl(dto.getStats().getStl())
                                    .blk(dto.getStats().getBlk())
                                    .to(dto.getStats().getTo())
                                    .build() : null)
                            .build())
                    .collect(Collectors.toList());
            builder.playerStats(playerStats);
        }

        // Highlights (null이면 업데이트 안 함)
        if (request.getHighlights() != null) {
            Diary.DiaryHighlights highlights = Diary.DiaryHighlights.builder()
                    .overtime(request.getHighlights().getOvertime())
                    .injury(request.getHighlights().getInjury())
                    .referee(request.getHighlights().getReferee())
                    .bestMood(request.getHighlights().getBestMood())
                    .worstMood(request.getHighlights().getWorstMood())
                    .custom(request.getHighlights().getCustom())
                    .build();
            builder.highlights(highlights);
        }

        // 경기 결과
        if (request.getGameResult() != null) builder.gameResult(request.getGameResult());
        if (request.getGameHomeScore() != null) builder.gameHomeScore(request.getGameHomeScore());
        if (request.getGameAwayScore() != null) builder.gameAwayScore(request.getGameAwayScore());
        if (request.getGameWinner() != null) builder.gameWinner(request.getGameWinner());

        // 동행자 & 좌석
        if (request.getCompanions() != null) builder.companions(request.getCompanions());
        if (request.getCompanion() != null) builder.companion(request.getCompanion());
        if (request.getSeat() != null) builder.seat(request.getSeat());
        if (request.getSeatId() != null) builder.seatId(request.getSeatId());

        // 사진 & 메모
        if (request.getPhotoUrls() != null) builder.photoUrls(request.getPhotoUrls());
        if (request.getMemo() != null) builder.memo(request.getMemo());
        if (request.getContent() != null) builder.content(request.getContent());

        // 하위 호환성 필드
        if (request.getCheeredPlayerName() != null) builder.cheeredPlayerName(request.getCheeredPlayerName());
        if (request.getCheeredPlayerPoints() != null) builder.cheeredPlayerPoints(request.getCheeredPlayerPoints());
        if (request.getCheeredPlayerAssists() != null) builder.cheeredPlayerAssists(request.getCheeredPlayerAssists());
        if (request.getCheeredPlayerRebounds() != null) builder.cheeredPlayerRebounds(request.getCheeredPlayerRebounds());
        if (request.getCheeredPlayerTwoPointMade() != null) builder.cheeredPlayerTwoPointMade(request.getCheeredPlayerTwoPointMade());
        if (request.getCheeredPlayerTwoPointPercent() != null) builder.cheeredPlayerTwoPointPercent(request.getCheeredPlayerTwoPointPercent());
        if (request.getCheeredPlayerThreePointMade() != null) builder.cheeredPlayerThreePointMade(request.getCheeredPlayerThreePointMade());
        if (request.getCheeredPlayerThreePointPercent() != null) builder.cheeredPlayerThreePointPercent(request.getCheeredPlayerThreePointPercent());
        if (request.getCheeredPlayerFreeThrowMade() != null) builder.cheeredPlayerFreeThrowMade(request.getCheeredPlayerFreeThrowMade());
        if (request.getCheeredPlayerFreeThrowPercent() != null) builder.cheeredPlayerFreeThrowPercent(request.getCheeredPlayerFreeThrowPercent());
        if (request.getCheeredPlayerFouls() != null) builder.cheeredPlayerFouls(request.getCheeredPlayerFouls());
        if (request.getCheeredPlayerBlocks() != null) builder.cheeredPlayerBlocks(request.getCheeredPlayerBlocks());
        if (request.getCheeredPlayerTurnovers() != null) builder.cheeredPlayerTurnovers(request.getCheeredPlayerTurnovers());
        if (request.getCheeredPlayerMemo() != null) builder.cheeredPlayerMemo(request.getCheeredPlayerMemo());

        Diary updatedDiary = builder.build();

        // createdAt은 BaseDocument에서 자동 관리되므로 저장 후 기존 값으로 설정
        Diary saved = diaryRepository.save(updatedDiary);
        saved.setCreatedAt(diary.getCreatedAt()); // 생성일시 유지
        return diaryRepository.save(saved);
    }

    // 직관일지 삭제
    @Transactional
    public void deleteDiary(String userId, String diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new RuntimeException("일지를 찾을 수 없습니다"));

        // 본인 일지인지 확인
        if (!diary.getUserId().equals(userId)) {
            throw new RuntimeException("본인의 일지만 삭제할 수 있습니다");
        }

        diaryRepository.delete(diary);
    }
}