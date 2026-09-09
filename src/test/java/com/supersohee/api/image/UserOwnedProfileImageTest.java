package com.supersohee.api.image;

import com.supersohee.api.image.service.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserOwnedProfileImageTest {
    @Test
    void listsEveryPageAndDeletesOnlyObjectsInsideTheUsersPrefix() {
        S3Client s3 = mock(S3Client.class);
        TestImageUploadService service = serviceUsing(s3);
        when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder()
                                .contents(object("profile/user-1/old.webp"), object("profile/user-2/other.webp"))
                                .isTruncated(true)
                                .nextContinuationToken("next-page")
                                .build(),
                        ListObjectsV2Response.builder()
                                .contents(object("profile/user-1/current.webp"), object("diary/legacy.webp"))
                                .isTruncated(false)
                                .build());
        when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenReturn(DeleteObjectsResponse.builder().build());

        service.deleteAllUserProfileImages("user-1");

        var listCaptor = org.mockito.ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3, org.mockito.Mockito.times(2)).listObjectsV2(listCaptor.capture());
        assertThat(listCaptor.getAllValues())
                .extracting(ListObjectsV2Request::prefix)
                .containsOnly("profile/user-1/");
        assertThat(listCaptor.getAllValues())
                .extracting(ListObjectsV2Request::continuationToken)
                .containsExactly(null, "next-page");

        var deleteCaptor = org.mockito.ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3).deleteObjects(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().delete().objects())
                .extracting(software.amazon.awssdk.services.s3.model.ObjectIdentifier::key)
                .containsExactly("profile/user-1/old.webp", "profile/user-1/current.webp");
        verify(s3).close();
    }

    @Test
    void listFailureDoesNotAttemptAnyDelete() {
        S3Client s3 = mock(S3Client.class);
        TestImageUploadService service = serviceUsing(s3);
        when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(new IllegalStateException("sensitive key detail"));

        assertThatThrownBy(() -> service.deleteAllUserProfileImages("user-1"))
                .isInstanceOf(IllegalStateException.class);
        verify(s3, never()).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3).close();
    }

    @Test
    void rejectsUnsafeUserIdBeforeOpeningStorageClient() {
        S3Client s3 = mock(S3Client.class);
        TestImageUploadService service = serviceUsing(s3);

        assertThatThrownBy(() -> service.deleteAllUserProfileImages("../user-1"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(s3, never()).listObjectsV2(any(ListObjectsV2Request.class));
    }

    private static TestImageUploadService serviceUsing(S3Client s3) {
        TestImageUploadService service = new TestImageUploadService(s3);
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
        return service;
    }

    private static S3Object object(String key) {
        return S3Object.builder().key(key).build();
    }

    private static final class TestImageUploadService extends ImageUploadService {
        private final S3Client s3;

        private TestImageUploadService(S3Client s3) {
            this.s3 = s3;
        }

        @Override
        protected S3Client getS3Client() {
            return s3;
        }
    }
}
