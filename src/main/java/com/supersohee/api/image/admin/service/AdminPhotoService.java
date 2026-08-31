package com.supersohee.api.image.admin.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.image.admin.domain.AdminPhotoSource;
import com.supersohee.api.image.admin.domain.StoredAdminPhoto;
import com.supersohee.api.image.admin.dto.*;
import com.supersohee.api.image.admin.repository.AdminPhotoStore;
import com.supersohee.api.image.validation.ImageContentSignatures;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPhotoService {
    static final int MAX_PHOTO_COUNT = 20;
    static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> EXTENSION_CONTENT_TYPES = Map.of(
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE,
            "png", MediaType.IMAGE_PNG_VALUE,
            "gif", MediaType.IMAGE_GIF_VALUE,
            "webp", "image/webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.copyOf(EXTENSION_CONTENT_TYPES.values());

    private final AdminPhotoStore photoStore;

    public AdminPhotoListResponse findAll() {
        return new AdminPhotoListResponse(
                toItems(photoStore.findAll(AdminPhotoSource.ADMIN)),
                toItems(photoStore.findAll(AdminPhotoSource.USER)));
    }

    public AdminPhotoUploadResponse upload(List<MultipartFile> files) {
        validateBatch(files);
        List<StoredAdminPhoto> uploaded = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                uploaded.add(photoStore.storeAdminPhoto(file));
            }
        } catch (IOException | RuntimeException uploadFailure) {
            boolean rollbackFailed = rollback(uploaded);
            String message = rollbackFailed
                    ? "Photo upload failed and storage rollback was incomplete."
                    : "Photo upload failed.";
            throw AdminApiException.storageFailure(message);
        }
        return new AdminPhotoUploadResponse("Photos uploaded successfully.", toItems(uploaded));
    }

    public AdminPhotoDeleteResponse delete(List<String> requestedIds) {
        LinkedHashSet<String> ids = new LinkedHashSet<>(requestedIds);
        Map<String, StoredAdminPhoto> resolved = new LinkedHashMap<>();
        for (String id : ids) {
            validateObjectId(id);
            StoredAdminPhoto photo = photoStore.findByIdAdminFirst(id)
                    .orElseThrow(() -> AdminApiException.notFound("Photo"));
            resolved.put(id, photo);
        }

        List<String> deleted = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, StoredAdminPhoto> entry : resolved.entrySet()) {
            try {
                photoStore.delete(entry.getValue());
                deleted.add(entry.getKey());
            } catch (RuntimeException storageFailure) {
                failed.add(entry.getKey());
            }
        }
        String message = failed.isEmpty()
                ? "Photos deleted successfully."
                : "Some photos could not be deleted.";
        return new AdminPhotoDeleteResponse(message, List.copyOf(deleted), List.copyOf(failed));
    }

    public AdminPhotoContent getContent(String id) {
        validateObjectId(id);
        StoredAdminPhoto photo = photoStore.findByIdAdminFirst(id)
                .orElseThrow(() -> AdminApiException.notFound("Photo"));
        if (photo.resource() == null) {
            throw AdminApiException.notFound("Photo content");
        }
        if (photo.size() < 0 || photo.size() > MAX_PHOTO_BYTES) {
            throw AdminApiException.payloadTooLarge("Photo exceeds the 5 MiB limit.");
        }
        MediaType mediaType = parseAllowedContentType(photo.contentType());
        return new AdminPhotoContent(photo.resource(), mediaType, photo.size());
    }

    private void validateBatch(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > MAX_PHOTO_COUNT) {
            throw AdminApiException.badRequest("photos must contain between 1 and 20 files.");
        }
        for (MultipartFile file : files) {
            validateFile(file);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw AdminApiException.badRequest("Each photo must contain data.");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw AdminApiException.payloadTooLarge("Each photo must be at most 5 MiB.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()
                || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw AdminApiException.badRequest("Photo filename is invalid.");
        }
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        String expectedContentType = EXTENSION_CONTENT_TYPES.get(extension);
        String actualContentType = normalizeContentType(file.getContentType());
        if (expectedContentType == null || !expectedContentType.equals(actualContentType)) {
            throw AdminApiException.unsupportedMediaType("Photo extension and content type must match.");
        }
        if (!hasExpectedSignature(file, actualContentType)) {
            throw AdminApiException.unsupportedMediaType("Photo content does not match its declared type.");
        }
    }

    private boolean hasExpectedSignature(MultipartFile file, String contentType) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            return switch (contentType) {
                case MediaType.IMAGE_JPEG_VALUE -> startsWith(header, 0xFF, 0xD8, 0xFF);
                case MediaType.IMAGE_PNG_VALUE -> startsWith(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
                case MediaType.IMAGE_GIF_VALUE -> asciiStartsWith(header, "GIF87a") || asciiStartsWith(header, "GIF89a");
                case "image/webp" -> ImageContentSignatures.isWebp(header);
                default -> false;
            };
        } catch (IOException exception) {
            throw AdminApiException.badRequest("Photo could not be read.");
        }
    }

    private String normalizeContentType(String contentType) {
        try {
            MediaType parsed = MediaType.parseMediaType(contentType == null ? "" : contentType);
            return parsed.getType().toLowerCase(Locale.ROOT) + "/" + parsed.getSubtype().toLowerCase(Locale.ROOT);
        } catch (InvalidMediaTypeException exception) {
            return "";
        }
    }

    private MediaType parseAllowedContentType(String contentType) {
        String normalized = normalizeContentType(contentType);
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw AdminApiException.unsupportedMediaType("Photo content type is not supported.");
        }
        return MediaType.parseMediaType(normalized);
    }

    private void validateObjectId(String id) {
        if (id == null || !ObjectId.isValid(id)) {
            throw AdminApiException.badRequest("Photo id is invalid.");
        }
    }

    private boolean rollback(List<StoredAdminPhoto> uploaded) {
        boolean failed = false;
        for (int index = uploaded.size() - 1; index >= 0; index--) {
            try {
                photoStore.delete(uploaded.get(index));
            } catch (RuntimeException rollbackFailure) {
                failed = true;
            }
        }
        return failed;
    }

    private List<AdminPhotoItemResponse> toItems(List<StoredAdminPhoto> photos) {
        return photos.stream().map(AdminPhotoItemResponse::from).toList();
    }

    private boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) {
            if ((bytes[index] & 0xFF) != prefix[index]) return false;
        }
        return true;
    }

    private boolean asciiStartsWith(byte[] bytes, String prefix) {
        return asciiAt(bytes, 0, prefix);
    }

    private boolean asciiAt(byte[] bytes, int offset, String expected) {
        if (bytes.length < offset + expected.length()) return false;
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != expected.charAt(index)) return false;
        }
        return true;
    }
}
