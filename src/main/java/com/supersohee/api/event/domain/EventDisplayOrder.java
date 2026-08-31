package com.supersohee.api.event.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "event_display_order")
public class EventDisplayOrder {
    public static final String GLOBAL_ID = "global";

    @Id
    private String id;
    private List<String> eventIds;
}
