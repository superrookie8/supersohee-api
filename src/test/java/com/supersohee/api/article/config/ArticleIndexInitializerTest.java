package com.supersohee.api.article.config;

import com.supersohee.api.article.domain.Article;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoSimpleTypes;
import org.springframework.data.mapping.model.SimpleTypeHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleIndexInitializerTest {
    private MongoTemplate mongoTemplate;
    private IndexOperations indexOperations;
    private ArticleIndexInitializer initializer;

    @BeforeEach
    void setUp() throws Exception {
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(new SimpleTypeHolder(
                Set.of(LocalDateTime.class), MongoSimpleTypes.HOLDER));
        mappingContext.afterPropertiesSet();
        MongoConverter converter = mock(MongoConverter.class);
        mongoTemplate = mock(MongoTemplate.class);
        indexOperations = mock(IndexOperations.class);
        doReturn(mappingContext).when(converter).getMappingContext();
        when(mongoTemplate.getConverter()).thenReturn(converter);
        when(mongoTemplate.indexOps(Article.class)).thenReturn(indexOperations);
        initializer = new ArticleIndexInitializer(mongoTemplate);
    }

    @Test
    void ensuresAndVerifiesOnlyTheAnnotationDefinedArticleIndex() throws Exception {
        IndexInfo actual = IndexInfo.indexInfoOf(new Document()
                .append("name", ArticleIndexInitializer.INDEX_NAME)
                .append("key", new Document("source", 1).append("url", 1))
                .append("unique", true)
                .append("partialFilterExpression", new Document("source", new Document("$type", "string"))
                        .append("url", new Document("$type", "string"))));
        assertThat(actual.getName()).isEqualTo(ArticleIndexInitializer.INDEX_NAME);
        assertThat(actual.isUnique()).isTrue();
        assertThat(actual.isIndexForFields(List.of("source", "url"))).isTrue();
        assertThat(actual.getPartialFilterExpression()).isNotNull();
        when(indexOperations.getIndexInfo()).thenReturn(List.of(actual));
        assertThat(initializer.resolveExpectedIndex().getIndexOptions().getString("name"))
                .isEqualTo(ArticleIndexInitializer.INDEX_NAME);

        initializer.run(new DefaultApplicationArguments());

        verify(indexOperations).createIndex(any(IndexDefinition.class));
        verify(indexOperations).getIndexInfo();
    }

    @Test
    void duplicateDataOrIndexMismatchFailsStartupWithoutLeakingMongoDetails() {
        when(indexOperations.createIndex(any(IndexDefinition.class)))
                .thenThrow(new IllegalStateException("duplicate source/url sensitive fixture"));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> initializer.run(new DefaultApplicationArguments()))
                .satisfies(exception -> {
                    assertThat(exception.getMessage())
                            .isEqualTo("Required article unique index could not be ensured.");
                    assertThat(exception.getCause()).isNull();
                    assertThat(exception.getMessage()).doesNotContain("sensitive fixture");
                });
    }

    @Test
    void wrongExistingIndexOptionsFailClosed() {
        IndexInfo nonUnique = IndexInfo.indexInfoOf(new Document()
                .append("name", ArticleIndexInitializer.INDEX_NAME)
                .append("key", new Document("source", 1).append("url", 1))
                .append("unique", false)
                .append("partialFilterExpression", new Document("source", new Document("$type", "string"))
                        .append("url", new Document("$type", "string"))));
        when(indexOperations.getIndexInfo()).thenReturn(List.of(nonUnique));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> initializer.run(new DefaultApplicationArguments()))
                .withMessage("Required article unique index could not be ensured.");
    }
}
