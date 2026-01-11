package com.supersohee.api.stadium.service;

import com.supersohee.api.stadium.domain.Stadium;
import com.supersohee.api.stadium.domain.StadiumSeat;
import com.supersohee.api.stadium.dto.*;
import com.supersohee.api.stadium.repository.StadiumRepository;
import com.supersohee.api.stadium.repository.StadiumSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final StadiumSeatRepository stadiumSeatRepository;

    // 모든 경기장 목록 조회
    public List<Stadium> findAll() {
        return stadiumRepository.findAllByOrderByNameAsc();
    }

    // 특정 경기장 조회
    public Optional<Stadium> findById(String stadiumId) {
        return stadiumRepository.findById(stadiumId);
    }

    // 이름으로 경기장 조회
    public Optional<Stadium> findByName(String name) {
        return stadiumRepository.findByName(name);
    }

    // 경기장별 좌석 목록 조회 (기존 - 평면 구조)
    public List<StadiumSeat> findSeatsByStadiumId(String stadiumId) {
        return stadiumSeatRepository.findByStadiumIdOrderBySeatTypeAscZoneNameAscBlockNameAscRowAscNumberAsc(stadiumId);
    }

    // 경기장별 좌석 계층 구조 조회 (신규)
    public SeatHierarchyResponse findSeatHierarchyByStadiumId(String stadiumId) {
        List<StadiumSeat> seats = stadiumSeatRepository
                .findByStadiumIdOrderBySeatTypeAscZoneNameAscBlockNameAscRowAscNumberAsc(stadiumId);

        // 구역별로 그룹화 (순서 보장)
        Map<String, List<StadiumSeat>> seatsByZone = seats.stream()
                .collect(Collectors.groupingBy(
                        StadiumSeat::getZoneName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<ZoneInfo> zones = new ArrayList<>();

        for (Map.Entry<String, List<StadiumSeat>> zoneEntry : seatsByZone.entrySet()) {
            String zoneName = zoneEntry.getKey();
            List<StadiumSeat> zoneSeats = zoneEntry.getValue();

            // 첫 번째 좌석에서 구역 공통 정보 가져오기
            StadiumSeat firstSeat = zoneSeats.get(0);
            String seatType = firstSeat.getSeatType();
            String floor = firstSeat.getFloor();

            // 블럭별로 그룹화
            Map<String, List<StadiumSeat>> seatsByBlock = zoneSeats.stream()
                    .collect(Collectors.groupingBy(
                            seat -> seat.getBlockName() != null ? seat.getBlockName() : "NO_BLOCK",
                            LinkedHashMap::new,
                            Collectors.toList()));

            List<BlockInfo> blocks = new ArrayList<>();
            List<RowInfo> directRows = new ArrayList<>(); // 블럭이 없는 경우

            for (Map.Entry<String, List<StadiumSeat>> blockEntry : seatsByBlock.entrySet()) {
                String blockName = blockEntry.getKey();
                List<StadiumSeat> blockSeats = blockEntry.getValue();

                if ("NO_BLOCK".equals(blockName)) {
                    // 블럭이 없는 경우 - 직접 rows 생성
                    directRows = buildRows(blockSeats);
                } else {
                    // 블럭이 있는 경우
                    List<RowInfo> rows = buildRows(blockSeats);
                    blocks.add(BlockInfo.builder()
                            .blockName(blockName)
                            .rows(rows)
                            .build());
                }
            }

            ZoneInfo zoneInfo = ZoneInfo.builder()
                    .zoneName(zoneName)
                    .seatType(seatType)
                    .floor(floor)
                    .blocks(blocks.isEmpty() ? null : blocks)
                    .rows(directRows.isEmpty() ? null : directRows)
                    .build();

            zones.add(zoneInfo);
        }

        // zones 정렬: 층 정보 → 좌석 타입 → 구역명 순서 (층 번호 순서: null/없음/1층 → 2층 → 3층)
        zones.sort((a, b) -> {
            // 1순위: 층 정보 (층 번호를 숫자로 파싱하여 비교)
            // floor 필드 또는 zoneName에서 층 번호 추출
            String floorA = a.getFloor() != null ? a.getFloor() : "";
            String floorB = b.getFloor() != null ? b.getFloor() : "";
            String zoneNameA = a.getZoneName() != null ? a.getZoneName() : "";
            String zoneNameB = b.getZoneName() != null ? b.getZoneName() : "";

            // 층 번호 추출 (floor 필드 또는 zoneName에서 "숫자+층" 패턴만 인식)
            int floorNumA = 0;
            int floorNumB = 0;

            // A의 층 번호 추출
            if (!floorA.isEmpty()) {
                // "2층", "3층" 같은 패턴 찾기
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)층");
                java.util.regex.Matcher matcher = pattern.matcher(floorA);
                if (matcher.find()) {
                    floorNumA = Integer.parseInt(matcher.group(1));
                }
            }
            if (floorNumA == 0 && !zoneNameA.isEmpty()) {
                // zoneName에서 "숫자+층" 패턴 찾기
                String combined = floorA + " " + zoneNameA;
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)층");
                java.util.regex.Matcher matcher = pattern.matcher(combined);
                if (matcher.find()) {
                    floorNumA = Integer.parseInt(matcher.group(1));
                }
            }

            // B의 층 번호 추출
            if (!floorB.isEmpty()) {
                // "2층", "3층" 같은 패턴 찾기
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)층");
                java.util.regex.Matcher matcher = pattern.matcher(floorB);
                if (matcher.find()) {
                    floorNumB = Integer.parseInt(matcher.group(1));
                }
            }
            if (floorNumB == 0 && !zoneNameB.isEmpty()) {
                // zoneName에서 "숫자+층" 패턴 찾기
                String combined = floorB + " " + zoneNameB;
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)층");
                java.util.regex.Matcher matcher = pattern.matcher(combined);
                if (matcher.find()) {
                    floorNumB = Integer.parseInt(matcher.group(1));
                }
            }

            // 층 번호로 비교 (0 → 1 → 2 → 3 순서)
            if (floorNumA != floorNumB) {
                return Integer.compare(floorNumA, floorNumB);
            }

            // 같은 층인 경우
            // 2순위: 좌석 타입
            String seatTypeA = a.getSeatType() != null ? a.getSeatType() : "";
            String seatTypeB = b.getSeatType() != null ? b.getSeatType() : "";
            int typeCompare = seatTypeA.compareTo(seatTypeB);
            if (typeCompare != 0) {
                return typeCompare;
            }

            // 3순위: 구역명
            return zoneNameA.compareTo(zoneNameB);
        });

        return SeatHierarchyResponse.builder()
                .zones(zones)
                .build();
    }

    // 좌석 리스트를 RowInfo 리스트로 변환
    private List<RowInfo> buildRows(List<StadiumSeat> seats) {
        // 열별로 그룹화
        Map<String, List<StadiumSeat>> seatsByRow = seats.stream()
                .collect(Collectors.groupingBy(
                        StadiumSeat::getRow,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<RowInfo> rows = new ArrayList<>();

        for (Map.Entry<String, List<StadiumSeat>> rowEntry : seatsByRow.entrySet()) {
            String row = rowEntry.getKey();
            List<String> numbers = rowEntry.getValue().stream()
                    .map(StadiumSeat::getNumber)
                    .sorted((a, b) -> {
                        // 숫자 부분만 추출하여 정렬
                        int numA = extractNumber(a);
                        int numB = extractNumber(b);
                        return Integer.compare(numA, numB);
                    })
                    .collect(Collectors.toList());

            rows.add(RowInfo.builder()
                    .row(row)
                    .numbers(numbers)
                    .build());
        }

        return rows;
    }

    // 번호 문자열에서 숫자 추출 (예: "1번" -> 1, "15번" -> 15)
    private int extractNumber(String numberStr) {
        try {
            return Integer.parseInt(numberStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 구역, 블럭, 열, 번호로 좌석 ID 찾기
    public Optional<String> findSeatId(String stadiumId, String zoneName, String blockName, String row, String number) {
        List<StadiumSeat> seats = stadiumSeatRepository
                .findByStadiumIdOrderBySeatTypeAscZoneNameAscBlockNameAscRowAscNumberAsc(stadiumId);

        return seats.stream()
                .filter(seat -> seat.getZoneName().equals(zoneName))
                .filter(seat -> {
                    if (blockName == null || blockName.isEmpty()) {
                        return seat.getBlockName() == null;
                    }
                    return blockName.equals(seat.getBlockName());
                })
                .filter(seat -> seat.getRow().equals(row))
                .filter(seat -> seat.getNumber().equals(number))
                .map(StadiumSeat::getId)
                .findFirst();
    }
}
