package com.supersohee.api.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest; // 수정
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    @Value("${cloudflare.r2.endpoint}")
    private String r2Endpoint;

    @Value("${cloudflare.r2.access-key-id}")
    private String accessKeyId;

    @Value("${cloudflare.r2.secret-access-key}")
    private String secretAccessKey;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Value("${cloudflare.r2.presigned-url-expiration:31536000}")
    private long presignedUrlExpirationSeconds;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif", "webp");

    private S3Client getS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        return S3Client.builder()
                .endpointOverride(java.net.URI.create(r2Endpoint))
                .region(Region.of("apac")) // 서울 리전 (Cloudflare R2는 region-less이지만 AWS SDK에서 필수)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .httpClient(UrlConnectionHttpClient.builder().build()) // HTTP 클라이언트 명시적 지정
                .build();
    }

    private S3Presigner getS3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        return S3Presigner.builder()
                .endpointOverride(java.net.URI.create(r2Endpoint))
                .region(Region.of("apac")) // 서울 리전 (Cloudflare R2는 region-less이지만 AWS SDK에서 필수)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    // 단일 이미지 업로드 (R2 키 반환)
    public String uploadImage(MultipartFile file) throws IOException {
        validateImage(file);

        String fileName = generateFileName(file.getOriginalFilename());
        String key = "diary/" + fileName;

        S3Client s3Client = getS3Client();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()));

        s3Client.close();

        // R2 키 반환 (서명된 URL이 아님)
        return key;
    }

    // 다중 이미지 업로드 (R2 키 리스트 반환)
    public List<String> uploadImages(List<MultipartFile> files) throws IOException {
        List<String> uploadedKeys = new ArrayList<>();

        for (MultipartFile file : files) {
            String key = uploadImage(file);
            uploadedKeys.add(key);
        }

        return uploadedKeys;
    }

    // 이벤트 이미지 업로드 (event/ 경로로 저장)
    public String uploadEventImage(MultipartFile file) throws IOException {
        validateImage(file);

        String fileName = generateFileName(file.getOriginalFilename());
        String key = "event/" + fileName;

        S3Client s3Client = getS3Client();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()));

        s3Client.close();

        return key;
    }

    // 다중 이벤트 이미지 업로드 (event/ 경로로 저장)
    public List<String> uploadEventImages(List<MultipartFile> files) throws IOException {
        List<String> uploadedKeys = new ArrayList<>();

        for (MultipartFile file : files) {
            String key = uploadEventImage(file);
            uploadedKeys.add(key);
        }

        return uploadedKeys;
    }

    // R2 키에서 서명된 URL 생성 (수정된 부분)
    public String generatePresignedUrl(String key) {
        S3Presigner presigner = getS3Presigner();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(
                presignRequest -> presignRequest
                        .signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
                        .getObjectRequest(getObjectRequest));

        String presignedUrl = presignedRequest.url().toString();

        presigner.close();

        return presignedUrl;
    }

    // 여러 키를 서명된 URL로 변환
    public List<String> convertKeysToPresignedUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> presignedUrls = new ArrayList<>();
        for (String key : keys) {
            presignedUrls.add(generatePresignedUrl(key));
        }
        return presignedUrls;
    }

    // R2에서 이미지 삭제
    public void deleteImage(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("이미지 키가 필요합니다");
        }

        S3Client s3Client = getS3Client();

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
        } finally {
            s3Client.close();
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("이미지 크기는 5MB 이하여야 합니다");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 없습니다");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하는 이미지 형식: jpg, jpeg, png, gif, webp");
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}