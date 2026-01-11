package com.supersohee.api.schedule.controller;

import com.supersohee.api.schedule.domain.Schedule;
import com.supersohee.api.schedule.dto.ScheduleDetailsResponse;
import com.supersohee.api.schedule.dto.ScheduleResponse;
import com.supersohee.api.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 스케줄 목록 조회 (공개, 날짜 범위 선택)
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<Schedule> schedules = scheduleService.findActiveSchedules(start, end);
        List<ScheduleResponse> responses = schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    // 특정 스케줄 조회 (공개)
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable String scheduleId) {
        return scheduleService.findById(scheduleId)
                .map(ScheduleResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 스케줄 상세 조회 (모달용 - 경기장 정보, 교통정보, 직관일지 링크 포함)
    @GetMapping("/{scheduleId}/details")
    public ResponseEntity<ScheduleDetailsResponse> getScheduleDetails(@PathVariable String scheduleId) {
        return scheduleService.findDetailsById(scheduleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
