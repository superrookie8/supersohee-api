package com.supersohee.api.image.controller;

import com.supersohee.api.image.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageUploadService imageUploadService;

    // 단일 이미지 업로드 (R2 키 반환)
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(
            @AuthenticationPrincipal String userId,
            @RequestParam("file") MultipartFile file) {
        System.out.println("=== ImageController.uploadImage ===");
        System.out.println("User ID: " + userId);
        System.out.println("File name: " + (file != null ? file.getOriginalFilename() : "null"));
        System.out.println("File size: " + (file != null ? file.getSize() : "null"));
        
        try {
            String key = imageUploadService.uploadImage(file);
            Map<String, String> response = new HashMap<>();
            response.put("key", key); // R2 키 반환
            System.out.println("Upload successful, key: " + key);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            System.out.println("IllegalArgumentException: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            System.out.println("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "이미지 업로드 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // 다중 이미지 업로드 (R2 키 리스트 반환)
    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadImages(
            @AuthenticationPrincipal String userId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            List<String> keys = imageUploadService.uploadImages(files);
            Map<String, Object> response = new HashMap<>();
            response.put("keys", keys); // R2 키 리스트 반환
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "이미지 업로드 실패: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
