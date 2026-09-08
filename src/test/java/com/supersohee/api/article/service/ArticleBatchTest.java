package com.supersohee.api.article.service;

import com.supersohee.api.admin.error.AdminApiException;
import com.supersohee.api.article.domain.Article;
import com.supersohee.api.article.dto.AdminArticleImportItem;
import com.supersohee.api.article.dto.AdminArticleImportRequest;
import com.supersohee.api.article.repository.ArticleRepository;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonString;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArticleBatchTest {
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final ArticleService service = new ArticleService(mock(ArticleRepository.class), mongo);

    @Test void validatesEntireBatchBeforeAnyWrites() {
        assertThatThrownBy(() -> service.batchArticles(new AdminArticleImportRequest(List.of(
                item("other", "https://news.example.com/1"), item("other", "http://127.0.0.1/")))))
                .isInstanceOf(AdminApiException.class);
        verifyNoInteractions(mongo);
    }

    @Test void unsafeUrlsAndPublisherMismatchFail() {
        for (String url : List.of("javascript:alert(1)", "https://user:pass@news.example.com/", "http://[::1]/",
                "http://2130706433/", "https://service.local/", "https://localhost/", "https://news.example.com:3000/")) {
            assertThatThrownBy(() -> ArticleUrlPolicy.normalize(item("other", url))).isInstanceOf(AdminApiException.class);
        }
        assertThatThrownBy(() -> ArticleUrlPolicy.normalize(item("other", "https://jumpball.co.kr/news/1")))
                .isInstanceOf(AdminApiException.class);
        assertThat(ArticleUrlPolicy.normalize(item("other", "https://jumpball.co.kr.evil.com/1")).source()).isEqualTo("other");
    }

    @Test void legacyPublisherVariantsCountAsExistingWithoutWriting() {
        for (String[] fixture : List.of(
                new String[]{"jumpball", "https://jumpball.co.kr/news/newsview.php?ncode=123", "http://www.jumpball.co.kr/news/newsview.php?utm_source=naver&ncode=123&fbclid=x"},
                new String[]{"rookie", "https://www.rookie.co.kr/news/articleView.html?idxno=123", "http://rookie.co.kr/news/articleView.html?idxno=123&utm_source=naver#top"})) {
            when(mongo.exists(any(Query.class), eq(Article.class))).thenAnswer(invocation -> {
                Query query = invocation.getArgument(0);
                var regex = (java.util.regex.Pattern) query.getQueryObject().get("url");
                return regex.matcher(fixture[2]).matches();
            });
            var result = service.batchArticles(new AdminArticleImportRequest(List.of(item(fixture[0], fixture[1]))));
            assertThat(result.created()).isZero();
            assertThat(result.existing()).isOne();
        }
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq(Article.class));
    }

    @Test void canonicalPreservesIdentityQueryAndCaseSensitivePath() {
        var normalized = ArticleUrlPolicy.normalize(item("rookie", "http://ROOKIE.CO.KR/news/articleView.html?idxno=123&utm_source=n&nclick=abc#top"));
        assertThat(normalized.url()).isEqualTo("https://www.rookie.co.kr/news/articleView.html?idxno=123&nclick=abc");
        assertThat(ArticleUrlPolicy.legacyPattern(normalized.url()).matcher(normalized.url().replace("articleView", "articleview")).matches()).isFalse();
    }

    @Test void repeatCanonicalImportIsInsertOnlyAndRetrySafe() {
        when(mongo.upsert(any(Query.class), any(Update.class), eq(Article.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, new BsonString("fixture")))
                .thenReturn(UpdateResult.acknowledged(1, 0L, null));
        var request = new AdminArticleImportRequest(List.of(item("other", "https://news.example.com/1?utm_source=n#top")));
        assertThat(service.batchArticles(request).created()).isOne();
        assertThat(service.batchArticles(request).existing()).isOne();
        verify(mongo, times(2)).upsert(argThat(q -> q.getQueryObject().getString("url").equals("https://news.example.com/1")),
                argThat(u -> u.getUpdateObject().keySet().equals(java.util.Set.of("$setOnInsert"))), eq(Article.class));
    }

    @Test void bothImportIngressesWriteTheSameAtomicIdentity() {
        when(mongo.upsert(any(Query.class), any(Update.class), eq(Article.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, new BsonString("fixture")))
                .thenReturn(UpdateResult.acknowledged(1, 0L, null));
        var direct = new AdminArticleImportRequest(List.of(item("jumpball", "http://www.jumpball.co.kr/news/1?utm_source=direct")));
        var naver = new AdminArticleImportRequest(List.of(item("jumpball", "https://jumpball.co.kr/news/1#top")));
        assertThat(service.importArticles(direct).created()).isOne();
        assertThat(service.batchArticles(naver).created()).isZero();
        verify(mongo, times(2)).upsert(argThat(q -> q.getQueryObject().getString("url").equals("https://jumpball.co.kr/news/1")),
                any(Update.class), eq(Article.class));
    }

    private AdminArticleImportItem item(String source, String url) {
        return new AdminArticleImportItem(source, "이소희", url, "요약", null, LocalDateTime.of(2026,9,8,12,0));
    }
}
