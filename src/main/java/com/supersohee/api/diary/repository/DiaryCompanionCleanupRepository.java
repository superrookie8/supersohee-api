package com.supersohee.api.diary.repository;

import com.supersohee.api.diary.domain.Diary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DiaryCompanionCleanupRepository {
    private final MongoOperations mongoOperations;

    public long removeUserFromOtherDiaries(String userId) {
        Query otherDiaries = Query.query(Criteria.where("userId").ne(userId).and("companion").is(userId));
        return mongoOperations.updateMulti(
                otherDiaries,
                new Update().pull("companion", userId),
                Diary.class).getModifiedCount();
    }
}
