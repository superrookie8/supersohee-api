package com.supersohee.api.image.admin.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record AdminPhotoContent(Resource resource, MediaType mediaType, long contentLength) {
}
