package com.supersohee.api.diary.dto;

import com.supersohee.api.stadium.domain.StadiumSeat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수정 페이지에서 좌석 UI를 복원할 때 사용 (zone/block/row/number + stadiumId).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiarySeatInfoResponse {

    private String seatId;
    private String stadiumId;
    private String zoneName;
    private String blockName;
    private String row;
    private String number;
    private String seatType;
    private String floor;

    public static DiarySeatInfoResponse from(StadiumSeat seat) {
        return DiarySeatInfoResponse.builder()
                .seatId(seat.getId())
                .stadiumId(seat.getStadiumId())
                .zoneName(seat.getZoneName())
                .blockName(seat.getBlockName())
                .row(seat.getRow())
                .number(seat.getNumber())
                .seatType(seat.getSeatType())
                .floor(seat.getFloor())
                .build();
    }
}
