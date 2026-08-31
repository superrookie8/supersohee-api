package com.supersohee.api.event.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.event.domain.Event;
import com.supersohee.api.event.domain.EventDisplayOrder;
import com.supersohee.api.event.dto.AdminEventOrderResponse;
import com.supersohee.api.event.repository.EventDisplayOrderRepository;
import com.supersohee.api.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventService {
    
    private final EventRepository eventRepository;
    private final EventDisplayOrderRepository eventDisplayOrderRepository;

    private static final Comparator<Event> LEGACY_FALLBACK_ORDER = Comparator
            .comparing(Event::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(event -> event.getId() == null ? "" : event.getId());
    
    public List<Event> findActiveEvents() {
        return applyDisplayOrder(eventRepository.findByIsActiveTrueOrderByCreatedAtDesc());
    }
    
    public Optional<Event> findById(String id) {
        return eventRepository.findByIdAndIsActiveTrue(id);
    }
    
    // 어드민용: 모든 이벤트 조회 (비활성 포함)
    public List<Event> findAllEvents() {
        return applyDisplayOrder(eventRepository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * The complete order is stored in one Mongo document, so concurrent reorder
     * requests are atomic last-write-wins instead of interleaving Event updates.
     */
    public AdminEventOrderResponse reorderEvents(List<String> requestedIds) {
        if (new LinkedHashSet<>(requestedIds).size() != requestedIds.size()) {
            throw AdminApiException.validation(
                    "Event order validation failed.",
                    Map.of("eventIds", "eventIds must not contain duplicates."));
        }

        Set<String> currentIds = new HashSet<>();
        for (Event event : eventRepository.findAll()) {
            currentIds.add(event.getId());
        }
        Set<String> requestedSet = new HashSet<>(requestedIds);
        if (!currentIds.equals(requestedSet)) {
            throw AdminApiException.conflict(
                    "Event order is stale.",
                    Map.of("eventIds", "eventIds must contain every current event id exactly once."));
        }

        List<String> storedIds = List.copyOf(requestedIds);
        eventDisplayOrderRepository.save(new EventDisplayOrder(EventDisplayOrder.GLOBAL_ID, storedIds));
        return new AdminEventOrderResponse("Event order saved successfully.", storedIds);
    }
    
    // 어드민용: 이벤트 생성
    @Transactional
    public Event createEvent(
            String title,
            String url,
            String description,
            String check1,
            String check2,
            String check3,
            List<String> photoKeys,
            Boolean isActive) {
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("이벤트 제목은 필수입니다");
        }
        
        Event event = Event.builder()
                .title(title)
                .url(url)
                .description(description)
                .check1(check1)
                .check2(check2)
                .check3(check3)
                .photoKeys(photoKeys != null ? photoKeys : new ArrayList<>())
                .isActive(isActive != null ? isActive : true)
                .build();
        
        return eventRepository.save(event);
    }
    
    // 어드민용: 이벤트 수정
    @Transactional
    public Event updateEvent(
            String eventId,
            String title,
            String url,
            String description,
            String check1,
            String check2,
            String check3,
            List<String> newPhotoKeys,
            Boolean isActive) {
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> AdminApiException.notFound("Event"));
        
        // 기존 photoKeys 가져오기
        List<String> photoKeys = event.getPhotoKeys() != null 
                ? new ArrayList<>(event.getPhotoKeys()) 
                : new ArrayList<>();
        
        // 새 이미지가 있으면 추가
        if (newPhotoKeys != null && !newPhotoKeys.isEmpty()) {
            photoKeys.addAll(newPhotoKeys);
        }
        
        // 업데이트
        Event updatedEvent = Event.builder()
                .id(event.getId())
                .title(title)
                .url(url)
                .description(description)
                .check1(check1)
                .check2(check2)
                .check3(check3)
                .photoKeys(photoKeys)
                .isActive(isActive != null ? isActive : event.getIsActive())
                .build();
        
        // BaseDocument 필드 유지
        updatedEvent.setCreatedAt(event.getCreatedAt());
        
        return eventRepository.save(updatedEvent);
    }
    
    // 어드민용: 이벤트에서 특정 사진 삭제
    @Transactional
    public Event deleteEventPhoto(String eventId, String photoKey) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> AdminApiException.notFound("Event"));
        
        List<String> photoKeys = event.getPhotoKeys() != null 
                ? new ArrayList<>(event.getPhotoKeys()) 
                : new ArrayList<>();
        
        if (!photoKeys.contains(photoKey)) {
            throw AdminApiException.notFound("Event photo");
        }
        
        photoKeys.remove(photoKey);
        
        Event updatedEvent = Event.builder()
                .id(event.getId())
                .title(event.getTitle())
                .url(event.getUrl())
                .description(event.getDescription())
                .check1(event.getCheck1())
                .check2(event.getCheck2())
                .check3(event.getCheck3())
                .photoKeys(photoKeys)
                .isActive(event.getIsActive())
                .build();
        
        updatedEvent.setCreatedAt(event.getCreatedAt());
        
        return eventRepository.save(updatedEvent);
    }
    
    // 어드민용: 이벤트 삭제 (isActive = false)
    @Transactional
    public void deleteEvent(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> AdminApiException.notFound("Event"));
        
        Event deletedEvent = Event.builder()
                .id(event.getId())
                .title(event.getTitle())
                .url(event.getUrl())
                .description(event.getDescription())
                .check1(event.getCheck1())
                .check2(event.getCheck2())
                .check3(event.getCheck3())
                .photoKeys(event.getPhotoKeys())
                .isActive(false)
                .build();
        
        deletedEvent.setCreatedAt(event.getCreatedAt());
        eventRepository.save(deletedEvent);
    }

    private List<Event> applyDisplayOrder(List<Event> events) {
        List<String> storedIds = eventDisplayOrderRepository.findById(EventDisplayOrder.GLOBAL_ID)
                .map(EventDisplayOrder::getEventIds)
                .orElse(List.of());
        Map<String, Integer> storedPositions = new HashMap<>();
        for (int index = 0; index < storedIds.size(); index++) {
            storedPositions.putIfAbsent(storedIds.get(index), index);
        }

        List<Event> ordered = new ArrayList<>(events);
        ordered.sort((left, right) -> {
            Integer leftPosition = storedPositions.get(left.getId());
            Integer rightPosition = storedPositions.get(right.getId());
            if (leftPosition != null && rightPosition != null) {
                return Integer.compare(leftPosition, rightPosition);
            }
            if (leftPosition == null && rightPosition != null) {
                return -1;
            }
            if (leftPosition != null) {
                return 1;
            }
            return LEGACY_FALLBACK_ORDER.compare(left, right);
        });
        return ordered;
    }
}
