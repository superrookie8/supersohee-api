package com.supersohee.api.image.admin.repository;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Sorts;
import com.supersohee.api.image.admin.domain.AdminPhotoSource;
import com.supersohee.api.image.admin.domain.StoredAdminPhoto;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GridFsAdminPhotoStore implements AdminPhotoStore {
    static final String ADMIN_BUCKET = "admin_photo";
    static final String USER_BUCKET = "user_photo";

    private final GridFsTemplate adminGridFs;
    private final GridFsTemplate userGridFs;
    private final MongoTemplate mongoTemplate;

    public GridFsAdminPhotoStore(
            MongoDatabaseFactory databaseFactory,
            MongoConverter converter,
            MongoTemplate mongoTemplate) {
        this.adminGridFs = new GridFsTemplate(databaseFactory, converter, ADMIN_BUCKET);
        this.userGridFs = new GridFsTemplate(databaseFactory, converter, USER_BUCKET);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<StoredAdminPhoto> findAll(AdminPhotoSource source) {
        GridFsTemplate template = template(source);
        List<StoredAdminPhoto> photos = new ArrayList<>();
        for (GridFSFile file : template.find(new Query()).sort(Sorts.descending("uploadDate"))) {
            photos.add(toPhoto(file, source, null));
        }
        return photos;
    }

    @Override
    public StoredAdminPhoto storeAdminPhoto(MultipartFile file) throws IOException {
        ObjectId id = adminGridFs.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType());
        return new StoredAdminPhoto(
                id.toHexString(),
                AdminPhotoSource.ADMIN,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                java.time.Instant.now(),
                null);
    }

    @Override
    public Optional<StoredAdminPhoto> findByIdAdminFirst(String id) {
        ObjectId objectId = new ObjectId(id);
        GridFSFile adminFile = findFile(adminGridFs, objectId);
        if (adminFile != null) {
            return Optional.of(toPhoto(adminFile, AdminPhotoSource.ADMIN, adminGridFs.getResource(adminFile)));
        }
        GridFSFile userFile = findFile(userGridFs, objectId);
        if (userFile != null) {
            return Optional.of(toPhoto(userFile, AdminPhotoSource.USER, userGridFs.getResource(userFile)));
        }
        return Optional.empty();
    }

    @Override
    public void delete(StoredAdminPhoto photo) {
        template(photo.source()).delete(Query.query(Criteria.where("_id").is(new ObjectId(photo.id()))));
    }

    private StoredAdminPhoto toPhoto(
            GridFSFile file,
            AdminPhotoSource source,
            GridFsResource resource) {
        return new StoredAdminPhoto(
                file.getObjectId().toHexString(),
                source,
                file.getFilename(),
                readContentType(source, file.getObjectId()),
                file.getLength(),
                file.getUploadDate().toInstant(),
                resource);
    }

    private GridFSFile findFile(GridFsTemplate template, ObjectId id) {
        return template.findOne(Query.query(Criteria.where("_id").is(id)));
    }

    private GridFsTemplate template(AdminPhotoSource source) {
        return source == AdminPhotoSource.ADMIN ? adminGridFs : userGridFs;
    }

    private String readContentType(AdminPhotoSource source, ObjectId id) {
        String bucket = source == AdminPhotoSource.ADMIN ? ADMIN_BUCKET : USER_BUCKET;
        Document fileDocument = mongoTemplate.findOne(
                Query.query(Criteria.where("_id").is(id)),
                Document.class,
                bucket + ".files");
        return extractContentType(fileDocument);
    }

    static String extractContentType(Document fileDocument) {
        if (fileDocument == null) {
            return null;
        }
        String rootContentType = firstString(fileDocument, "contentType", "content_type");
        if (rootContentType != null) {
            return rootContentType;
        }
        Object metadataValue = fileDocument.get("metadata");
        if (metadataValue instanceof Document metadata) {
            return firstString(metadata, "_contentType", "contentType", "content_type");
        }
        return null;
    }

    private static String firstString(Document document, String... keys) {
        for (String key : keys) {
            Object value = document.get(key);
            if (value instanceof String string && !string.isBlank()) {
                return string;
            }
        }
        return null;
    }
}
