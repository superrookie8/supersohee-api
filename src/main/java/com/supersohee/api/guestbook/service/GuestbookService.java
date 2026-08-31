package com.supersohee.api.guestbook.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.guestbook.domain.Guestbook;
import com.supersohee.api.guestbook.repository.GuestbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestbookService {
    private final GuestbookRepository guestbookRepository;

    public Page<Guestbook> findAdminGuestbooks(int page, int size, String name) {
        PageRequest pageable = PageRequest.of(page, size);
        if (name == null || name.isBlank()) {
            return guestbookRepository.findAllByOrderByDateDesc(pageable);
        }
        return guestbookRepository.findByNameContainingIgnoreCaseOrderByDateDesc(name.trim(), pageable);
    }

    public void delete(String id) {
        Guestbook guestbook = guestbookRepository.findById(id)
                .orElseThrow(() -> AdminApiException.notFound("Guestbook entry"));
        guestbookRepository.delete(guestbook);
    }
}
