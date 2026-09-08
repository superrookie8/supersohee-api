package com.supersohee.api.article.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.article.dto.AdminArticleImportItem;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Pure syntax validation: never resolves DNS or fetches a submitted URL. */
final class ArticleUrlPolicy {
    private ArticleUrlPolicy() {}

    static AdminArticleImportItem normalize(AdminArticleImportItem item) {
        String url = canonical(item.url(), "url");
        String host = URI.create(url).getHost();
        String source = host.equals("jumpball.co.kr") ? "jumpball"
                : host.equals("www.rookie.co.kr") ? "rookie" : "other";
        if (!source.equals(item.source())) throw invalid("source", "Source must match the original publisher URL.");
        String image = item.imageUrl() == null || item.imageUrl().isBlank() ? null : canonical(item.imageUrl(), "imageUrl");
        return new AdminArticleImportItem(source, item.title().trim(), url, item.summary(), image, item.publishedAt());
    }

    static String canonical(String raw, String field) {
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("https") || scheme.equals("http")) || uri.getRawUserInfo() != null
                    || uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443
                    || !host.matches("(?=.{1,253}$)[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\\.[a-z]{2,63}")
                    || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")
                    || host.endsWith(".test") || host.endsWith(".invalid") || host.contains("..")) {
                throw new IllegalArgumentException();
            }
            boolean known = host.equals("jumpball.co.kr") || host.equals("www.jumpball.co.kr")
                    || host.equals("rookie.co.kr") || host.equals("www.rookie.co.kr");
            if (known) {
                scheme = "https";
                host = host.contains("jumpball") ? "jumpball.co.kr" : "www.rookie.co.kr";
            }
            String query = uri.getRawQuery();
            if (query != null) query = Arrays.stream(query.split("&"))
                    .filter(part -> !tracking(part.split("=", 2)[0]))
                    .collect(Collectors.joining("&"));
            String port = known || uri.getPort() == -1 || scheme.equals("https") && uri.getPort() == 443
                    || scheme.equals("http") && uri.getPort() == 80 ? "" : ":" + uri.getPort();
            return scheme + "://" + host + port + (uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                    + (query == null || query.isEmpty() ? "" : "?" + query);
        } catch (IllegalArgumentException ex) {
            throw invalid(field, "A public HTTP(S) URL without credentials is required.");
        }
    }

    private static boolean tracking(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.startsWith("utm_") || lower.equals("fbclid") || lower.equals("gclid");
    }

    /** Recognize legacy HTTP/www and tracking variants without changing stored rows. */
    static Pattern legacyPattern(String canonical) {
        URI uri = URI.create(canonical);
        String host = uri.getHost();
        boolean known = host.equals("jumpball.co.kr") || host.equals("www.rookie.co.kr");
        String authority = known ? "(?:www\\.)?" + Pattern.quote(host.replaceFirst("^www\\.", ""))
                : Pattern.quote(uri.getRawAuthority());
        String prefix = known ? "https?" : Pattern.quote(uri.getScheme());
        String tracking = "(?:utm_[^=&#]+|fbclid|gclid)=[^&#]*";
        String query = uri.getRawQuery();
        String suffix;
        if (query == null) suffix = "(?:\\?" + tracking + "(?:&" + tracking + ")*)?";
        else suffix = "\\?(?:" + tracking + "&)*" + Arrays.stream(query.split("&"))
                .map(Pattern::quote).collect(Collectors.joining("&(?:" + tracking + "&)*"))
                + "(?:&" + tracking + ")*";
        return Pattern.compile("^(?i:" + prefix + "://" + authority + ")" + Pattern.quote(uri.getRawPath()) + suffix + "(?:#.*)?$");
    }

    private static AdminApiException invalid(String field, String message) {
        return AdminApiException.unprocessable(message, Map.of(field, message));
    }
}
