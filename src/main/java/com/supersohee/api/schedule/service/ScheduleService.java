package com.supersohee.api.schedule.service;

import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.dto.ScheduleDetailsResponse;
import com.supersohee.api.schedule.repository.ScheduleRepository;
import com.supersohee.api.stadium.domain.Stadium;
import com.supersohee.api.stadium.service.StadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final StadiumService stadiumService;

    // 공개: 활성 스케줄 조회 (날짜 범위)
    public List<Schedule> findActiveSchedules(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return scheduleRepository.findByIsActiveTrueAndStartDateTimeBetweenOrderByStartDateTimeAsc(start, end);
        }
        return scheduleRepository.findByIsActiveTrueOrderByStartDateTimeAsc();
    }

    // 공개: 특정 스케줄 조회
    public Optional<Schedule> findById(String id) {
        return scheduleRepository.findById(id)
                .filter(Schedule::getIsActive);
    }

    // 공개: 스케줄 상세 조회 (경기장 정보 포함)
    public Optional<ScheduleDetailsResponse> findDetailsById(String id) {
        return scheduleRepository.findById(id)
                .filter(Schedule::getIsActive)
                .map(schedule -> {
                    Stadium stadium = null;
                    String gameId = schedule.getGameId();

                    // 1. stadiumId가 있으면 우선 사용
                    if (schedule.getStadiumId() != null) {
                        stadium = stadiumService.findById(schedule.getStadiumId())
                                .orElse(null);
                    }

                    // 2. stadiumId가 없으면 location 기반으로 자동 매핑
                    if (stadium == null) {
                        stadium = findStadiumByLocation(schedule.getLocation());
                    }

                    return ScheduleDetailsResponse.from(schedule, stadium, gameId);
                });
    }

    // location 기반으로 경기장 찾기
    private Stadium findStadiumByLocation(String location) {
        if (location == null) {
            return null;
        }

        // "Home"이면 부산 사직실내체육관 (부산 BNK 썸의 홈 경기장)
        if ("Home".equals(location)) {
            return stadiumService.findByName("부산 사직실내체육관").orElse(null);
        }

        // location에 팀 이름이 포함되어 있으면 팀의 홈 경기장 매핑
        String locationLower = location.toLowerCase();

        // 부산 BNK 썸
        if (locationLower.contains("bnk") || locationLower.contains("썸")) {
            return stadiumService.findByName("부산 사직실내체육관").orElse(null);
        }

        // 용인 삼성생명
        if (locationLower.contains("삼성생명") || locationLower.contains("삼성")) {
            return stadiumService.findByName("용인실내체육관").orElse(null);
        }

        // 아산 우리은행
        if (locationLower.contains("우리은행") || locationLower.contains("우리")) {
            return stadiumService.findByName("아산 이순신체육관").orElse(null);
        }

        // 인천 신한은행
        if (locationLower.contains("신한은행") || locationLower.contains("신한")) {
            return stadiumService.findByName("인천 도원체육관").orElse(null);
        }

        // 부천 하나원큐 / 하나은행
        if (locationLower.contains("하나은행") || locationLower.contains("하나원큐") || locationLower.contains("하나")) {
            return stadiumService.findByName("부천체육관").orElse(null);
        }

        // 청주 KB스타즈
        if (locationLower.contains("kb스타즈") || locationLower.contains("kb")) {
            return stadiumService.findByName("청주체육관").orElse(null);
        }

        return null;
    }

    // 어드민용: 모든 스케줄 조회
    public List<Schedule> findAllSchedules() {
        return scheduleRepository.findAllByOrderByStartDateTimeDesc();
    }

    // 어드민용: 스케줄 생성
    @Transactional
    public Schedule createSchedule(
            String title,
            String description,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String location,
            String type,
            String color,
            String url,
            Boolean isActive,
            String stadiumId,
            String gameId) {

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("스케줄 제목은 필수입니다");
        }

        if (startDateTime == null) {
            throw new IllegalArgumentException("시작 일시는 필수입니다");
        }

        // endDateTime이 없으면 startDateTime + 2시간으로 자동 설정
        if (endDateTime == null) {
            endDateTime = startDateTime.plusHours(2);
        }

        // 기본 색상 설정
        if (color == null || color.trim().isEmpty()) {
            color = "#3B82F6"; // 기본 파란색
        }

        // 기본 타입 설정
        if (type == null || type.trim().isEmpty()) {
            type = "other";
        }

        Schedule schedule = Schedule.builder()
                .title(title)
                .description(description)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .location(location)
                .type(type)
                .color(color)
                .url(url)
                .stadiumId(stadiumId)
                .gameId(gameId)
                .isActive(isActive != null ? isActive : true)
                .build();

        return scheduleRepository.save(schedule);
    }

    // 어드민용: 스케줄 수정
    @Transactional
    public Schedule updateSchedule(
            String scheduleId,
            String title,
            String description,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String location,
            String type,
            String color,
            String url,
            Boolean isActive,
            String stadiumId,
            String gameId) {

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("스케줄을 찾을 수 없습니다"));

        // 최종적으로 사용할 startDateTime 결정
        LocalDateTime finalStartDateTime = startDateTime != null ? startDateTime : schedule.getStartDateTime();

        // endDateTime이 null이면 finalStartDateTime + 2시간으로 자동 설정
        LocalDateTime finalEndDateTime = endDateTime;
        if (finalEndDateTime == null) {
            finalEndDateTime = finalStartDateTime.plusHours(2);
        }

        Schedule updatedSchedule = Schedule.builder()
                .id(schedule.getId())
                .title(title != null ? title : schedule.getTitle())
                .description(description != null ? description : schedule.getDescription())
                .startDateTime(finalStartDateTime)
                .endDateTime(finalEndDateTime)
                .location(location != null ? location : schedule.getLocation())
                .type(type != null ? type : schedule.getType())
                .color(color != null ? color : schedule.getColor())
                .url(url != null ? url : schedule.getUrl())
                .stadiumId(stadiumId != null ? stadiumId : schedule.getStadiumId())
                .gameId(gameId != null ? gameId : schedule.getGameId())
                .isActive(isActive != null ? isActive : schedule.getIsActive())
                .build();

        updatedSchedule.setCreatedAt(schedule.getCreatedAt());

        return scheduleRepository.save(updatedSchedule);
    }

    // 어드민용: 스케줄 삭제 (isActive = false)
    @Transactional
    public void deleteSchedule(String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("스케줄을 찾을 수 없습니다"));

        Schedule deletedSchedule = Schedule.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .description(schedule.getDescription())
                .startDateTime(schedule.getStartDateTime())
                .endDateTime(schedule.getEndDateTime())
                .location(schedule.getLocation())
                .type(schedule.getType())
                .color(schedule.getColor())
                .url(schedule.getUrl())
                .stadiumId(schedule.getStadiumId())
                .gameId(schedule.getGameId())
                .isActive(false)
                .build();

        deletedSchedule.setCreatedAt(schedule.getCreatedAt());
        scheduleRepository.save(deletedSchedule);
    }
}
