package com.supersohee.api.video;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.video.domain.Video;
import com.supersohee.api.video.dto.*;
import com.supersohee.api.video.repository.VideoRepository;
import com.supersohee.api.video.service.VideoService;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.*;

class VideoServiceTest {
    private final VideoRepository repository = mock(VideoRepository.class);
    private final VideoService service = new VideoService(repository);

    static VideoRequest request(
            String id, String category, LocalDate date, boolean focused, int order, String thumb) {
        return new VideoRequest(
                id,
                " Title ",
                " Channel ",
                category,
                date,
                "final",
                "결승",
                null,
                focused,
                order,
                true,
                "1:23",
                null,
                null,
                thumb);
    }

    private Video video(String id, String category, LocalDate date, boolean focused, int order) {
        var r = request(id, category, date, focused, order, null);
        return new Video(
                r.id(),
                r.title(),
                r.channelName(),
                r.category(),
                r.eventDate(),
                r.eventKey(),
                r.eventLabel(),
                r.eventDateBasis(),
                focused,
                order,
                true,
                r.duration(),
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void filtersAndSortsBeforeSlicingIncludingUnknownDateAndFocusedPriority() {
        var date = LocalDate.of(2026, 9, 26);
        var rows =
                List.of(
                        video("aaaaaaaaaaa", "shorts", date, true, 0),
                        video("bbbbbbbbbbb", "highlights", date, false, 1),
                        video("ccccccccccc", "highlights", date, false, 0),
                        video("ddddddddddd", "highlights", null, true, 0));
        when(repository.findByPublishedTrue()).thenReturn(rows);
        assertThat(service.list(false, "all", "recent", 0, 2).videos())
                .extracting(VideoResponse::id)
                .containsExactly("ccccccccccc", "bbbbbbbbbbb");
        var second = service.list(false, "all", "recent", 1, 2);
        assertThat(second.videos())
                .extracting(VideoResponse::id)
                .containsExactly("aaaaaaaaaaa", "ddddddddddd");
        assertThat(second.hasNext()).isFalse();
        assertThat(second.hasPrevious()).isTrue();
        assertThat(second.total()).isEqualTo(4);
        assertThat(service.list(false, "all", "focused", 0, 100).videos())
                .extracting(VideoResponse::id)
                .containsExactly("aaaaaaaaaaa", "ddddddddddd", "ccccccccccc", "bbbbbbbbbbb");
        assertThat(service.list(false, "shorts", "recent", 0, 12).total()).isOne();
        assertThat(service.list(false, "all", "recent", Integer.MAX_VALUE, 100).videos()).isEmpty();
        verify(repository, never()).findAll();
        when(repository.findAll()).thenReturn(rows);
        assertThat(service.list(true, "all", "recent", 0, 12).total()).isEqualTo(4);
    }

    @Test
    void tiesUseIdAndListRejectsBadFiltersBeforeRead() {
        var date = LocalDate.of(2026, 9, 26);
        when(repository.findByPublishedTrue())
                .thenReturn(
                        List.of(
                                video("bbbbbbbbbbb", "highlights", date, false, 0),
                                video("aaaaaaaaaaa", "highlights", date, false, 0)));
        assertThat(service.list(false, "all", "recent", 0, 12).videos())
                .extracting(VideoResponse::id)
                .containsExactly("aaaaaaaaaaa", "bbbbbbbbbbb");
        clearInvocations(repository);
        assertThatThrownBy(() -> service.list(false, "bad", "recent", 0, 12))
                .isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.list(false, "all", "bad", 0, 12))
                .isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.list(false, "all", "recent", -1, 12))
                .isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.list(false, "all", "recent", 0, 101))
                .isInstanceOf(AdminApiException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void normalizesMetadataAndDerivesSafeLinksWithoutNetwork() {
        when(repository.insert(any(Video.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.create(request("aaaaaaaaaaa", "highlights", null, false, 0, null));
        assertThat(result.title()).isEqualTo("Title");
        assertThat(result.url()).isEqualTo("https://www.youtube.com/watch?v=aaaaaaaaaaa");
        assertThat(result.thumbnailUrl())
                .isEqualTo("https://i.ytimg.com/vi/aaaaaaaaaaa/hqdefault.jpg");
        assertThat(result.createdAt()).isNotNull();
        assertThat(
                        service.create(
                                        request(
                                                "aaaaaaaaaaa",
                                                "shorts",
                                                null,
                                                false,
                                                0,
                                                "https://i.ytimg.com/vi_webp/aaaaaaaaaaa/hqdefault.webp"))
                                .thumbnailUrl())
                .endsWith(".webp");
    }

    @Test
    void rejectsUnsafeThumbnailsAndMismatchedIdsBeforeWrites() {
        for (var thumb :
                List.of(
                        "http://i.ytimg.com/vi/aaaaaaaaaaa/x.jpg",
                        "https://i.ytimg.com.evil.com/vi/aaaaaaaaaaa/x.jpg",
                        "https://user@i.ytimg.com/vi/aaaaaaaaaaa/x.jpg",
                        "https://i.ytimg.com:443/vi/aaaaaaaaaaa/x.jpg",
                        "https://i.ytimg.com/vi/bbbbbbbbbbb/x.jpg",
                        "https://i.ytimg.com/vi/aaaaaaaaaaa/x.jpg?q=1",
                        "https://i.ytimg.com/vi/aaaaaaaaaaa/x.jpg#x",
                        "https://i.ytimg.com/vi/aaaaaaaaaaa/../x.jpg",
                        "https://i.ytimg.com/vi/aaaaaaaaaaa/%2e%2e.jpg",
                        "https://127.0.0.1/vi/aaaaaaaaaaa/x.jpg")) {
            assertThatThrownBy(
                            () ->
                                    service.create(
                                            request(
                                                    "aaaaaaaaaaa",
                                                    "shorts",
                                                    null,
                                                    false,
                                                    0,
                                                    thumb)))
                    .isInstanceOf(AdminApiException.class);
        }
        assertThatThrownBy(() -> service.create(request("bad", "shorts", null, false, 0, null)))
                .isInstanceOf(AdminApiException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void validatesEntireImportAndDuplicateInputBeforeAnyWrite() {
        var valid = request("aaaaaaaaaaa", "shorts", null, false, 0, null);
        assertThatThrownBy(
                        () ->
                                service.importVideos(
                                        new VideoImportRequest(
                                                List.of(
                                                        valid,
                                                        request(
                                                                "bad", "shorts", null, false, 0,
                                                                null)))))
                .isInstanceOf(AdminApiException.class);
        assertThatThrownBy(
                        () -> service.importVideos(new VideoImportRequest(List.of(valid, valid))))
                .isInstanceOf(AdminApiException.class);
        assertThatThrownBy(
                        () ->
                                service.importVideos(
                                        new VideoImportRequest(Collections.nCopies(101, valid))))
                .isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.importVideos(new VideoImportRequest(List.of())))
                .isInstanceOf(AdminApiException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void insertOnlyRetriesPreserveExistingMetadataAndReportPartialFailures() {
        var first = request("aaaaaaaaaaa", "shorts", null, false, 0, null);
        var second = request("bbbbbbbbbbb", "shorts", null, false, 0, null);
        when(repository.insert(any(Video.class)))
                .thenAnswer(
                        i -> {
                            Video v = i.getArgument(0);
                            if (v.id().equals(first.id()))
                                throw new DuplicateKeyException("fixture");
                            return v;
                        });
        when(repository.existsById(first.id())).thenReturn(true);
        assertThat(service.importVideos(new VideoImportRequest(List.of(first, second))))
                .isEqualTo(new VideoImportResponse(2, 1, 1));
        verify(repository, never()).save(any(Video.class));
        when(repository.insert(any(Video.class)))
                .thenThrow(new IllegalStateException("fixture storage failure"));
        assertThatThrownBy(() -> service.importVideos(new VideoImportRequest(List.of(second))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateKeepsIdentityAndCreatedTimeAndDeleteChecksExistence() {
        var original = video("aaaaaaaaaaa", "shorts", null, false, 0);
        when(repository.findById(original.id())).thenReturn(Optional.of(original));
        when(repository.save(any(Video.class))).thenAnswer(i -> i.getArgument(0));
        var input = request(null, "highlights", null, true, 2, null);
        assertThat(service.update(original.id(), input).id()).isEqualTo(original.id());
        assertThatThrownBy(
                        () ->
                                service.update(
                                        original.id(),
                                        request("bbbbbbbbbbb", "shorts", null, false, 0, null)))
                .isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.update("bbbbbbbbbbb", input))
                .isInstanceOf(AdminApiException.class);
        assertThatThrownBy(() -> service.delete(original.id()))
                .isInstanceOf(AdminApiException.class);
        when(repository.existsById(original.id())).thenReturn(true);
        service.delete(original.id());
        verify(repository).deleteById(original.id());
    }
}
