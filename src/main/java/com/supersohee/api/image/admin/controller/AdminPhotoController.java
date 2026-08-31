package com.supersohee.api.image.admin.controller;

import com.supersohee.api.image.admin.dto.AdminPhotoDeleteRequest;
import com.supersohee.api.image.admin.dto.AdminPhotoDeleteResponse;
import com.supersohee.api.image.admin.dto.AdminPhotoListResponse;
import com.supersohee.api.image.admin.dto.AdminPhotoUploadResponse;
import com.supersohee.api.image.admin.service.AdminPhotoContent;
import com.supersohee.api.image.admin.service.AdminPhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/photos")
@RequiredArgsConstructor
public class AdminPhotoController {
    private final AdminPhotoService adminPhotoService;

    @GetMapping
    public AdminPhotoListResponse getPhotos() {
        return adminPhotoService.findAll();
    }

    @PostMapping
    public ResponseEntity<AdminPhotoUploadResponse> uploadPhotos(
            @RequestParam("photos") List<MultipartFile> photos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminPhotoService.upload(photos));
    }

    @DeleteMapping
    public ResponseEntity<AdminPhotoDeleteResponse> deletePhotos(
            @Valid @RequestBody AdminPhotoDeleteRequest request) {
        AdminPhotoDeleteResponse response = adminPhotoService.delete(request.photoIds());
        HttpStatus status = response.complete() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> getPhotoContent(@PathVariable String id) {
        AdminPhotoContent content = adminPhotoService.getContent(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(content.mediaType())
                .contentLength(content.contentLength())
                .body(content.resource());
    }
}
