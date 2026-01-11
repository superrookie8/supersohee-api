package com.supersohee.api.stadium.config;

import com.supersohee.api.stadium.domain.Stadium;
import com.supersohee.api.stadium.repository.StadiumRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class StadiumDataInitializer {

    private final StadiumRepository stadiumRepository;

    @PostConstruct
    public void init() {
        // 이미 데이터가 있으면 스킵
        if (stadiumRepository.count() > 0) {
            return;
        }

        // 1. 부산 사직실내체육관
        Stadium busan = Stadium.builder()
                .name("부산 사직실내체육관")
                .address("부산시 동래구 사직동에 위치")
                .capacity(14099)
                .latitude(null) // 나중에 추가
                .longitude(null)
                .imageUrl(null)
                .subwayInfo(Arrays.asList("지하철 3호선 종합운동장역 9번 11번 출구"))
                .busInfo(Arrays.asList(
                        "46번 버스 사직야구장",
                        "80번, 105번, 111번 버스 창신초등학교",
                        "10번, 210번, 부산진구17번 버스 아시아드 주경기장",
                        "54번, 83-1번, 131번 사직실내수영장 하차"))
                .intercityRoute(
                        "KTX -> 구포역 하차 -> 지하철 3호선 구포역 승차 (9개역이동) -> 지하철 3호선 종합운동장역 하차, 11번출구로 나와서 직진 후 다이소 옆골목으로 꺾은 뒤 길따라 가면 됩니다. 단, 구포역 하루 3번 섬")
                .build();

        // 2. 용인실내체육관
        Stadium yongin = Stadium.builder()
                .name("용인실내체육관")
                .address("경기도 용인시 처인구 마평동에 위치")
                .capacity(1914)
                .latitude(null)
                .longitude(null)
                .imageUrl(null)
                .subwayInfo(Arrays.asList("에버라인 용인중앙시장 2번 출구"))
                .busInfo(Arrays.asList(
                        "66번, 66-4번 운동장 송담대역 (그외 다수)",
                        "5005번 용인터미널,용인교"))
                .intercityRoute(null)
                .build();

        // 3. 아산 이순신체육관
        Stadium asan = Stadium.builder()
                .name("아산 이순신체육관")
                .address("충청남도 아산시 서원구 풍기동에 위치")
                .capacity(3176)
                .latitude(null)
                .longitude(null)
                .imageUrl(null)
                .subwayInfo(Arrays.asList(
                        "1호선 아산역 -> 버스환승",
                        "1호선 온양온천역 -> 버스환승"))
                .busInfo(Arrays.asList(
                        "990번, 991번 버스 아산경찰서 / 동일하이빌A",
                        "900번, 910번 버스 모종2통",
                        "시즌 시 셔틀버스 운행"))
                .intercityRoute("KTX - 아산역 하차 후 버스 또는 지하철 이용, 시외버스터미널 경로도 있음")
                .build();

        // 4. 인천 도원체육관
        Stadium incheon = Stadium.builder()
                .name("인천 도원체육관")
                .address("인천시 중구 도원동에 위치")
                .capacity(2630)
                .latitude(null)
                .longitude(null)
                .imageUrl(null)
                .subwayInfo(Arrays.asList("1호선 도원역 1번 출구"))
                .busInfo(Arrays.asList(
                        "521번, 519번 버스 신흥시장",
                        "15번 버스 도원고개(도원역)",
                        "4번, 112번 인천옹진농협본점",
                        "4번, 23번, 45번 숭의로터리.도원체육관"))
                .intercityRoute(null)
                .build();

        // 5. 부천체육관
        Stadium bucheon = Stadium.builder()
                .name("부천체육관")
                .address("경기도 부천시 원미구 중동에 위치")
                .capacity(5400)
                .latitude(null)
                .longitude(null)
                .imageUrl(null)
                .subwayInfo(Arrays.asList("7호선 부천시청역 4번 출구"))
                .busInfo(Arrays.asList(
                        "70-3번, 6-2번 버스 중원고등학교",
                        "60-1번 버스 부천테크노파크4단지 401동 앞",
                        "50-1번 버스 부천체육관.부천초교사거리",
                        "83번, 5-4번 버스 부천체육관북문",
                        "302번 부천실내체육관",
                        "8번 버스 중원초교"))
                .intercityRoute(null)
                .build();

        // 6. 청주체육관
        Stadium cheongju = Stadium.builder()
                .name("청주체육관")
                .address("충청북도 청주시 서원구 사직동에 위치")
                .capacity(4183)
                .latitude(null)
                .longitude(null)
                .imageUrl(null)
                .subwayInfo(null) // 지하철 없음
                .busInfo(Arrays.asList(
                        "618번, 311번, 511번, 513번, 514번, 516번, 101번, 711번, 814번 버스 청주체육관",
                        "105번, 114번 버스 시계탑",
                        "109번, 502번 버스 사직사거리.시립미술관(교육도서관)",
                        "813번 버스 예술의전당.유네스코국제기록유산센터"))
                .intercityRoute("청주고속버스터미널 하차후 시내버스 환승, KTX 오송역 하차 후 시내버스 환승")
                .build();

        stadiumRepository.save(busan);
        stadiumRepository.save(yongin);
        stadiumRepository.save(asan);
        stadiumRepository.save(incheon);
        stadiumRepository.save(bucheon);
        stadiumRepository.save(cheongju);

        System.out.println("경기장 데이터가 생성되었습니다. (총 6개 경기장)");
    }
}