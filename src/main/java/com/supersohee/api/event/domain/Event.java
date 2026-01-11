package com.supersohee.api.event.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.supersohee.api.common.BaseDocument;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "events")
public class Event extends BaseDocument {
    
    @Id
    private String id;
    
    private String title;
    private String url;
    private String description;
    
    private String check1;
    private String check2;
    private String check3;
    
    // R2 키 리스트로 저장
    private List<String> photoKeys;
    
    // 노출 여부 (어드민에서 관리)
    private Boolean isActive;
}

