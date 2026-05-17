package com.supersohee.api.stadium.repository;

import com.supersohee.api.stadium.domain.StadiumSeat;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StadiumSeatRepository extends MongoRepository<StadiumSeat, String> {
     // 경기장별 좌석 목록 조회 (일지 작성시 드롭다운용)
     // 정렬: 좌석 타입 → 구역명 → 블럭명 → 열 → 번호
     List<StadiumSeat> findByStadiumIdOrderBySeatTypeAscZoneNameAscBlockNameAscRowAscNumberAsc(String stadiumId);
    
     // 좌석 타입별 조회
     List<StadiumSeat> findByStadiumIdAndSeatType(String stadiumId, String seatType);
     
     // 경기장별 좌석 개수 조회
     long countByStadiumId(String stadiumId);

     /** 구역·열·번호로 좌석 1건 조회 (전체 목록 로드 없이 seatId 해석용) */
     Optional<StadiumSeat> findFirstByStadiumIdAndZoneNameAndRowAndNumber(
             String stadiumId, String zoneName, String row, String number);

     Optional<StadiumSeat> findFirstByStadiumIdAndZoneNameAndBlockNameAndRowAndNumber(
             String stadiumId, String zoneName, String blockName, String row, String number);
}
