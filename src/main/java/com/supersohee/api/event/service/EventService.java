package com.supersohee.api.event.service;

import com.supersohee.api.event.domain.Event;
import com.supersohee.api.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {
    
    private final EventRepository eventRepository;
    
    public List<Event> findActiveEvents() {
        return eventRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }
    
    public Optional<Event> findById(String id) {
        return eventRepository.findByIdAndIsActiveTrue(id);
    }
    
    // 어드민용: 모든 이벤트 조회 (비활성 포함)
    public List<Event> findAllEvents() {
        return eventRepository.findAllByOrderByCreatedAtDesc();
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
                .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다"));
        
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
                .title(title != null ? title : event.getTitle())
                .url(url != null ? url : event.getUrl())
                .description(description != null ? description : event.getDescription())
                .check1(check1 != null ? check1 : event.getCheck1())
                .check2(check2 != null ? check2 : event.getCheck2())
                .check3(check3 != null ? check3 : event.getCheck3())
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
                .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다"));
        
        List<String> photoKeys = event.getPhotoKeys() != null 
                ? new ArrayList<>(event.getPhotoKeys()) 
                : new ArrayList<>();
        
        if (!photoKeys.contains(photoKey)) {
            throw new RuntimeException("해당 사진을 찾을 수 없습니다");
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
                .orElseThrow(() -> new RuntimeException("이벤트를 찾을 수 없습니다"));
        
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
}

