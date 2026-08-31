package com.supersohee.api.user.config;

import com.supersohee.api.user.domain.User;
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
 * Ensures only User's social-identity unique index. Global Mongo auto-index
 * creation stays disabled, so the {@code @CompoundIndex} declared on
 * {@link User} would otherwise never reach the database and
 * {@code UserService.findOrCreateUser}'s DuplicateKeyException guard would
 * never fire — letting one Google subject create several accounts.
 *
 * The declared partial filter covers social accounts only, so legacy
 * email/password documents without a provider stay untouched.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.user-index",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class UserIndexInitializer implements ApplicationRunner {
    static final String PROVIDER_INDEX_NAME = "provider_subject_unique";
    static final String NICKNAME_INDEX_NAME = "nickname_unique";
    private static final String SAFE_FAILURE_MESSAGE =
            "Required user unique index could not be ensured.";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            IndexOperations operations = mongoTemplate.indexOps(User.class);
            for (String name : List.of(PROVIDER_INDEX_NAME, NICKNAME_INDEX_NAME)) {
                operations.createIndex(resolveExpectedIndex(name));
                verifyIndex(operations, name);
            }
        } catch (RuntimeException failure) {
            // Do not attach the Mongo exception: duplicate-key diagnostics can
            // contain provider subjects and email addresses. Fail closed.
            throw new IllegalStateException(SAFE_FAILURE_MESSAGE);
        }
    }

    IndexDefinition resolveExpectedIndex(String name) {
        MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(
                mongoTemplate.getConverter().getMappingContext());
        for (IndexDefinition definition : resolver.resolveIndexFor(User.class)) {
            if (name.equals(definition.getIndexOptions().getString("name"))) {
                return definition;
            }
        }
        throw new IllegalStateException(SAFE_FAILURE_MESSAGE);
    }

    private void verifyIndex(IndexOperations operations, String name) {
        IndexInfo actual = operations.getIndexInfo().stream()
                .filter(index -> name.equals(index.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(SAFE_FAILURE_MESSAGE));

        List<String> expectedFields = NICKNAME_INDEX_NAME.equals(name)
                ? List.of("nickname")
                : List.of("provider", "providerId");

        if (!actual.isUnique()
                || !actual.isIndexForFields(expectedFields)
                || actual.getPartialFilterExpression() == null) {
            throw new IllegalStateException(SAFE_FAILURE_MESSAGE);
        }
    }
}
