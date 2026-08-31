package com.supersohee.api.article.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "articles")
@CompoundIndex(
        name = "article_source_url_unique",
        def = "{'source': 1, 'url': 1}",
        unique = true,
        partialFilter = "{'source': {$type: 'string'}, 'url': {$type: 'string'}}")
public class Article {
    @Id
    private String id;

    private String source;
    private String title;
    private String url;
    private String summary;
    private String imageUrl;


    private LocalDateTime publishedAt;
    private LocalDateTime crawledAt;

       // 노출/정렬용 (지금은 null 허용)
       private Integer score;
       private Boolean mainTarget;
    
}
