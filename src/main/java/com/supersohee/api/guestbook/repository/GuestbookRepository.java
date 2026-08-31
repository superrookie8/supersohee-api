package com.supersohee.api.guestbook.repository;

import com.supersohee.api.guestbook.domain.Guestbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GuestbookRepository extends MongoRepository<Guestbook, String> {
    Page<Guestbook> findAllByOrderByDateDesc(Pageable pageable);
    Page<Guestbook> findByNameContainingIgnoreCaseOrderByDateDesc(String name, Pageable pageable);
}
