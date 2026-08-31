package com.supersohee.api.guestbook.repository;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class GridFsGuestbookPhotoStore implements GuestbookPhotoStore {
    private static final String LEGACY_BUCKET = "guestbooks_photo";

    private final GridFsTemplate gridFsTemplate;
    private final MongoTemplate mongoTemplate;

    public GridFsGuestbookPhotoStore(
            MongoDatabaseFactory databaseFactory,
            MongoConverter converter,
            MongoTemplate mongoTemplate) {
        this.gridFsTemplate = new GridFsTemplate(databaseFactory, converter, LEGACY_BUCKET);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<StoredGuestbookPhoto> findById(ObjectId photoId) {
        GridFSFile file = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(photoId)));
        if (file == null) {
            return Optional.empty();
        }

        GridFsResource resource = gridFsTemplate.getResource(file);
        return Optional.of(new StoredGuestbookPhoto(resource, findContentType(photoId), file.getLength()));
    }

    private String findContentType(ObjectId photoId) {
        org.bson.Document fileDocument = mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(photoId)),
                org.bson.Document.class,
                LEGACY_BUCKET + ".files");
        return readContentType(fileDocument);
    }

    static String readContentType(org.bson.Document fileDocument) {
        if (fileDocument == null) {
            return null;
        }
        String rootContentType = firstString(fileDocument, "contentType", "content_type");
        if (rootContentType != null) {
            return rootContentType;
        }
        Object metadataValue = fileDocument.get("metadata");
        if (metadataValue instanceof org.bson.Document metadata) {
            return firstString(metadata, "_contentType", "contentType", "content_type");
        }
        return null;
    }

    private static String firstString(org.bson.Document document, String... keys) {
        for (String key : keys) {
            Object value = document.get(key);
            if (value instanceof String string && !string.isBlank()) {
                return string;
            }
        }
        return null;
    }
}
