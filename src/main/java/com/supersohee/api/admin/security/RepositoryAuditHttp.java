package com.supersohee.api.admin.security;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Fixed GitHub host, pinned public DNS, no redirects, bounded body, no response logging. */
@Component
public class RepositoryAuditHttp {
    record Reply(int status, com.fasterxml.jackson.databind.JsonNode body, boolean more) {}
    private static final ScheduledExecutorService DEADLINES = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "repository-http-deadline"); t.setDaemon(true); return t;
    });
    private final ExecutorService dns = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new SynchronousQueue<>(), r -> { Thread t = new Thread(r, "repository-dns"); t.setDaemon(true); return t; });
    @jakarta.annotation.PreDestroy void shutdown() { dns.shutdownNow(); }
    Reply get(String path, String token) throws Exception {
        if (!path.matches("/repos/superrookie8/(sofanpage|supersohee-api)/(actions/runs\\?per_page=1|dependabot/alerts\\?state=open&per_page=100|actions/workflows/supersohee-weekly-crawler\\.yml(?:/runs\\?event=schedule&per_page=1)?)"))
            throw new IllegalArgumentException();
        Future<InetAddress[]> task = dns.submit(() -> InetAddress.getAllByName("api.github.com"));
        InetAddress[] pinned;
        try { pinned = task.get(1, TimeUnit.SECONDS); } finally { task.cancel(true); }
        if (pinned.length == 0 || Arrays.stream(pinned).anyMatch(a -> !SecurityAuditProbe.publicAddress(a)))
            throw new java.io.IOException();
        return fetch(URI.create("https://api.github.com" + path), pinned, token);
    }
    static Reply fetch(URI target, InetAddress[] pinned, String token) throws Exception {
        if (!"https".equals(target.getScheme()) || !"api.github.com".equals(target.getHost())
                || target.getPort() != -1 || target.getUserInfo() != null) throw new IllegalArgumentException();
        String host = target.getHost();
        DnsResolver fixed =
                new DnsResolver() {
                    public InetAddress[] resolve(String requested) throws UnknownHostException {
                        if (!host.equalsIgnoreCase(requested)) throw new UnknownHostException();
                        return pinned.clone();
                    }

                    public String resolveCanonicalHostname(String requested)
                            throws UnknownHostException {
                        if (!host.equalsIgnoreCase(requested)) throw new UnknownHostException();
                        return host;
                    }
                };
        var manager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDnsResolver(fixed)
                        .setMaxConnTotal(1)
                        .setMaxConnPerRoute(1)
                        .setConnectionFactory(
                                ManagedHttpClientConnectionFactory.builder()
                                        .http1Config(
                                                Http1Config.custom()
                                                        .setMaxHeaderCount(64)
                                                        .setMaxLineLength(8192)
                                                        .build())
                                        .build())
                        .setDefaultConnectionConfig(
                                ConnectionConfig.custom()
                                        .setConnectTimeout(Timeout.ofSeconds(1))
                                        .setSocketTimeout(Timeout.ofSeconds(1))
                                        .build())
                        .build();
        // No system credential/proxy stores; default TLS hostname and certificate checks remain
        // enabled.
        try (var client =
                HttpClients.custom()
                        .setConnectionManager(manager)
                        .disableRedirectHandling()
                        .disableAutomaticRetries()
                        .disableCookieManagement()
                        .disableAuthCaching()
                        .disableContentCompression()
                        .setDefaultRequestConfig(
                                RequestConfig.custom()
                                        .setConnectionRequestTimeout(Timeout.ofSeconds(1))
                                        .setResponseTimeout(Timeout.ofSeconds(1))
                                        .build())
                        .build()) {
            var request = new HttpGet(target);
            request.setHeader("Accept", "application/vnd.github+json");
            request.setHeader("X-GitHub-Api-Version", "2022-11-28");
            if (!token.isBlank()) request.setHeader("Authorization", "Bearer " + token);
            var deadline = DEADLINES.schedule(request::cancel, 2, TimeUnit.SECONDS);
            try {
                var response = client.execute(request);
                try {
                    int status = response.getCode();
                    if (status != 200) return new Reply(status, null, false);
                    if (response.getEntity() == null) throw new java.io.IOException();
                    boolean more = Arrays.stream(response.getHeaders("Link"))
                            .anyMatch(h -> h.getValue().contains("rel=\"next\""));
                    return decode(status, response.getEntity().getContent(), more);
                } finally {
                    response.close(CloseMode.IMMEDIATE);
                } // Non-success response bodies are not consumed.
            } finally {
                deadline.cancel(false);
            }
        }
    }

    static Reply decode(int status, java.io.InputStream body, boolean more) throws java.io.IOException {
        if (status != 200) return new Reply(status, null, false);
        byte[] bytes = body.readNBytes(262145);
        if (bytes.length > 262144) throw new java.io.IOException();
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(bytes);
        return new Reply(status, json, more);
    }

}
