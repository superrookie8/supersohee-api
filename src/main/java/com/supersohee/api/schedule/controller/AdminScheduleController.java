package com.supersohee.api.schedule.controller;

import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.dto.ScheduleResponse;
import com.supersohee.api.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final ScheduleService scheduleService;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    // date와 time을 조합하여 한국 시간대 LocalDateTime으로 변환
    private LocalDateTime parseDateAndTime(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 날짜 파싱 (YYYY-MM-DD 형식)
            LocalDate date = LocalDate.parse(dateStr);

            // 시간 파싱 (HH:mm 형식)
            LocalTime time = timeStr != null && !timeStr.trim().isEmpty()
                    ? LocalTime.parse(timeStr)
                    : LocalTime.of(0, 0);

            // 한국 시간대로 조합 (LocalDateTime은 시간대 정보가 없으므로, 한국 시간으로 간주)
            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            throw new IllegalArgumentException("날짜 또는 시간 형식이 올바르지 않습니다. date: " + dateStr + ", time: " + timeStr);
        }
    }

    // 시간대 정보가 포함된 ISO 8601 문자열을 한국 시간대 LocalDateTime으로 변환
    private LocalDateTime parseToKoreaTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // ISO 8601 형식 파싱 (시간대 정보 포함 가능)
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
            // 한국 시간대로 변환 후 LocalDateTime으로 변환
            return zonedDateTime.withZoneSameInstant(KOREA_ZONE).toLocalDateTime();
        } catch (Exception e) {
            // 시간대 정보가 없는 경우 직접 파싱 (이미 한국 시간으로 간주)
            try {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다: " + dateTimeStr);
            }
        }
    }

    // 스케줄 생성
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSchedule(
            @AuthenticationPrincipal String userId,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            // 기존 방식 지원 (startDateTime이 있으면 사용)
            @RequestParam(value = "startDateTime", required = false) String startDateTimeStr,
            // 새로운 방식 지원 (date와 time 분리)
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "time", required = false) String time,
            @RequestParam(value = "endDateTime", required = false) String endDateTimeStr,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "color", required = false) String color,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
            @RequestParam(value = "stadiumId", required = false) String stadiumId,
            @RequestParam(value = "gameId", required = false) String gameId) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(new HashMap<>(error));
        }

        try {
            // 날짜/시간 파싱: date와 time이 있으면 우선 사용, 없으면 startDateTime 사용
            LocalDateTime startDateTime;
            if (date != null && !date.trim().isEmpty()) {
                // 새로운 방식: date와 time을 합쳐서 파싱
                startDateTime = parseDateAndTime(date, time);
            } else if (startDateTimeStr != null && !startDateTimeStr.trim().isEmpty()) {
                // 기존 방식: startDateTime 문자열 파싱
                startDateTime = parseToKoreaTime(startDateTimeStr);
            } else {
                throw new IllegalArgumentException("날짜 정보가 필요합니다. date 또는 startDateTime을 제공해주세요.");
            }

            LocalDateTime endDateTime = parseToKoreaTime(endDateTimeStr);

            Schedule schedule = scheduleService.createSchedule(
                    title,
                    description,
                    startDateTime,
                    endDateTime,
                    location,
                    type,
                    color,
                    url,
                    isActive,
                    stadiumId,
                    gameId);

            ScheduleResponse response = ScheduleResponse.from(schedule);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "스케줄이 생성되었습니다");
            result.put("schedule", response);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "스케줄 생성 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 스케줄 수정
    @PutMapping("/{scheduleId}")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @AuthenticationPrincipal String userId,
            @PathVariable String scheduleId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "time", required = false) String time,
            @RequestParam(value = "startDateTime", required = false) String startDateTimeStr,
            @RequestParam(value = "endDateTime", required = false) String endDateTimeStr,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "color", required = false) String color,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "stadiumId", required = false) String stadiumId,
            @RequestParam(value = "gameId", required = false) String gameId) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(new HashMap<>(error));
        }

        try {
            // date와 time이 제공된 경우 이를 우선 사용, 없으면 startDateTime 사용
            LocalDateTime startDateTime = null;
            if (date != null && !date.trim().isEmpty()) {
                // date와 time을 조합하여 한국 시간대로 변환
                startDateTime = parseDateAndTime(date, time);
            } else if (startDateTimeStr != null && !startDateTimeStr.trim().isEmpty()) {
                // 기존 방식: startDateTime 문자열 파싱
                startDateTime = parseToKoreaTime(startDateTimeStr);
            }

            LocalDateTime endDateTime = null;
            if (endDateTimeStr != null && !endDateTimeStr.trim().isEmpty()) {
                endDateTime = parseToKoreaTime(endDateTimeStr);
            }

            Schedule schedule = scheduleService.updateSchedule(
                    scheduleId,
                    title,
                    description,
                    startDateTime,
                    endDateTime,
                    location,
                    type,
                    color,
                    url,
                    isActive,
                    stadiumId,
                    gameId);

            ScheduleResponse response = ScheduleResponse.from(schedule);

            Map<String, Object> result = new HashMap<>();
            result.put("message", "스케줄이 수정되었습니다");
            result.put("schedule", response);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "스케줄 수정 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 스케줄 삭제 (isActive = false)
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Map<String, String>> deleteSchedule(
            @AuthenticationPrincipal String userId,
            @PathVariable String scheduleId) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "어드민 권한이 필요합니다");
            return ResponseEntity.status(403).body(error);
        }

        try {
            scheduleService.deleteSchedule(scheduleId);
            Map<String, String> result = new HashMap<>();
            result.put("message", "스케줄이 삭제되었습니다");
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // 모든 스케줄 조회 (어드민용, 비활성 포함)
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getAllSchedules(
            @AuthenticationPrincipal String userId) {

        // 어드민 권한 체크
        if (!"admin".equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        List<Schedule> schedules = scheduleService.findAllSchedules();
        List<ScheduleResponse> responses = schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
