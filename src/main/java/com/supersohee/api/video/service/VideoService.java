package com.supersohee.api.video.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.video.domain.Video;
import com.supersohee.api.video.dto.*;
import com.supersohee.api.video.repository.VideoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository repository;
    private static final List<String> CATEGORIES =
            List.of("highlights", "interviews", "behind", "shorts");

    public VideoPageResponse list(
            boolean admin, String category, String sort, int page, int limit) {
        if (page < 0 || limit < 1 || limit > 100)
            throw invalid("page", "page must be non-negative and limit between 1 and 100.");
        if (!"all".equals(category) && !CATEGORIES.contains(category))
            throw invalid("category", "Unsupported category.");
        if (!Set.of("recent", "focused").contains(sort)) throw invalid("sort", "Unsupported sort.");
        Comparator<Video> order =
                Comparator.comparing(
                                Video::eventDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparingInt(v -> CATEGORIES.indexOf(v.category()))
                        .thenComparing(Video::displayOrder)
                        .thenComparing(Video::id);
        if ("focused".equals(sort))
            order =
                    Comparator.comparing(Video::leeFocused, Comparator.reverseOrder())
                            .thenComparing(order);
        var rows =
                (admin ? repository.findAll() : repository.findByPublishedTrue())
                        .stream()
                                .filter(
                                        v ->
                                                "all".equals(category)
                                                        || category.equals(v.category()))
                                .sorted(order)
                                .toList();
        long offset = (long) page * limit;
        int total = rows.size();
        var items = rows.stream().skip(offset).limit(limit).map(VideoResponse::from).toList();
        return new VideoPageResponse(
                items,
                total,
                page,
                limit,
                (int) ((total + (long) limit - 1) / limit),
                offset + limit < total,
                page > 0);
    }

    public VideoResponse create(VideoRequest r) {
        Video v = normalize(r, null, null);
        try {
            return VideoResponse.from(repository.insert(v));
        } catch (DuplicateKeyException e) {
            throw AdminApiException.conflict("Video already exists.");
        }
    }

    public VideoResponse update(String id, VideoRequest r) {
        validateId(id);
        Video old = repository.findById(id).orElseThrow(() -> AdminApiException.notFound("Video"));
        return VideoResponse.from(repository.save(normalize(r, id, old.createdAt())));
    }

    public void delete(String id) {
        validateId(id);
        if (!repository.existsById(id)) throw AdminApiException.notFound("Video");
        repository.deleteById(id);
    }

    public VideoImportResponse importVideos(VideoImportRequest r) {
        if (r == null || r.videos() == null || r.videos().isEmpty() || r.videos().size() > 100)
            throw invalid("videos", "Provide 1 to 100 videos.");
        var rows = r.videos().stream().map(v -> normalize(v, null, null)).toList();
        var ids = new HashSet<String>();
        for (var v : rows)
            if (!ids.add(v.id())) throw invalid("videos", "Duplicate IDs in request.");
        int created = 0, existing = 0;
        for (var v : rows) {
            try {
                repository.insert(v);
                created++;
            } catch (DuplicateKeyException e) {
                if (repository.existsById(v.id())) existing++;
                else throw e;
            }
        }
        return new VideoImportResponse(rows.size(), created, existing);
    }

    private Video normalize(VideoRequest r, String pathId, LocalDateTime created) {
        if (r == null) throw invalid("video", "Video is required.");
        String id = r.id() == null ? pathId : r.id();
        validateId(id);
        if (pathId != null && !pathId.equals(id)) throw invalid("id", "Video ID cannot change.");
        String title = text(r.title(), "title", 300, true),
                channel = text(r.channelName(), "channelName", 100, true);
        if (r.category() == null || !CATEGORIES.contains(r.category()))
            throw invalid("category", "Unsupported category.");
        String key = text(r.eventKey(), "eventKey", 100, false);
        if (key != null && !key.matches("[a-z0-9]+(?:-[a-z0-9]+)*"))
            throw invalid("eventKey", "Use a lowercase slug.");
        String label = text(r.eventLabel(), "eventLabel", 120, true),
                basis = text(r.eventDateBasis(), "eventDateBasis", 300, false);
        if (r.leeFocused() == null || r.published() == null)
            throw invalid("published", "published and leeFocused are required booleans.");
        if (r.displayOrder() == null || r.displayOrder() < 0)
            throw invalid("displayOrder", "Non-negative displayOrder is required.");
        String duration = text(r.duration(), "duration", 30, false);
        if (duration != null
                && !duration.matches("(?:[0-9]+:[0-5][0-9]|[0-9]+:[0-5][0-9]:[0-5][0-9])"))
            throw invalid("duration", "Use M:SS or H:MM:SS.");
        String thumb = text(r.thumbnailUrl(), "thumbnailUrl", 2048, false);
        if (thumb == null) thumb = "https://i.ytimg.com/vi/" + id + "/hqdefault.jpg";
        try {
            URI u = URI.create(thumb);
            if (!"https".equals(u.getScheme())
                    || !"i.ytimg.com".equals(u.getHost())
                    || u.getPort() != -1
                    || u.getRawUserInfo() != null
                    || u.getRawQuery() != null
                    || u.getRawFragment() != null
                    || !u.getRawPath()
                            .matches(
                                    "/(?:vi/"
                                            + id
                                            + "/[A-Za-z0-9_-]+\\.jpg|vi_webp/"
                                            + id
                                            + "/[A-Za-z0-9_-]+\\.webp)"))
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw invalid("thumbnailUrl", "Use an allowed YouTube thumbnail for this video ID.");
        }
        var now = LocalDateTime.now();
        return new Video(
                id,
                title,
                channel,
                r.category(),
                r.eventDate(),
                key,
                label,
                basis,
                r.leeFocused(),
                r.displayOrder(),
                r.published(),
                duration,
                text(r.badge(), "badge", 30, false),
                r.verifiedOn(),
                thumb,
                created == null ? now : created,
                now);
    }

    private static void validateId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{11}"))
            throw invalid("id", "A valid YouTube video ID is required.");
    }

    private static String text(String s, String field, int max, boolean required) {
        String v = s == null ? null : s.trim();
        if (v != null && v.isEmpty()) v = null;
        if (required && v == null
                || v != null && (v.length() > max || v.chars().anyMatch(Character::isISOControl)))
            throw invalid(field, "Invalid " + field + ".");
        return v;
    }

    private static AdminApiException invalid(String field, String message) {
        return AdminApiException.validation(message, Map.of(field, message));
    }
}
