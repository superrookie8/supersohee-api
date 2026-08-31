package com.supersohee.api.image.admin.repository;

import com.supersohee.api.image.admin.domain.AdminPhotoSource;
import com.supersohee.api.image.admin.domain.StoredAdminPhoto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface AdminPhotoStore {
    List<StoredAdminPhoto> findAll(AdminPhotoSource source);
    StoredAdminPhoto storeAdminPhoto(MultipartFile file) throws IOException;
    Optional<StoredAdminPhoto> findByIdAdminFirst(String id);
    void delete(StoredAdminPhoto photo);
}
