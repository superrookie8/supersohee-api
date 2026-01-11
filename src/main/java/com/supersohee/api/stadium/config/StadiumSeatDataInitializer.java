package com.supersohee.api.stadium.config;

import com.supersohee.api.stadium.domain.Stadium;
import com.supersohee.api.stadium.domain.StadiumSeat;
import com.supersohee.api.stadium.repository.StadiumRepository;
import com.supersohee.api.stadium.repository.StadiumSeatRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StadiumSeatDataInitializer {

    private final StadiumSeatRepository stadiumSeatRepository;
    private final StadiumRepository stadiumRepository;

    @PostConstruct
    public void init() {
        List<StadiumSeat> allSeats = new ArrayList<>();

        // 경기장별로 데이터가 없으면 생성
        Optional<Stadium> asanStadium = findStadiumByName("아산 이순신체육관");
        if (asanStadium.isPresent() && stadiumSeatRepository.countByStadiumId(asanStadium.get().getId()) == 0) {
            allSeats.addAll(initAsanSeats());
        }

        Optional<Stadium> bucheonStadium = findStadiumByName("부천체육관");
        if (bucheonStadium.isPresent() && stadiumSeatRepository.countByStadiumId(bucheonStadium.get().getId()) == 0) {
            allSeats.addAll(initBucheonSeats());
        }

        Optional<Stadium> cheongjuStadium = findStadiumByName("청주체육관");
        if (cheongjuStadium.isPresent() && stadiumSeatRepository.countByStadiumId(cheongjuStadium.get().getId()) == 0) {
            allSeats.addAll(initCheongjuSeats());
        }

        Optional<Stadium> yonginStadium = findStadiumByName("용인실내체육관");
        if (yonginStadium.isPresent() && stadiumSeatRepository.countByStadiumId(yonginStadium.get().getId()) == 0) {
            allSeats.addAll(initYonginSeats());
        }

        Optional<Stadium> incheonStadium = findStadiumByName("인천 도원체육관");
        if (incheonStadium.isPresent() && stadiumSeatRepository.countByStadiumId(incheonStadium.get().getId()) == 0) {
            allSeats.addAll(initIncheonSeats());
        }

        Optional<Stadium> busanStadium = findStadiumByName("부산 사직실내체육관");
        if (busanStadium.isPresent() && stadiumSeatRepository.countByStadiumId(busanStadium.get().getId()) == 0) {
            allSeats.addAll(initBusanSeats());
        }

        // 일괄 저장
        if (!allSeats.isEmpty()) {
            stadiumSeatRepository.saveAll(allSeats);
        }
    }

    // 아산 이순신체육관 좌석 데이터 생성
    private List<StadiumSeat> initAsanSeats() {
        Optional<Stadium> stadium = findStadiumByName("아산 이순신체육관");
        if (stadium.isEmpty()) {
            return new ArrayList<>();
        }

        String stadiumId = stadium.get().getId();
        List<StadiumSeat> seats = new ArrayList<>();

        // 1. 1층 flex석 (4석)
        // flex석은 좌석이 4개뿐이므로 개별로 생성
        for (int i = 1; i <= 4; i++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("1층 flex석")
                    .blockName(null)
                    .row("전체")
                    .number(i + "번")
                    .seatType("flex석")
                    .floor("1층")
                    .description(null)
                    .build());
        }

        // 2. 1층 유니시티 테이블석
        // T블럭: 2열 3행
        for (int row = 1; row <= 2; row++) {
            for (int num = 1; num <= 3; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("1층 유니시티 테이블석")
                        .blockName("T블럭")
                        .row(row + "열")
                        .number(num + "행")
                        .seatType("테이블석")
                        .floor("1층")
                        .description("T블럭(" + row + "열 " + num + "행)")
                        .build());
            }
        }

        // U블럭: 4열 3행
        for (int row = 1; row <= 4; row++) {
            for (int num = 1; num <= 3; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("1층 유니시티 테이블석")
                        .blockName("U블럭")
                        .row(row + "열")
                        .number(num + "행")
                        .seatType("테이블석")
                        .floor("1층")
                        .description("U블럭(" + row + "열 " + num + "행)")
                        .build());
            }
        }

        // V블럭: 2열 3행
        for (int row = 1; row <= 2; row++) {
            for (int num = 1; num <= 3; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("1층 유니시티 테이블석")
                        .blockName("V블럭")
                        .row(row + "열")
                        .number(num + "행")
                        .seatType("테이블석")
                        .floor("1층")
                        .description("V블럭(" + row + "열 " + num + "행)")
                        .build());
            }
        }

        // 3. 1층 스케쳐스 테이블석
        // O블럭, P블럭, Q블럭 - 상세 정보가 없으므로 일단 기본 구조만 생성
        // 실제 좌석 정보가 필요하면 나중에 수정
        String[] sketchBlocks = { "O블럭", "P블럭", "Q블럭" };
        for (String block : sketchBlocks) {
            // 임시로 1-10열, 1-10행으로 생성 (나중에 수정 필요)
            for (int row = 1; row <= 10; row++) {
                for (int num = 1; num <= 10; num++) {
                    seats.add(StadiumSeat.builder()
                            .stadiumId(stadiumId)
                            .zoneName("1층 스케쳐스 테이블석")
                            .blockName(block)
                            .row(row + "열")
                            .number(num + "행")
                            .seatType("테이블석")
                            .floor("1층")
                            .description(null)
                            .build());
                }
            }
        }

        // 4. 1층 우리WON 응원석
        // L블럭: A-H열, 1-19번
        for (char row = 'A'; row <= 'H'; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("1층 우리WON 응원석")
                        .blockName("L블럭")
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("1층")
                        .description(null)
                        .build());
            }
        }

        // M블럭: A-H열, 1-28번
        for (char row = 'A'; row <= 'H'; row++) {
            for (int num = 1; num <= 28; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("1층 우리WON 응원석")
                        .blockName("M블럭")
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("1층")
                        .description(null)
                        .build());
            }
        }

        // N블럭: A-H열, 1-19번
        for (char row = 'A'; row <= 'H'; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("1층 우리WON 응원석")
                        .blockName("N블럭")
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("1층")
                        .description(null)
                        .build());
            }
        }

        // 5. 1층 위비존 R블럭: 1-26번, F-H열만 판매 (A-E열은 판매하지 않음)
        for (char row = 'F'; row <= 'H'; row++) {
            for (int num = 1; num <= 26; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("1층 위비존")
                        .blockName("R블럭")
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("1층")
                        .description("A-E열은 판매하지 않음")
                        .build());
            }
        }

        // 6. 1층 봄봄존 S블럭: 1-26번, F-H열만 판매 (A-E열은 판매하지 않음)
        for (char row = 'F'; row <= 'H'; row++) {
            for (int num = 1; num <= 26; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("1층 봄봄존")
                        .blockName("S블럭")
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("1층")
                        .description("A-E열은 판매하지 않음")
                        .build());
            }
        }

        // 7. 2층 D구역: 1-35번, 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 35; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 D구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 8. 2층 E구역: 1-19번, 1-6열
        for (int row = 1; row <= 6; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 E구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 9. 2층 F구역: 1-32번, 1-6열
        for (int row = 1; row <= 6; row++) {
            for (int num = 1; num <= 32; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 F구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 10. 2층 G구역: 1-19번, 1-6열
        for (int row = 1; row <= 6; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 G구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 11. 2층 H구역: 전체 번호, 1-7열
        // 번호 정보가 없으므로 임시로 1-50번으로 생성 (나중에 수정 필요)
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 50; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 H구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description("번호 정보 확인 필요")
                        .build());
            }
        }

        // 12. 2층 I구역: 전체 번호, 1-7열
        // 번호 정보가 없으므로 임시로 1-50번으로 생성 (나중에 수정 필요)
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 50; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 I구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description("번호 정보 확인 필요")
                        .build());
            }
        }

        // 13. 2층 J구역: 전체 번호, 1-7열
        // 번호 정보가 없으므로 임시로 1-50번으로 생성 (나중에 수정 필요)
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 50; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 J구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description("번호 정보 확인 필요")
                        .build());
            }
        }

        // 14. 2층 K구역: 1-19번, 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 K구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 15. 원정석 A구역: 1-19번, 1-6열
        for (int row = 1; row <= 6; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 A구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 16. 원정석 B구역: 1-35번, 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 35; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 B구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 17. 원정석 C구역: 전체 번호, 1-7열
        // 번호 정보가 없으므로 임시로 1-50번으로 생성 (나중에 수정 필요)
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 50; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 C구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor(null)
                        .description("번호 정보 확인 필요")
                        .build());
            }
        }

        return seats;
    }

    // 부천체육관 좌석 데이터 생성
    private List<StadiumSeat> initBucheonSeats() {
        Optional<Stadium> stadium = findStadiumByName("부천체육관");
        if (stadium.isEmpty()) {
            return new ArrayList<>();
        }

        String stadiumId = stadium.get().getId();
        List<StadiumSeat> seats = new ArrayList<>();

        // 1. flex석 (4자리)
        for (int i = 1; i <= 4; i++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("flex석")
                    .blockName(null)
                    .row("전체")
                    .number(i + "번")
                    .seatType("flex석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 2. 하나라운지석 (1-14번) 3열 - 2자리씩 예매 가능
        // 좌석은 1-14번 각각 개별 좌석으로 존재, 예매는 2자리씩 묶어서 가능
        for (int num = 1; num <= 14; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("하나라운지석")
                    .blockName(null)
                    .row("3열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 3. 하나합파이브석
        // (1-15번) - 홈 15자리
        for (int num = 1; num <= 15; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("하나합파이브석")
                    .blockName("홈")
                    .row("전체")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // (16-30번) - 어웨이 15자리
        for (int num = 16; num <= 30; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("하나합파이브석")
                    .blockName("어웨이")
                    .row("전체")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 4. 하나원큐석 (A) - (1-10번) 1-2열
        for (int row = 1; row <= 2; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("하나원큐석")
                        .blockName("A")
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 5. 하나원큐석 (H) - (1-10번) 1-2열
        for (int row = 1; row <= 2; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("하나원큐석")
                        .blockName("H")
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 6. 하나프렌즈석 (1-54번) 2-9열 (1,10열 판매 안함)
        for (int row = 2; row <= 9; row++) {
            for (int num = 1; num <= 54; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("하나프렌즈석")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description("1,10열 판매 안함")
                        .build());
            }
        }

        // 7. 2층 A2구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 A2구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 8. 2층 A3구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 A3구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 9. 2층 A4구역 (1-5번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 5; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 A4구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 10. 2층 A5구역 (1-5번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 5; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 A5구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 11. 2층 A6구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 A6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 12. 2층 A7구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 A7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 13. 2층 A8구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 A8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 14. 2층 B1구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 B1구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 15. 2층 B2구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 B2구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 16. 2층 B3구역 (1-5번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 5; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 B3구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 17. 2층 B4구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 B4구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 18. 2층 B5구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 B5구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 19. 2층 B6구역 (1-5번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 5; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 B6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 20. 2층 B7구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 B7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 21. 2층 B8구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 B8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 22. 2층 C1구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 C1구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 23. 2층 C2구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 C2구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 24. 2층 C3구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 C3구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 25. 2층 C4구역 (1-5번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 5; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 C4구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 26. 원정석 2층 C5구역 (1-5번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 5; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 2층 C5구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 27. 원정석 2층 C6구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 2층 C6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 28. 원정석 2층 C7구역 (1-10번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 2층 C7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 29. 3층 A2구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 A2구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 30. 3층 A3구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 A3구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 31. 3층 A4구역 (1-11번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 11; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 A4구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 32. 3층 A5구역 (1-11번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 11; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 A5구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 33. 3층 A6구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 A6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 34. 3층 A7구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 A7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 35. 3층 A8구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 A8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 36. 3층 B1구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 B1구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 37. 3층 B2구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 B2구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 38. 3층 B3구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 B3구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 39. 3층 B4구역 (1-8번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 8; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 B4구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 40. 3층 B5구역 (1-8번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 8; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 B5구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 41. 3층 B6구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 B6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 42. 3층 B7구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 B7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 43. 3층 B8구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 B8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 44. 3층 C1구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 C1구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 45. 3층 C2구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 C2구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 46. 3층 C3구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("3층 C3구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 47. 원정석 3층 C5구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 3층 C5구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 48. 원정석 3층 C6구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 3층 C6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        // 49. 원정석 3층 C7구역 (1-12번) 8-15열
        for (int row = 8; row <= 15; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 3층 C7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("3층")
                        .description(null)
                        .build());
            }
        }

        return seats;
    }

    // 청주체육관 좌석 데이터 생성
    private List<StadiumSeat> initCheongjuSeats() {
        Optional<Stadium> stadium = findStadiumByName("청주체육관");
        if (stadium.isEmpty()) {
            return new ArrayList<>();
        }

        String stadiumId = stadium.get().getId();
        List<StadiumSeat> seats = new ArrayList<>();

        // 1. 스타뱅킹석 - s1부터 s60까지 (s가 붙은 넘버링)
        for (int num = 1; num <= 60; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("스타뱅킹석")
                    .blockName(null)
                    .row("전체")
                    .number("s" + num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 2. 리브모바일석 1 (10번) 1-3열
        // 10번이 1-3열에 있다는 의미로 해석 (각 열에 10번)
        for (int row = 1; row <= 3; row++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("리브모바일석 1")
                    .blockName(null)
                    .row(row + "열")
                    .number("10번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 3. 리브모바일석 2 (10번) 1-3열 - 1번부터 30번까지일 수도 있음
        // 1-30번으로 생성
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 30; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("리브모바일석 2")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 4. 커플석 (3,3,3) 2명씩 3쌍 3덩어리
        // 3개 덩어리, 각 덩어리에 3쌍, 각 쌍에 2명씩 = 총 18개 좌석
        // 좌석 번호를 1-18번으로 생성
        for (int num = 1; num <= 18; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("커플석")
                    .blockName(null)
                    .row("전체")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description("2명씩 3쌍 3덩어리")
                    .build());
        }

        // 5. KB 여섯시은행석 (테이블석)
        // 좌석 정보 없음 - 나중에 보완 예정
        // 일단 생성하지 않음

        // 6. 6구역 (3,2,2,3) A-B열
        // 자리 배치: 3개, 2개, 2개, 3개 = 1-3번, 4-5번, 6-7번, 8-10번
        for (char row = 'A'; row <= 'B'; row++) {
            // 1-3번 (3개)
            for (int num = 1; num <= 3; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 4-5번 (2개)
            for (int num = 4; num <= 5; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 6-7번 (2개)
            for (int num = 6; num <= 7; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 8-10번 (3개)
            for (int num = 8; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 7. 7구역 (4,3,3,4) A-B열
        // 자리 배치: 4개, 3개, 3개, 4개 = 1-4번, 5-7번, 8-10번, 11-14번
        for (char row = 'A'; row <= 'B'; row++) {
            // 1-4번 (4개)
            for (int num = 1; num <= 4; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 5-7번 (3개)
            for (int num = 5; num <= 7; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 8-10번 (3개)
            for (int num = 8; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 11-14번 (4개)
            for (int num = 11; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 8. 8구역 (3,2,2,3) A-B열
        // 자리 배치: 3개, 2개, 2개, 3개 = 1-3번, 4-5번, 6-7번, 8-10번
        for (char row = 'A'; row <= 'B'; row++) {
            // 1-3번 (3개)
            for (int num = 1; num <= 3; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 4-5번 (2개)
            for (int num = 4; num <= 5; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 6-7번 (2개)
            for (int num = 6; num <= 7; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
            // 8-10번 (3개)
            for (int num = 8; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 9. 9구역 (2,2,2,3 -A열 / 2,3,3,3 - B열)
        // A열: 자리 배치 2개, 2개, 2개, 3개 = 1-2번, 3-4번, 5-6번, 7-9번
        // 1-2번 (2개)
        for (int num = 1; num <= 2; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("9구역")
                    .blockName(null)
                    .row("A열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 3-4번 (2개)
        for (int num = 3; num <= 4; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("9구역")
                    .blockName(null)
                    .row("A열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 5-6번 (2개)
        for (int num = 5; num <= 6; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("9구역")
                    .blockName(null)
                    .row("A열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 7-9번 (3개)
        for (int num = 7; num <= 9; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("9구역")
                    .blockName(null)
                    .row("A열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // B열: 자리 배치 2개, 3개, 3개, 3개 = 1-2번, 3-5번, 6-8번, 9-11번
        // 1-2번 (2개)
        for (int num = 1; num <= 2; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("9구역")
                    .blockName(null)
                    .row("B열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 3-5번 (3개)
        for (int num = 3; num <= 5; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("9구역")
                    .blockName(null)
                    .row("B열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 6-8번 (3개)
        for (int num = 6; num <= 8; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("9구역")
                    .blockName(null)
                    .row("B열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 9-11번 (3개)
        for (int num = 9; num <= 11; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("9구역")
                    .blockName(null)
                    .row("B열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 10. 열광응원석 11구역 (1-13번) A-D열
        for (char row = 'A'; row <= 'D'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("열광응원석 11구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 11. 열광응원석 12구역 (1-13번) A-D열
        for (char row = 'A'; row <= 'D'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("열광응원석 12구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 12. 열광응원석 38구역 (1-10번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("열광응원석 38구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 13. 열광응원석 39구역 (1-10번) A-H열
        for (char row = 'A'; row <= 'H'; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("열광응원석 39구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 14. 열광응원석 40구역 (1-10번) A-H열
        for (char row = 'A'; row <= 'H'; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("열광응원석 40구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 15. 열광응원석 41구역 (1-10번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("열광응원석 41구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 16. 일반석 10구역 (1-12번) A-D열
        for (char row = 'A'; row <= 'D'; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 10구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 17. 일반석 13구역 (1-13번) A-D열
        for (char row = 'A'; row <= 'D'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 13구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 18. 일반석 14구역 (1-13번) A-C열
        for (char row = 'A'; row <= 'C'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 14구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 19. 일반석 15구역 (1-2번) A열 / B열은 장애인석
        for (int num = 1; num <= 2; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("일반석 15구역")
                    .blockName(null)
                    .row("A열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // B열은 장애인석
        for (int num = 1; num <= 2; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("일반석 15구역")
                    .blockName(null)
                    .row("B열")
                    .number(num + "번")
                    .seatType("장애인석")
                    .floor(null)
                    .description("장애인석")
                    .build());
        }

        // 20. 일반석 16구역 (1-12번) A-J열
        for (char row = 'A'; row <= 'J'; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 16구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 21. 일반석 17구역 (1-12번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 17구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 22. 일반석 23구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 23구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 23. 일반석 24구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 24구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 24. 일반석 25구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 25구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 25. 일반석 26구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 26구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 26. 일반석 27구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 27구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 27. 일반석 28구역 (1-11번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 11; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 28구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 28. 일반석 29구역 (1-12번) A-N열
        for (char row = 'A'; row <= 'N'; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 29구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 29. 일반석 30구역 (1-13번) A-N열
        for (char row = 'A'; row <= 'N'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 30구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 30. 일반석 31구역 (1-11번) A-N열
        for (char row = 'A'; row <= 'N'; row++) {
            for (int num = 1; num <= 11; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 31구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 31. 일반석 32구역 (1-11번) A-N열
        for (char row = 'A'; row <= 'N'; row++) {
            for (int num = 1; num <= 11; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 32구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 32. 일반석 33구역 (1-12번) A-N열
        for (char row = 'A'; row <= 'N'; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 33구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 33. 일반석 34구역 (1-13번) A-N열
        for (char row = 'A'; row <= 'N'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 34구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 34. 일반석 35구역 (1-10번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 35구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 35. 일반석 36구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 36구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 36. 일반석 37구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 37구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 37. 일반석 42구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 42구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 38. 일반석 43구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 43구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 39. 일반석 44구역 (1-10번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 10; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 44구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 40. 일반석 45구역 (1-13번) A-M열
        for (char row = 'A'; row <= 'M'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 45구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 41. 일반석 46구역 (1-12번) A-M열
        for (char row = 'A'; row <= 'M'; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 46구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 42. 일반석 47구역 (1-12번) A-J열
        for (char row = 'A'; row <= 'J'; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 47구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 43. 일반석 4구역 (1-13번) A-D열
        for (char row = 'A'; row <= 'D'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 4구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 44. 일반석 5구역 (1-14번) A-D열
        for (char row = 'A'; row <= 'D'; row++) {
            for (int num = 1; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 5구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 45. 휠체어석 어웨이 (휠체어 1,3,5,7 / 보호자 2,4,6,8)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 어웨이")
                .blockName(null)
                .row("전체")
                .number("1번")
                .seatType("휠체어석")
                .floor(null)
                .description("휠체어")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 어웨이")
                .blockName(null)
                .row("전체")
                .number("2번")
                .seatType("휠체어석")
                .floor(null)
                .description("보호자")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 어웨이")
                .blockName(null)
                .row("전체")
                .number("3번")
                .seatType("휠체어석")
                .floor(null)
                .description("휠체어")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 어웨이")
                .blockName(null)
                .row("전체")
                .number("4번")
                .seatType("휠체어석")
                .floor(null)
                .description("보호자")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 어웨이")
                .blockName(null)
                .row("전체")
                .number("5번")
                .seatType("휠체어석")
                .floor(null)
                .description("휠체어")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 어웨이")
                .blockName(null)
                .row("전체")
                .number("6번")
                .seatType("휠체어석")
                .floor(null)
                .description("보호자")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 어웨이")
                .blockName(null)
                .row("전체")
                .number("7번")
                .seatType("휠체어석")
                .floor(null)
                .description("휠체어")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 어웨이")
                .blockName(null)
                .row("전체")
                .number("8번")
                .seatType("휠체어석")
                .floor(null)
                .description("보호자")
                .build());

        // 46. 휠체어석 홈 (휠체어 5,7 / 보호자 6,8)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 홈")
                .blockName(null)
                .row("전체")
                .number("5번")
                .seatType("휠체어석")
                .floor(null)
                .description("휠체어")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 홈")
                .blockName(null)
                .row("전체")
                .number("6번")
                .seatType("휠체어석")
                .floor(null)
                .description("보호자")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 홈")
                .blockName(null)
                .row("전체")
                .number("7번")
                .seatType("휠체어석")
                .floor(null)
                .description("휠체어")
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("휠체어석 홈")
                .blockName(null)
                .row("전체")
                .number("8번")
                .seatType("휠체어석")
                .floor(null)
                .description("보호자")
                .build());

        // 47. 원정석 18구역 (1-13번) A-M열
        for (char row = 'A'; row <= 'M'; row++) {
            for (int num = 1; num <= 13; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 18구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 48. 원정석 19구역 (1-11번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 11; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 19구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 49. 원정석 20구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 20구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 50. 원정석 21구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 21구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 51. 원정석 22구역 (1-9번) A-K열
        for (char row = 'A'; row <= 'K'; row++) {
            for (int num = 1; num <= 9; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 22구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        return seats;
    }

    // 부산 사직실내체육관 좌석 데이터 생성 (추가 예정)
    // private List<StadiumSeat> initBusanSeats() {
    // Optional<Stadium> stadium = findStadiumByName("부산 사직실내체육관");
    // if (stadium.isEmpty()) {
    // System.out.println("부산 사직실내체육관을 찾을 수 없습니다.");
    // return new ArrayList<>();
    // }
    //
    // String stadiumId = stadium.get().getId();
    // List<StadiumSeat> seats = new ArrayList<>();
    //
    // // 부산 사직실내체육관 좌석 데이터 생성 로직
    // // ...
    //
    // System.out.println("부산 사직실내체육관 좌석 데이터 생성 완료: " + seats.size() + "개");
    // return seats;
    // }

    // 용인실내체육관 좌석 데이터 생성
    private List<StadiumSeat> initYonginSeats() {
        Optional<Stadium> stadium = findStadiumByName("용인실내체육관");
        if (stadium.isEmpty()) {
            return new ArrayList<>();
        }

        String stadiumId = stadium.get().getId();
        List<StadiumSeat> seats = new ArrayList<>();

        // 1. flex석 (Home 2자리 / Away 2자리)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("flex석")
                .blockName("Home")
                .row("전체")
                .number("1번")
                .seatType("flex석")
                .floor(null)
                .description(null)
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("flex석")
                .blockName("Home")
                .row("전체")
                .number("2번")
                .seatType("flex석")
                .floor(null)
                .description(null)
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("flex석")
                .blockName("Away")
                .row("전체")
                .number("1번")
                .seatType("flex석")
                .floor(null)
                .description(null)
                .build());
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("flex석")
                .blockName("Away")
                .row("전체")
                .number("2번")
                .seatType("flex석")
                .floor(null)
                .description(null)
                .build());

        // 2. Sideflex석 1-4번 (4자리)
        for (int num = 1; num <= 4; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("Sideflex석")
                    .blockName(null)
                    .row("전체")
                    .number(num + "번")
                    .seatType("flex석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 3. 버팔로석 (Home R29 - R59 / Away R29 - R59)
        // Home
        for (int num = 29; num <= 59; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("버팔로석")
                    .blockName("Home")
                    .row("전체")
                    .number("R" + num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // Away
        for (int num = 29; num <= 59; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("버팔로석")
                    .blockName("Away")
                    .row("전체")
                    .number("R" + num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 4. 티켓링크석 (Home T1 - T7 / 각 1,2 // Away T8 - T14 / 각 1,2)
        // Home T1-T7, 각각 1,2번
        for (int t = 1; t <= 7; t++) {
            for (int num = 1; num <= 2; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("티켓링크석")
                        .blockName("Home")
                        .row("전체")
                        .number("T" + t + "-" + num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }
        // Away T8-T14, 각각 1,2번
        for (int t = 8; t <= 14; t++) {
            for (int num = 1; num <= 2; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("티켓링크석")
                        .blockName("Away")
                        .row("전체")
                        .number("T" + t + "-" + num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 5. 삼성생명석 (3,4,3) 1열
        // 자리 배치: 3개, 4개, 3개 = 1-3번, 4-7번, 8-10번
        // 1-3번 (3개)
        for (int num = 1; num <= 3; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("삼성생명석")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 4-7번 (4개)
        for (int num = 4; num <= 7; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("삼성생명석")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 8-10번 (3개)
        for (int num = 8; num <= 10; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("삼성생명석")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 6. 해태버터링석 (1-12번) 총2열 6석씩 2줄
        for (int row = 1; row <= 2; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("해태버터링석")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 7. 해태오예스석 (1-12번) 총2열 6석씩 2줄
        for (int row = 1; row <= 2; row++) {
            for (int num = 1; num <= 12; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("해태오예스석")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 8. 블루밍스 응원석 1층 D구역 (1-27번) 1-6열
        for (int row = 1; row <= 6; row++) {
            for (int num = 1; num <= 27; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("블루밍스 응원석 1층 D구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("1층")
                        .description(null)
                        .build());
            }
        }

        // 9. 블루밍스 응원석 1층 E구역 (1-27번) 1-6열
        for (int row = 1; row <= 6; row++) {
            for (int num = 1; num <= 27; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("블루밍스 응원석 1층 E구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("1층")
                        .description(null)
                        .build());
            }
        }

        // 10. 블루밍스 응원석 1층 F구역 (1-27번) 1-6열
        for (int row = 1; row <= 6; row++) {
            for (int num = 1; num <= 27; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("블루밍스 응원석 1층 F구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("1층")
                        .description(null)
                        .build());
            }
        }

        // 11. 블루밍스 응원석 2층 D구역 (1-19번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("블루밍스 응원석 2층 D구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 12. 블루밍스 응원석 2층 E구역 (1-27번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 27; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("블루밍스 응원석 2층 E구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 13. 블루밍스 응원석 2층 F구역 (1-19번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("블루밍스 응원석 2층 F구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 14. 일반석 A구역 (1층) (1-27번) 1-9열
        for (int row = 1; row <= 9; row++) {
            for (int num = 1; num <= 27; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 A구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("1층")
                        .description(null)
                        .build());
            }
        }

        // 15. 일반석 A구역 (2층) (1-19번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 A구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 16. 일반석 B구역 (2층) (1-27번) 1-2열
        for (int row = 1; row <= 2; row++) {
            for (int num = 1; num <= 27; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 B구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 17. 일반석 G구역 (1-20번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 20; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 G구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 18. 일반석 H구역 (1-20번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 20; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("일반석 H구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 19. 원정석 C구역 (1층) (1-27번) 1-9열
        for (int row = 1; row <= 9; row++) {
            for (int num = 1; num <= 27; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 C구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("1층")
                        .description(null)
                        .build());
            }
        }

        // 20. 원정석 C구역 (2층) (1-19번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("원정석 C구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        return seats;
    }

    // 인천 도원체육관 좌석 데이터 생성
    private List<StadiumSeat> initIncheonSeats() {
        Optional<Stadium> stadium = findStadiumByName("인천 도원체육관");
        if (stadium.isEmpty()) {
            return new ArrayList<>();
        }

        String stadiumId = stadium.get().getId();
        List<StadiumSeat> seats = new ArrayList<>();

        // 1. flex석 (4자리)
        for (int num = 1; num <= 4; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("flex석")
                    .blockName(null)
                    .row("전체")
                    .number(num + "번")
                    .seatType("flex석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 2. 슈퍼Sol석 (1-8, 9-16) 1-2열
        // 1열에 1-8번, 2열에 9-16번으로 해석
        for (int num = 1; num <= 8; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("슈퍼Sol석")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        for (int num = 9; num <= 16; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("슈퍼Sol석")
                    .blockName(null)
                    .row("2열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 3. Oh! Yes석 R1 구역 (1-16번) 1-5열
        for (int row = 1; row <= 5; row++) {
            for (int num = 1; num <= 16; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("Oh! Yes석 R1 구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 4. Oh! Yes석 R2 구역 (1-14번) 1-5열
        for (int row = 1; row <= 5; row++) {
            for (int num = 1; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("Oh! Yes석 R2 구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 5. Oh! Yes석 R3 구역 (1-16번) 1-5열
        for (int row = 1; row <= 5; row++) {
            for (int num = 1; num <= 16; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("Oh! Yes석 R3 구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 6. Maeil응원석 (2층 테이블석) C구역 (1-21번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 21; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("Maeil응원석 C구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description("테이블석")
                        .build());
            }
        }

        // 7. Maeil응원석 (2층 테이블석) G구역 (1-21번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 21; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("Maeil응원석 G구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description("테이블석")
                        .build());
            }
        }

        // 8. Sol쏠한 응원석 (2층) D구역 (1-40번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 40; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("Sol쏠한 응원석 D구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 9. Sol쏠한 응원석 (2층) E구역 (1-28번) 1-6열
        for (int row = 1; row <= 6; row++) {
            for (int num = 1; num <= 28; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("Sol쏠한 응원석 E구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 10. Sol쏠한 응원석 (2층) F구역 (1-40번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 40; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("Sol쏠한 응원석 F구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 11. 2층 일반석 (자동배정) (1-21번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 21; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 일반석")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description("자동배정")
                        .build());
            }
        }

        // 12. 2층 일반석 (자동배정) (1-40번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 40; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 일반석")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description("자동배정")
                        .build());
            }
        }

        // 13. 2층 원정석 (자동배정) (1-40번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 40; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 원정석")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("원정석")
                        .floor("2층")
                        .description("자동배정")
                        .build());
            }
        }

        // 14. 2층 휠체어석 (자동배정) (1-21번) 1-3열
        for (int row = 1; row <= 3; row++) {
            for (int num = 1; num <= 21; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 휠체어석")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("휠체어석")
                        .floor("2층")
                        .description("자동배정")
                        .build());
            }
        }

        return seats;
    }

    // 부산 사직실내체육관 좌석 데이터 생성
    private List<StadiumSeat> initBusanSeats() {
        Optional<Stadium> stadium = findStadiumByName("부산 사직실내체육관");
        if (stadium.isEmpty()) {
            return new ArrayList<>();
        }

        String stadiumId = stadium.get().getId();
        List<StadiumSeat> seats = new ArrayList<>();

        // 1. flex석 (4자리)
        for (int num = 1; num <= 4; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("flex석")
                    .blockName(null)
                    .row("전체")
                    .number(num + "번")
                    .seatType("flex석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 2. 플로어석 (1-20/21-40) 총 2열
        // 1열에 1-20번
        for (int num = 1; num <= 20; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("플로어석")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }
        // 2열에 21-40번
        for (int num = 21; num <= 40; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("플로어석")
                    .blockName(null)
                    .row("2열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor(null)
                    .description(null)
                    .build());
        }

        // 3. 익사이팅석 1 (1-28번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 28; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("익사이팅석 1")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 4. 익사이팅석 2 (1-28번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 28; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("익사이팅석 2")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 5. 익사이팅석 3 (1-38번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 38; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("익사이팅석 3")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 6. 익사이팅석 4 (1-38번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 38; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("익사이팅석 4")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 7. 익사이팅석 5 (1-48번) 1-8열
        for (int row = 1; row <= 8; row++) {
            for (int num = 1; num <= 48; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("익사이팅석 5")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 8. 익사이팅석 6 (1-48번) 1-8열
        for (int row = 1; row <= 8; row++) {
            for (int num = 1; num <= 48; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("익사이팅석 6")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor(null)
                        .description(null)
                        .build());
            }
        }

        // 9. 2층 VIP 테이블석 1구역 (1-16번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 16; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 VIP 테이블석 1구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("VIP석")
                        .floor("2층")
                        .description("테이블석")
                        .build());
            }
        }

        // 10. 2층 VIP 테이블석 2구역 (1-16번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 16; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 VIP 테이블석 2구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("VIP석")
                        .floor("2층")
                        .description("테이블석")
                        .build());
            }
        }

        // 11. 2층 시네마석 1구역 (1-14번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 시네마석 1구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 12. 2층 시네마석 2구역 (1-14번) 1-7열
        for (int row = 1; row <= 7; row++) {
            for (int num = 1; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 시네마석 2구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("일반석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 13. 2층 패밀리석 1구역 - 4자리 (1-4번, 1열)
        for (int num = 1; num <= 4; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("2층 패밀리석 1구역")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor("2층")
                    .description("패밀리석")
                    .build());
        }

        // 14. 2층 패밀리석 2구역 - 2자리 (1-2번, 1열)
        for (int num = 1; num <= 2; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("2층 패밀리석 2구역")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor("2층")
                    .description("패밀리석")
                    .build());
        }

        // 15. 2층 패밀리석 3구역 - 2자리 (1-2번, 1열)
        for (int num = 1; num <= 2; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("2층 패밀리석 3구역")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor("2층")
                    .description("패밀리석")
                    .build());
        }

        // 16. 2층 패밀리석 4구역 - 4자리 (1-4번, 1열)
        for (int num = 1; num <= 4; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("2층 패밀리석 4구역")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor("2층")
                    .description("패밀리석")
                    .build());
        }

        // 17. 2층 패밀리석 5구역 - 4자리 (1-4번, 1열)
        for (int num = 1; num <= 4; num++) {
            seats.add(StadiumSeat.builder()
                    .stadiumId(stadiumId)
                    .zoneName("2층 패밀리석 5구역")
                    .blockName(null)
                    .row("1열")
                    .number(num + "번")
                    .seatType("일반석")
                    .floor("2층")
                    .description("패밀리석")
                    .build());
        }

        // 18. 2층 응원석 2-10구역 (1-34번) 1-20열
        for (int row = 1; row <= 20; row++) {
            for (int num = 1; num <= 34; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-10구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 19. 2층 응원석 2-11구역 (1-15번) 1-20열
        for (int row = 1; row <= 20; row++) {
            for (int num = 1; num <= 15; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-11구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 20. 2층 응원석 2-12구역 (1-15번) 1-18열
        for (int row = 1; row <= 18; row++) {
            for (int num = 1; num <= 15; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-12구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 21. 2층 응원석 2-13구역 (1-15번) 1-18열
        for (int row = 1; row <= 18; row++) {
            for (int num = 1; num <= 15; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-13구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 22. 2층 응원석 2-14구역 (1-15번) 1-20열
        for (int row = 1; row <= 20; row++) {
            for (int num = 1; num <= 15; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-14구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 23. 2층 응원석 2-15구역 (1-34번) 1-20열
        for (int row = 1; row <= 20; row++) {
            for (int num = 1; num <= 34; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-15구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 24. 2층 응원석 2-16구역 (1-14번) 1-17열
        for (int row = 1; row <= 17; row++) {
            for (int num = 1; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-16구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 25. 2층 응원석 2-17구역 (1-34번) 1-20열
        for (int row = 1; row <= 20; row++) {
            for (int num = 1; num <= 34; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-17구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 26. 2층 응원석 2-18구역 (1-15번) 1-18열
        for (int row = 1; row <= 18; row++) {
            for (int num = 1; num <= 15; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-18구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 27. 2층 응원석 2-19구역 (1-15번) 1-18열
        for (int row = 1; row <= 18; row++) {
            for (int num = 1; num <= 15; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-19구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 28. 2층 응원석 2-20구역 (1-34번) 1-20열
        for (int row = 1; row <= 20; row++) {
            for (int num = 1; num <= 34; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-20구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 29. 2층 응원석 2-21구역 (1-14번) 1-17열
        for (int row = 1; row <= 17; row++) {
            for (int num = 1; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-21구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 30. 2층 응원석 2-22구역 (1-18번) 1-17열
        for (int row = 1; row <= 17; row++) {
            for (int num = 1; num <= 18; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-22구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 31. 2층 응원석 2-3구역 (1-19번) 1-17열
        for (int row = 1; row <= 17; row++) {
            for (int num = 1; num <= 19; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-3구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 32. 2층 응원석 2-4구역 (1-14번) 1-17열
        for (int row = 1; row <= 17; row++) {
            for (int num = 1; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-4구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 33. 2층 응원석 2-5구역 (1-34번) 1-20열
        for (int row = 1; row <= 20; row++) {
            for (int num = 1; num <= 34; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-5구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 34. 2층 응원석 2-6구역 (1-15번) 1-18열
        for (int row = 1; row <= 18; row++) {
            for (int num = 1; num <= 15; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-6구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 35. 2층 응원석 2-7구역 (1-15번) 1-18열
        for (int row = 1; row <= 18; row++) {
            for (int num = 1; num <= 15; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-7구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 36. 2층 응원석 2-8구역 (1-34번) 1-20열
        for (int row = 1; row <= 20; row++) {
            for (int num = 1; num <= 34; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-8구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 37. 2층 응원석 2-9구역 (1-14번) 1-17열
        for (int row = 1; row <= 17; row++) {
            for (int num = 1; num <= 14; num++) {
                seats.add(StadiumSeat.builder()
                        .stadiumId(stadiumId)
                        .zoneName("2층 응원석 2-9구역")
                        .blockName(null)
                        .row(row + "열")
                        .number(num + "번")
                        .seatType("응원석")
                        .floor("2층")
                        .description(null)
                        .build());
            }
        }

        // 38. 휠체어석 2-13구역 (휠체어석 5,6열 1번 / 동반자석 5,6열 2번)
        // 5열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-13구역")
                .blockName(null)
                .row("5열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 5열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-13구역")
                .blockName(null)
                .row("5열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());
        // 6열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-13구역")
                .blockName(null)
                .row("6열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 6열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-13구역")
                .blockName(null)
                .row("6열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());

        // 39. 휠체어석 2-18구역 (휠체어석 7,8열 1번 / 동반자석 7,8열 2번)
        // 7열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-18구역")
                .blockName(null)
                .row("7열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 7열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-18구역")
                .blockName(null)
                .row("7열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());
        // 8열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-18구역")
                .blockName(null)
                .row("8열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 8열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-18구역")
                .blockName(null)
                .row("8열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());

        // 40. 휠체어석 2-19구역 (휠체어석 9,10열 1번 / 동반자석 9,10열 2번)
        // 9열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-19구역")
                .blockName(null)
                .row("9열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 9열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-19구역")
                .blockName(null)
                .row("9열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());
        // 10열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-19구역")
                .blockName(null)
                .row("10열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 10열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-19구역")
                .blockName(null)
                .row("10열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());

        // 41. 휠체어석 2-6구역 (휠체어석 1,2열 1번 / 동반자석 1,2열 2번)
        // 1열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-6구역")
                .blockName(null)
                .row("1열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 1열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-6구역")
                .blockName(null)
                .row("1열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());
        // 2열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-6구역")
                .blockName(null)
                .row("2열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 2열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-6구역")
                .blockName(null)
                .row("2열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());

        // 42. 휠체어석 2-7구역 (휠체어석 3,4열 1번 / 동반자석 3,4열 2번)
        // 3열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-7구역")
                .blockName(null)
                .row("3열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 3열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-7구역")
                .blockName(null)
                .row("3열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());
        // 4열 1번 (휠체어석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-7구역")
                .blockName(null)
                .row("4열")
                .number("1번")
                .seatType("휠체어석")
                .floor("2층")
                .description("휠체어석")
                .build());
        // 4열 2번 (동반자석)
        seats.add(StadiumSeat.builder()
                .stadiumId(stadiumId)
                .zoneName("2층 응원석 2-7구역")
                .blockName(null)
                .row("4열")
                .number("2번")
                .seatType("휠체어석")
                .floor("2층")
                .description("동반자석")
                .build());

        return seats;
    }

    // 경기장 이름으로 찾기 헬퍼 메서드
    private Optional<Stadium> findStadiumByName(String stadiumName) {
        return stadiumRepository.findAll().stream()
                .filter(s -> stadiumName.equals(s.getName()))
                .findFirst();
    }
}
