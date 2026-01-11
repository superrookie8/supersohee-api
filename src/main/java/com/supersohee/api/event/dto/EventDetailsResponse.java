package com.supersohee.api.event.dto;

import com.supersohee.api.event.domain.Event;
import lombok.*;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailsResponse {
    private String id;
    private String title;
    private String url;
    private String description;
    private CheckFields checkFields;
    private List<String> photos; // presigned URL 배열
    private List<String> photoKeys; // R2 키 배열 (사진 삭제용)
    
    public static EventDetailsResponse from(Event event) {
        return EventDetailsResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .url(event.getUrl())
                .description(event.getDescription())
                .checkFields(CheckFields.builder()
                        .check1(event.getCheck1())
                        .check2(event.getCheck2())
                        .check3(event.getCheck3())
                        .build())
                .build();
        // photos는 Controller에서 R2 키를 presigned URL로 변환해서 설정
    }
}

