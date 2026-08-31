package com.supersohee.api.image.admin.repository;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GridFsAdminPhotoStoreTest {

    @Test
    void adaptsPythonAndSpringGridFsContentTypeMetadata() {
        assertThat(GridFsAdminPhotoStore.extractContentType(
                new Document("contentType", "image/jpeg"))).isEqualTo("image/jpeg");
        assertThat(GridFsAdminPhotoStore.extractContentType(
                new Document("metadata", new Document("_contentType", "image/png"))))
                .isEqualTo("image/png");
        assertThat(GridFsAdminPhotoStore.extractContentType(new Document())).isNull();
    }
}
