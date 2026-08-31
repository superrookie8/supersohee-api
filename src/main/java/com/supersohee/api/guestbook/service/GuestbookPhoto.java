package com.supersohee.api.guestbook.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record GuestbookPhoto(Resource resource, MediaType mediaType, long contentLength) {
}
