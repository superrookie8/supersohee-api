package com.supersohee.api.article.config;

import com.supersohee.api.article.domain.Article;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures only Article's idempotency index. Global Mongo auto-index creation
 * remains disabled so unrelated document indexes are not changed implicitly.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.article-index",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ArticleIndexInitializer implements ApplicationRunner {
    static final String INDEX_NAME = "article_source_url_unique";
    private static final String SAFE_FAILURE_MESSAGE =
            "Required article unique index could not be ensured.";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            IndexDefinition expected = resolveExpectedIndex();
            IndexOperations operations = mongoTemplate.indexOps(Article.class);
            operations.createIndex(expected);
            verifyIndex(operations);
        } catch (RuntimeException failure) {
            // Do not attach the Mongo exception: duplicate-key diagnostics can
            // contain article field values. Startup must still fail closed.
            throw new IllegalStateException(SAFE_FAILURE_MESSAGE);
        }
    }

    IndexDefinition resolveExpectedIndex() {
        MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(
                mongoTemplate.getConverter().getMappingContext());
        for (IndexDefinition definition : resolver.resolveIndexFor(Article.class)) {
            if (INDEX_NAME.equals(definition.getIndexOptions().getString("name"))) {
                return definition;
            }
        }
        throw new IllegalStateException(SAFE_FAILURE_MESSAGE);
    }

    private void verifyIndex(IndexOperations operations) {
        IndexInfo actual = operations.getIndexInfo().stream()
                .filter(index -> INDEX_NAME.equals(index.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(SAFE_FAILURE_MESSAGE));

        if (!actual.isUnique()
                || !actual.isIndexForFields(List.of("source", "url"))
                || actual.getPartialFilterExpression() == null) {
            throw new IllegalStateException(SAFE_FAILURE_MESSAGE);
        }
    }
}
