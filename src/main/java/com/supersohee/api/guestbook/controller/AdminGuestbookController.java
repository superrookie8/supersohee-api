package com.supersohee.api.guestbook.controller;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.guestbook.dto.AdminGuestbookPageResponse;
import com.supersohee.api.guestbook.service.GuestbookPhoto;
import com.supersohee.api.guestbook.service.GuestbookPhotoService;
import com.supersohee.api.guestbook.service.GuestbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/guestbooks")
@RequiredArgsConstructor
public class AdminGuestbookController {
    private static final int MAX_PAGE_SIZE = 100;
    private final GuestbookService guestbookService;
    private final GuestbookPhotoService guestbookPhotoService;

    @GetMapping
    public AdminGuestbookPageResponse getGuestbooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw AdminApiException.badRequest("page must be non-negative and size must be between 1 and 100.");
        }
        return AdminGuestbookPageResponse.from(guestbookService.findAdminGuestbooks(page, size, name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuestbook(@PathVariable String id) {
        guestbookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<org.springframework.core.io.Resource> getGuestbookPhoto(@PathVariable String id) {
        GuestbookPhoto photo = guestbookPhotoService.getPhotoForEntry(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(photo.mediaType())
                .contentLength(photo.contentLength())
                .body(photo.resource());
    }
}
