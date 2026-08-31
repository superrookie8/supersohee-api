package com.supersohee.api.guestbook.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.guestbook.domain.Guestbook;
import com.supersohee.api.guestbook.repository.GuestbookRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuestbookServiceTest {

    @Test
    void nameSearchUsesRepositoryPaginationWithoutMigrationWrites() {
        GuestbookRepository repository = mock(GuestbookRepository.class);
        GuestbookService service = new GuestbookService(repository);
        Guestbook entry = Guestbook.builder().id("guest-1").name("팬").build();
        when(repository.findByNameContainingIgnoreCaseOrderByDateDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry)));

        assertThat(service.findAdminGuestbooks(0, 20, " 팬 ").getContent()).containsExactly(entry);
        verify(repository).findByNameContainingIgnoreCaseOrderByDateDesc(eq("팬"), any(Pageable.class));
    }

    @Test
    void deleteDistinguishesMissingEntry() {
        GuestbookRepository repository = mock(GuestbookRepository.class);
        GuestbookService service = new GuestbookService(repository);
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("missing"))
                .isInstanceOfSatisfying(AdminApiException.class,
                        exception -> assertThat(exception.status().value()).isEqualTo(404));
    }
}
