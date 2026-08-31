package com.supersohee.api.event.controller;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.event.domain.Event;
import com.supersohee.api.event.dto.EventDetailsResponse;
import com.supersohee.api.event.dto.AdminEventOrderRequest;
import com.supersohee.api.event.dto.AdminEventOrderResponse;
import com.supersohee.api.event.service.EventService;
import com.supersohee.api.image.service.ImageUploadService;
import com.supersohee.api.image.service.ImageValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;
    private final ImageUploadService imageUploadService;

    @GetMapping
    public List<EventDetailsResponse> getAllEvents() {
        return eventService.findAllEvents().stream().map(this::response).toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDetailsResponse> createEvent(
            @RequestParam String title,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String check1,
            @RequestParam(required = false) String check2,
            @RequestParam(required = false) String check3,
            @RequestParam(required = false) List<MultipartFile> photos,
            @RequestParam(defaultValue = "true") Boolean isActive) {
        if (title == null || title.isBlank()) {
            throw AdminApiException.badRequest("Event title is required.");
        }
        try {
            List<String> photoKeys = photos == null || photos.isEmpty()
                    ? List.of()
                    : imageUploadService.uploadEventImages(photos);
            Event created = eventService.createEvent(
                    title, url, description, check1, check2, check3, photoKeys, isActive);
            return ResponseEntity.status(HttpStatus.CREATED).body(response(created));
        } catch (IOException exception) {
            throw AdminApiException.validation(
                    "Event photos could not be uploaded.",
                    Map.of("photos", "Photo upload could not be completed."));
        } catch (ImageValidationException exception) {
            throw AdminApiException.validation(
                    "Event photo validation failed.",
                    Map.of("photos", safePhotoValidationMessage(exception.reason())));
        }
    }

    @PutMapping("/order")
    public AdminEventOrderResponse reorderEvents(@Valid @RequestBody AdminEventOrderRequest request) {
        return eventService.reorderEvents(request.eventIds());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EventDetailsResponse updateEvent(
            @PathVariable String id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String check1,
            @RequestParam(required = false) String check2,
            @RequestParam(required = false) String check3,
            @RequestParam(required = false) List<MultipartFile> photos,
            @RequestParam(required = false) String isActive) {
        validateEditMetadata(title, isActive);
        List<String> newPhotoKeys;
        try {
            newPhotoKeys = photos == null || photos.isEmpty()
                    ? List.of()
                    : imageUploadService.uploadEventImages(photos);
        } catch (IOException exception) {
            throw AdminApiException.validation(
                    "Event photos could not be uploaded.",
                    Map.of("photos", "Photo upload could not be completed."));
        } catch (ImageValidationException exception) {
            throw AdminApiException.validation(
                    "Event photo validation failed.",
                    Map.of("photos", safePhotoValidationMessage(exception.reason())));
        }

        Event updated;
        try {
            updated = eventService.updateEvent(
                    id,
                    title.trim(),
                    normalizeOptional(url),
                    normalizeOptional(description),
                    normalizeOptional(check1),
                    normalizeOptional(check2),
                    normalizeOptional(check3),
                    newPhotoKeys,
                    Boolean.valueOf(isActive));
        } catch (RuntimeException updateFailure) {
            if (!rollbackNewPhotos(newPhotoKeys)) {
                throw AdminApiException.storageFailure(
                        "Event update failed and new photo cleanup was incomplete.");
            }
            throw updateFailure;
        }
        return response(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/photos")
    public EventDetailsResponse deleteEventPhoto(
            @PathVariable String id,
            @RequestParam String photoKey) {
        if (photoKey == null || photoKey.isBlank()) {
            throw AdminApiException.badRequest("photoKey is required.");
        }
        Event updated = eventService.deleteEventPhoto(id, photoKey);
        imageUploadService.deleteImage(photoKey);
        return response(updated);
    }

    private EventDetailsResponse response(Event event) {
        EventDetailsResponse response = EventDetailsResponse.from(event);
        if (event.getPhotoKeys() == null || event.getPhotoKeys().isEmpty()) {
            return response;
        }
        return response.toBuilder()
                .photos(imageUploadService.convertKeysToPresignedUrls(event.getPhotoKeys()))
                .photoKeys(event.getPhotoKeys())
                .build();
    }

    private String safePhotoValidationMessage(ImageValidationException.Reason reason) {
        return switch (reason) {
            case MISSING_FILE -> "Each photo must contain data.";
            case TOO_LARGE -> "Each photo must be 5 MiB or smaller.";
            case INVALID_FILENAME -> "Each photo must have a valid filename.";
            case INVALID_CONTENT -> "Each photo must contain readable image data.";
            case UNSUPPORTED_FORMAT -> "Photos must use jpg, jpeg, png, gif, or webp format.";
        };
    }

    private void validateEditMetadata(String title, String isActive) {
        if (title == null || title.isBlank()) {
            throw AdminApiException.validation(
                    "Event validation failed.",
                    Map.of("title", "Event title is required."));
        }
        if (!"true".equalsIgnoreCase(isActive) && !"false".equalsIgnoreCase(isActive)) {
            throw AdminApiException.validation(
                    "Event validation failed.",
                    Map.of("isActive", "isActive must be true or false."));
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean rollbackNewPhotos(List<String> newPhotoKeys) {
        boolean complete = true;
        for (String key : newPhotoKeys) {
            try {
                imageUploadService.deleteImage(key);
            } catch (RuntimeException cleanupFailure) {
                complete = false;
            }
        }
        return complete;
    }
}
