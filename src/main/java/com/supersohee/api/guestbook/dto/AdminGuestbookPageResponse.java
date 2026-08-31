package com.supersohee.api.guestbook.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminGuestbookPageResponse(
        List<AdminGuestbookResponse> content,
        long totalElements,
        int page,
        int size,
        int totalPages,
        boolean hasNext) {
    public static AdminGuestbookPageResponse from(Page<com.supersohee.api.guestbook.domain.Guestbook> result) {
        return new AdminGuestbookPageResponse(
                result.getContent().stream().map(AdminGuestbookResponse::from).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(),
                result.getTotalPages(), result.hasNext());
    }
}
