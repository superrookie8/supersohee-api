package com.supersohee.api.guestbook.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "guestbooks")
public class Guestbook {
    @Id private String id;
    private String name;
    private String message;
    private LocalDateTime date;
    private LocalDateTime createdAt;
    private String photoId;
    @Field("photo_id") private String legacyPhotoId;

    public LocalDateTime effectiveDate() {
        return date != null ? date : createdAt;
    }

    public String effectivePhotoId() {
        return photoId != null ? photoId : legacyPhotoId;
    }
}
