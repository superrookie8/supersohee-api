package com.supersohee.api.event;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.event.domain.Event;
import com.supersohee.api.event.domain.EventDisplayOrder;
import com.supersohee.api.event.repository.EventDisplayOrderRepository;
import com.supersohee.api.event.repository.EventRepository;
import com.supersohee.api.event.service.EventService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventServiceOrderTest {
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventDisplayOrderRepository orderRepository = mock(EventDisplayOrderRepository.class);
    private final EventService service = new EventService(eventRepository, orderRepository);

    @Test
    void legacyAdminAndPublicListsUseTheSameStableCreatedAtFallback() {
        Event oldEvent = event("old", LocalDateTime.of(2026, 1, 1, 12, 0), true);
        Event newEvent = event("new", LocalDateTime.of(2026, 2, 1, 12, 0), true);
        when(orderRepository.findById(EventDisplayOrder.GLOBAL_ID)).thenReturn(Optional.empty());
        when(eventRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(oldEvent, newEvent));
        when(eventRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(oldEvent, newEvent));

        assertThat(service.findAllEvents()).extracting(Event::getId).containsExactly("new", "old");
        assertThat(service.findActiveEvents()).extracting(Event::getId).containsExactly("new", "old");
    }

    @Test
    void persistedOrderIsSharedAndNewUnlistedEventsAppearAtTheTop() {
        Event first = event("first", LocalDateTime.of(2026, 1, 1, 12, 0), true);
        Event second = event("second", LocalDateTime.of(2026, 1, 2, 12, 0), false);
        Event newest = event("newest", LocalDateTime.of(2026, 2, 1, 12, 0), true);
        when(orderRepository.findById(EventDisplayOrder.GLOBAL_ID))
                .thenReturn(Optional.of(new EventDisplayOrder(
                        EventDisplayOrder.GLOBAL_ID, List.of("second", "first"))));
        when(eventRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(first, newest, second));
        when(eventRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(first, newest));

        assertThat(service.findAllEvents()).extracting(Event::getId)
                .containsExactly("newest", "second", "first");
        assertThat(service.findActiveEvents()).extracting(Event::getId)
                .containsExactly("newest", "first");
    }

    @Test
    void completeReorderIsPersistedAsOneAtomicLastWriteWinsDocument() {
        when(eventRepository.findAll()).thenReturn(List.of(
                event("first", null, true), event("second", null, true)));
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(EventDisplayOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.reorderEvents(List.of("second", "first"));

        assertThat(response.eventIds()).containsExactly("second", "first");
        ArgumentCaptor<EventDisplayOrder> captor = ArgumentCaptor.forClass(EventDisplayOrder.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(EventDisplayOrder.GLOBAL_ID);
        assertThat(captor.getValue().getEventIds()).containsExactly("second", "first");
    }

    @Test
    void duplicateUnknownAndMissingIdsNeverPersistAnOrder() {
        when(eventRepository.findAll()).thenReturn(List.of(
                event("first", null, true), event("second", null, true)));

        assertAdminStatus(HttpStatus.BAD_REQUEST,
                () -> service.reorderEvents(List.of("first", "first")),
                "eventIds must not contain duplicates.");
        assertAdminStatus(HttpStatus.CONFLICT,
                () -> service.reorderEvents(List.of("first", "unknown")),
                "eventIds must contain every current event id exactly once.");
        assertAdminStatus(HttpStatus.CONFLICT,
                () -> service.reorderEvents(List.of("first")),
                "eventIds must contain every current event id exactly once.");

        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void orderStorageFailureIsNeverReportedAsSuccess() {
        when(eventRepository.findAll()).thenReturn(List.of(event("first", null, true)));
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(EventDisplayOrder.class)))
                .thenThrow(new IllegalStateException("storage unavailable"));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> service.reorderEvents(List.of("first")));
    }

    @Test
    void editReplacesMetadataAppendsPhotosAndLeavesDisplayOrderUntouched() {
        Event existing = Event.builder()
                .id("event-1")
                .title("Old title")
                .url("https://old.example")
                .description("Old description")
                .check1("Old check")
                .photoKeys(List.of("event/existing.webp"))
                .isActive(true)
                .build();
        existing.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(existing));
        when(eventRepository.save(org.mockito.ArgumentMatchers.any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = service.updateEvent(
                "event-1",
                "Updated title",
                null,
                null,
                null,
                null,
                null,
                List.of("event/new-1.webp", "event/new-2.webp"),
                false);

        assertThat(updated.getTitle()).isEqualTo("Updated title");
        assertThat(updated.getUrl()).isNull();
        assertThat(updated.getDescription()).isNull();
        assertThat(updated.getCheck1()).isNull();
        assertThat(updated.getCheck2()).isNull();
        assertThat(updated.getCheck3()).isNull();
        assertThat(updated.getPhotoKeys()).containsExactly(
                "event/existing.webp", "event/new-1.webp", "event/new-2.webp");
        assertThat(updated.getIsActive()).isFalse();
        assertThat(updated.getCreatedAt()).isEqualTo(existing.getCreatedAt());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void metadataOnlyEditPreservesExistingPhotosAndMissingEventIsNotFound() {
        Event existing = Event.builder()
                .id("event-1")
                .title("Old")
                .photoKeys(List.of("event/existing.webp"))
                .isActive(true)
                .build();
        when(eventRepository.findById("event-1")).thenReturn(Optional.of(existing));
        when(eventRepository.findById("missing")).thenReturn(Optional.empty());
        when(eventRepository.save(org.mockito.ArgumentMatchers.any(Event.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = service.updateEvent(
                "event-1", "Updated", null, null, null, null, null, List.of(), true);

        assertThat(updated.getPhotoKeys()).containsExactly("event/existing.webp");
        assertThatExceptionOfType(AdminApiException.class)
                .isThrownBy(() -> service.updateEvent(
                        "missing", "Updated", null, null, null, null, null, List.of(), true))
                .satisfies(exception -> assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(orderRepository);
    }

    private Event event(String id, LocalDateTime createdAt, boolean active) {
        Event event = Event.builder().id(id).title(id).isActive(active).build();
        event.setCreatedAt(createdAt);
        return event;
    }

    private void assertAdminStatus(
            HttpStatus status,
            Runnable operation,
            String fieldMessage) {
        assertThatExceptionOfType(AdminApiException.class)
                .isThrownBy(operation::run)
                .satisfies(exception -> {
                    assertThat(exception.status()).isEqualTo(status);
                    assertThat(exception.fieldErrors()).containsEntry("eventIds", fieldMessage);
                });
    }
}
