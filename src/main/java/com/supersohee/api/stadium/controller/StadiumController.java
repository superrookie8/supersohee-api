package com.supersohee.api.stadium.controller;

import com.supersohee.api.stadium.domain.Stadium;
import com.supersohee.api.stadium.domain.StadiumSeat;
import com.supersohee.api.stadium.dto.SeatHierarchyResponse;
import com.supersohee.api.stadium.service.StadiumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stadiums")
@RequiredArgsConstructor
public class StadiumController {

    private final StadiumService stadiumService;

    // 모든 경기장 목록 조회 (공개)
    @GetMapping
    public ResponseEntity<List<Stadium>> getAllStadiums() {
        return ResponseEntity.ok(stadiumService.findAll());
    }

    // 특정 경기장 상세 조회 (공개)
    @GetMapping("/{stadiumId}")
    public ResponseEntity<Stadium> getStadium(@PathVariable String stadiumId) {
        return stadiumService.findById(stadiumId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 경기장별 좌석 목록 조회 (공개) - 평면 구조 (기존)
    @GetMapping("/{stadiumId}/seats")
    public ResponseEntity<List<StadiumSeat>> getStadiumSeats(@PathVariable String stadiumId) {
        List<StadiumSeat> seats = stadiumService.findSeatsByStadiumId(stadiumId);
        return ResponseEntity.ok(seats);
    }

    // 경기장별 좌석 계층 구조 조회 (공개) - 단계별 선택용
    @GetMapping("/{stadiumId}/seats/hierarchy")
    public ResponseEntity<SeatHierarchyResponse> getStadiumSeatHierarchy(@PathVariable String stadiumId) {
        SeatHierarchyResponse hierarchy = stadiumService.findSeatHierarchyByStadiumId(stadiumId);
        return ResponseEntity.ok(hierarchy);
    }

    // 최종 좌석 ID 조회 (공개) - 구역, 블럭, 열, 번호로 좌석 ID 찾기
    @GetMapping("/{stadiumId}/seat")
    public ResponseEntity<String> getSeatId(
            @PathVariable String stadiumId,
            @RequestParam String zoneName,
            @RequestParam(required = false) String blockName,
            @RequestParam String row,
            @RequestParam String number) {
        Optional<String> seatId = stadiumService.findSeatId(stadiumId, zoneName, blockName, row, number);
        return seatId.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
