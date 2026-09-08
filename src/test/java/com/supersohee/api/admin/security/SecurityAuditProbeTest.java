package com.supersohee.api.admin.security;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class SecurityAuditProbeTest {
    @Test
    void validatesEveryDnsAddressAndNeverContactsPrivateOrMetadataTargets() throws Exception {
        for (String ip :
                List.of(
                        "127.0.0.1",
                        "10.0.0.1",
                        "169.254.169.254",
                        "100.64.0.1",
                        "192.168.0.1",
                        "::1",
                        "fc00::1")) {
            var probe =
                    new SecurityAuditProbe(
                            h ->
                                    new InetAddress[] {
                                        InetAddress.getByName("8.8.8.8"), InetAddress.getByName(ip)
                                    },
                            (u, p) -> {
                                throw new AssertionError("No network call allowed");
                            });
            assertThat(probe.inspect("https://frontend.example.com", true))
                    .allMatch(c -> c.status().equals("unknown"));
            probe.shutdown();
        }
    }

    @Test
    void publicDnsIsPinnedOnceWithNoRedirectFollowOrCredentials() throws Exception {
        AtomicInteger dns = new AtomicInteger();
        List<URI> urls = new ArrayList<>();
        var probe =
                new SecurityAuditProbe(
                        h -> {
                            dns.incrementAndGet();
                            return new InetAddress[] {InetAddress.getByName("8.8.8.8")};
                        },
                        (u, p) -> {
                            urls.add(u);
                            assertThat(u.getRawUserInfo()).isNull();
                            assertThat(p[0].getHostAddress()).isEqualTo("8.8.8.8");
                            return new SecurityAuditProbe.Observation(
                                    302, false, false, false, false);
                        });
        var checks = probe.inspect("https://frontend.example.com", true);
        assertThat(dns.get()).isOne();
        assertThat(urls).hasSize(5);
        assertThat(urls)
                .allMatch(
                        u ->
                                SecurityAuditProbe.PATHS.contains(u.getPath())
                                        && u.getHost().equals("frontend.example.com"));
        assertThat(checks)
                .filteredOn(c -> c.id().equals("frontend-admin"))
                .allMatch(c -> c.status().equals("warn"));
        probe.shutdown();
    }

    @Test
    void invalidOriginsDoNotResolveAndTimeoutDoesNotLeakSentinel() {
        var probe =
                new SecurityAuditProbe(
                        h -> {
                            throw new AssertionError();
                        },
                        (u, p) -> {
                            throw new SocketTimeoutException(
                                    "SECRET_SENTINEL https://private.invalid/key");
                        });
        for (String uri :
                List.of(
                        "http://10.0.0.1",
                        "https://user:SECRET_SENTINEL@frontend.example.com",
                        "https://frontend.example.com/a",
                        "https://frontend.example.com?token=SECRET_SENTINEL"))
            assertThat(probe.inspect(uri, true)).allMatch(c -> c.status().equals("unknown"));
        var checks = probe.inspect("http://localhost:3000", false);
        assertThat(checks).hasSize(5).allMatch(c -> c.status().equals("unknown"));
        assertThat(checks.toString()).doesNotContain("SECRET_SENTINEL", "private.invalid");
        probe.shutdown();
    }

    @Test
    void productionCannotProbeLoopbackAndSuccessIsScopedToObservedResponse() throws Exception {
        var probe =
                new SecurityAuditProbe(
                        h -> new InetAddress[] {InetAddress.getByName("8.8.8.8")},
                        (u, p) ->
                                new SecurityAuditProbe.Observation(
                                        u.getPath().equals("/api/admin/session") ? 401 : 200,
                                        true,
                                        true,
                                        true,
                                        true));
        assertThat(probe.targetKind("http://localhost:3000", true)).isEqualTo("unknown");
        assertThat(probe.inspect("https://frontend.example.com", true))
                .anyMatch(c -> c.id().equals("frontend-session") && c.status().equals("pass"));
        probe.shutdown();
    }

    @Test
    void headerTokensMustMatchExactly() {
        var response = new org.apache.hc.core5.http.message.BasicHttpResponse(200);
        response.setHeader("X-Content-Type-Options", "notnosniff");
        response.setHeader("X-Robots-Tag", "not-noindex");
        response.setHeader("Cache-Control", "x-no-store");
        assertThat(SecurityAuditProbe.has(response, "X-Content-Type-Options", "nosniff")).isFalse();
        assertThat(SecurityAuditProbe.has(response, "X-Robots-Tag", "noindex")).isFalse();
        assertThat(SecurityAuditProbe.has(response, "Cache-Control", "no-store")).isFalse();
        response.setHeader("X-Content-Type-Options", " NOSNIFF ");
        response.setHeader("X-Robots-Tag", "nofollow, noindex");
        response.setHeader("Cache-Control", "private, no-store");
        assertThat(SecurityAuditProbe.has(response, "X-Content-Type-Options", "nosniff")).isTrue();
        assertThat(SecurityAuditProbe.has(response, "X-Robots-Tag", "noindex")).isTrue();
        assertThat(SecurityAuditProbe.has(response, "Cache-Control", "no-store")).isTrue();
    }

    @Test
    void actualTransportSendsNoCredentialsAndDoesNotFollowRedirect() throws Exception {
        var server =
                com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger redirected = new AtomicInteger();
        List<String> credentials = new java.util.concurrent.CopyOnWriteArrayList<>();
        server.createContext(
                "/",
                exchange -> {
                    if (exchange.getRequestURI().getPath().equals("/redirected"))
                        redirected.incrementAndGet();
                    if (exchange.getRequestHeaders().getFirst("Authorization") != null)
                        credentials.add("authorization");
                    if (exchange.getRequestHeaders().getFirst("Cookie") != null)
                        credentials.add("cookie");
                    exchange.getResponseHeaders().set("Location", "/redirected");
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();
                });
        server.start();
        try {
            var result =
                    SecurityAuditProbe.fetch(
                            URI.create(
                                    "http://fixture.example.com:"
                                            + server.getAddress().getPort()
                                            + "/"),
                            new InetAddress[] {InetAddress.getByName("127.0.0.1")});
            assertThat(result.status()).isEqualTo(302);
            assertThat(redirected.get()).isZero();
            assertThat(credentials).isEmpty();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void actualTransportDeadlineStopsSlowHeaderDrip() throws Exception {
        try (var server = new java.net.ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            Thread responder =
                    new Thread(
                            () -> {
                                try (var socket = server.accept()) {
                                    for (int i = 0; i < 30; i++) {
                                        socket.getOutputStream().write('H');
                                        socket.getOutputStream().flush();
                                        Thread.sleep(150);
                                    }
                                } catch (Exception ignored) {
                                }
                            });
            responder.setDaemon(true);
            responder.start();
            long start = System.nanoTime();
            assertThatThrownBy(
                            () ->
                                    SecurityAuditProbe.fetch(
                                            URI.create(
                                                    "http://fixture.example.com:"
                                                            + server.getLocalPort()
                                                            + "/"),
                                            new InetAddress[] {InetAddress.getByName("127.0.0.1")}))
                    .isInstanceOf(Exception.class);
            assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(4000);
        }
    }
}
