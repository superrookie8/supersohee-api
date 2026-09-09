package com.supersohee.api.diary;

import com.mongodb.client.result.UpdateResult;
import com.supersohee.api.diary.domain.Diary;
import com.supersohee.api.diary.repository.DiaryCompanionCleanupRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiaryCompanionCleanupRepositoryTest {
    @Test
    void pullsUserOnlyFromOtherUsersCompanionArrays() {
        MongoOperations mongo = mock(MongoOperations.class);
        when(mongo.updateMulti(any(Query.class), any(Update.class), eq(Diary.class)))
                .thenReturn(UpdateResult.acknowledged(3, 2L, null));
        DiaryCompanionCleanupRepository repository = new DiaryCompanionCleanupRepository(mongo);

        assertThat(repository.removeUserFromOtherDiaries("user-1")).isEqualTo(2);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateMulti(query.capture(), update.capture(), eq(Diary.class));
        assertThat(query.getValue().getQueryObject().toJson())
                .contains("userId", "$ne", "user-1", "companion");
        assertThat(update.getValue().getUpdateObject().toJson())
                .contains("$pull", "companion", "user-1")
                .doesNotContain("$set");
    }
}
