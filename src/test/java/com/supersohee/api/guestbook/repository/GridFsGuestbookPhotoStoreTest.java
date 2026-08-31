package com.supersohee.api.guestbook.repository;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GridFsGuestbookPhotoStoreTest {

    @Test
    void readsLegacyPythonGridFsRootContentType() {
        Document file = new Document("contentType", "image/jpeg");

        assertThat(GridFsGuestbookPhotoStore.readContentType(file)).isEqualTo("image/jpeg");
    }

    @Test
    void readsSpringAndLegacyMetadataContentTypes() {
        Document springFile = new Document("metadata", new Document("_contentType", "image/png"));
        Document legacyFile = new Document("metadata", new Document("content_type", "image/webp"));

        assertThat(GridFsGuestbookPhotoStore.readContentType(springFile)).isEqualTo("image/png");
        assertThat(GridFsGuestbookPhotoStore.readContentType(legacyFile)).isEqualTo("image/webp");
        assertThat(GridFsGuestbookPhotoStore.readContentType(new Document())).isNull();
    }
}
