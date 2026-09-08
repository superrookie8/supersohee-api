package com.supersohee.api.schedule.controller;

import com.supersohee.api.schedule.dto.AdminScheduleRequest;
import com.supersohee.api.schedule.dto.AdminScheduleResponse;
import com.supersohee.api.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<AdminScheduleResponse> getSchedules(@RequestParam(required = false) String season,
            @RequestParam(required = false) String competitionKey,
            @RequestParam(required = false) String editionLabel,
            @RequestParam(required = false) String teamType) {
        var schedules = scheduleService.findAdminSchedules(season);
        if (competitionKey != null || editionLabel != null || teamType != null || season != null) {
            schedules = scheduleService.filterSchedules(schedules, season, competitionKey, editionLabel, teamType);
        }
        return schedules.stream()
                .map(AdminScheduleResponse::from)
                .toList();
    }

    @GetMapping("/seasons")
    public List<String> getSeasons() {
        return scheduleService.findAdminSeasons();
    }

    @PostMapping
    public ResponseEntity<AdminScheduleResponse> createSchedule(
            @Valid @RequestBody AdminScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AdminScheduleResponse.from(scheduleService.createAdminSchedule(request)));
    }

    @PutMapping("/{id}")
    public AdminScheduleResponse updateSchedule(
            @PathVariable String id,
            @Valid @RequestBody AdminScheduleRequest request) {
        return AdminScheduleResponse.from(scheduleService.updateAdminSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable String id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
