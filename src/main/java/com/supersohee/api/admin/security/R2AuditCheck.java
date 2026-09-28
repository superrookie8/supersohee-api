package com.supersohee.api.admin.security;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

@Component
public class R2AuditCheck {
    interface Head { void read(URI endpoint, String access, String secret, String bucket); }
    private final Environment env;
    private final Head head;
    @org.springframework.beans.factory.annotation.Autowired
    public R2AuditCheck(Environment env) { this(env, R2AuditCheck::read); }
    R2AuditCheck(Environment env, Head head) { this.env = env; this.head = head; }
    SecurityAuditRun.Check inspect() {
        String status = "unknown";
        String evidence = "R2 read was not completed; configuration, permission or service availability requires review.";
        try {
            URI endpoint = URI.create(env.getProperty("cloudflare.r2.endpoint", ""));
            String access = env.getProperty("cloudflare.r2.access-key-id", "");
            String secret = env.getProperty("cloudflare.r2.secret-access-key", "");
            String bucket = env.getProperty("cloudflare.r2.bucket-name", "");
            if (!allowed(endpoint) || access.isBlank() || secret.isBlank() || bucket.isBlank()) throw new IllegalArgumentException();
            head.read(endpoint, access, secret, bucket);
            status = "pass";
            evidence = "HEAD on the configured R2 bucket succeeded with the configured identity. No object body, listing, write, delete, ACL or backup request was sent. This does not establish public exposure, total size or restore capability.";
        } catch (RuntimeException ignored) { }
        return new SecurityAuditRun.Check("storage-r2", "storage", "storage-r2", status, "provider-api", evidence,
                "Review the existing R2 endpoint and read permission; credentials and bucket identifiers are never retained in findings.", Instant.now(), 0, null);
    }
    static boolean allowed(URI endpoint) {
        return "https".equals(endpoint.getScheme()) && endpoint.getHost() != null
                && endpoint.getHost().matches("[a-z0-9]+(?:\\.(?:eu|fedramp))?\\.r2\\.cloudflarestorage\\.com")
                && endpoint.getPort() == -1 && endpoint.getUserInfo() == null && endpoint.getQuery() == null
                && endpoint.getFragment() == null && (endpoint.getPath().isEmpty() || endpoint.getPath().equals("/"));
    }
    private static void read(URI endpoint, String access, String secret, String bucket) {
        try (S3Client client = S3Client.builder().endpointOverride(endpoint).region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access, secret)))
                .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(1)).socketTimeout(Duration.ofSeconds(1)))
                .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofSeconds(2)).apiCallAttemptTimeout(Duration.ofSeconds(2))
                        .retryPolicy(RetryPolicy.none())).build()) {
            client.headBucket(b -> b.bucket(bucket));
        }
    }
}
