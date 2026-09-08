package com.supersohee.api.internationalresult;

import com.supersohee.api.internationalresult.config.InternationalResultV1Migration;
import com.supersohee.api.internationalresult.domain.InternationalResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternationalResultV1MigrationTest {
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final IndexOperations indexes = mock(IndexOperations.class);
    private final InternationalResultV1Migration migration = new InternationalResultV1Migration(mongo);

    @Test
    void insertsEveryStableKeyBeforeWritingCompletionMarker() throws Exception {
        when(mongo.indexOps(InternationalResult.class)).thenReturn(indexes);
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);

        migration.run(new DefaultApplicationArguments());

        verify(indexes).createIndex(any(IndexDefinition.class));
        verify(mongo, times(10)).upsert(any(Query.class), any(Update.class), eq(InternationalResult.class));
        verify(mongo).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
        var ordered = inOrder(mongo);
        ordered.verify(mongo, times(10)).upsert(any(Query.class), any(Update.class), eq(InternationalResult.class));
        ordered.verify(mongo).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }

    @Test
    void markerSkipsSeedsButStillEnsuresRequiredUniqueIndex() throws Exception {
        when(mongo.indexOps(InternationalResult.class)).thenReturn(indexes);
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(true);

        migration.run(new DefaultApplicationArguments());

        verify(indexes).createIndex(any(IndexDefinition.class));
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq(InternationalResult.class));
    }

    @Test
    void failedSeedDoesNotWriteMarkerAndFailureMessageDoesNotLeakData() {
        when(mongo.indexOps(InternationalResult.class)).thenReturn(indexes);
        when(mongo.exists(any(Query.class), eq("data_migrations"))).thenReturn(false);
        AtomicBoolean first = new AtomicBoolean(true);
        when(mongo.upsert(any(Query.class), any(Update.class), eq(InternationalResult.class)))
                .thenAnswer(invocation -> {
                    if (first.getAndSet(false)) {
                        throw new IllegalStateException("sensitive stored value");
                    }
                    return null;
                });

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> migration.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("International result migration v1 could not be completed.")
                .hasNoCause();
        verify(mongo, never()).upsert(any(Query.class), any(Update.class), eq("data_migrations"));
    }
}
