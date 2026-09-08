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

/** Fixed origin/path, validated DNS pinned per run, normal TLS identity verification. */
@Component
public class SecurityAuditProbe {
    static final List<String> PATHS =
            List.of("/", "/admin", "/api/admin/session", "/robots.txt", "/sitemap.xml");

    interface Resolver {
        InetAddress[] resolve(String host) throws Exception;
    }

    interface Transport {
        Observation get(URI target, InetAddress[] pinned) throws Exception;
    }

    record Observation(
            int status, boolean csp, boolean nosniff, boolean noindex, boolean noStore) {}

    private static final ScheduledExecutorService DEADLINES =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "audit-http-deadlines");
                        t.setDaemon(true);
                        return t;
                    });
    private final Resolver resolver;
    private final Transport transport;
    private final ExecutorService dns =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "audit-dns");
                        t.setDaemon(true);
                        return t;
                    });

    public SecurityAuditProbe() {
        this(InetAddress::getAllByName, SecurityAuditProbe::fetch);
    }

    SecurityAuditProbe(Resolver resolver, Transport transport) {
        this.resolver = resolver;
        this.transport = transport;
    }

    @jakarta.annotation.PreDestroy
    void shutdown() {
        dns.shutdownNow();
    }

    public String targetKind(String configured, boolean production) {
        try {
            URI u = URI.create(configured);
            if (u.getRawUserInfo() != null
                    || u.getRawQuery() != null
                    || u.getRawFragment() != null
                    || u.getHost() == null
                    || !(u.getPath().isEmpty() || u.getPath().equals("/"))
                    || u.getPort() > 65535
                    || u.getPort() == 0) return "unknown";
            if (!production
                    && "http".equals(u.getScheme())
                    && List.of("localhost", "127.0.0.1").contains(u.getHost()))
                return "loopback-development";
            if ("https".equals(u.getScheme())
                    && (u.getPort() == -1 || u.getPort() == 443)
                    && u.getHost().matches("[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,63}")
                    && !u.getHost().endsWith(".local")
                    && !u.getHost().endsWith(".internal")
                    && !u.getHost().endsWith(".localhost")) return "https-configured";
        } catch (RuntimeException ignored) {
        }
        return "unknown";
    }

    public List<SecurityAuditRun.Check> inspect(String configured, boolean production) {
        String kind = targetKind(configured, production);
        if (kind.equals("unknown"))
            return List.of(
                    check(
                            "frontend-target",
                            "unknown",
                            "The configured origin is not an allowed probe target.",
                            0,
                            null));
        URI origin = URI.create(configured);
        InetAddress[] addresses;
        try {
            if (kind.equals("loopback-development"))
                addresses = new InetAddress[] {InetAddress.getByAddress(new byte[] {127, 0, 0, 1})};
            else {
                Future<InetAddress[]> task = dns.submit(() -> resolver.resolve(origin.getHost()));
                try {
                    addresses = task.get(1, TimeUnit.SECONDS);
                } finally {
                    task.cancel(true);
                }
                if (addresses == null
                        || addresses.length == 0
                        || Arrays.stream(addresses).anyMatch(a -> !publicAddress(a)))
                    return List.of(
                            check(
                                    "frontend-dns",
                                    "unknown",
                                    "DNS contained a non-public address; no HTTP request was sent.",
                                    0,
                                    null));
            }
        } catch (Exception ignored) {
            return List.of(
                    check(
                            "frontend-dns",
                            "unknown",
                            "DNS resolution failed or exceeded its deadline; no HTTP request was"
                                + " sent.",
                            0,
                            null));
        }
        List<SecurityAuditRun.Check> results = new ArrayList<>();
        for (String path : PATHS) {
            if (Thread.currentThread().isInterrupted()) break;
            long start = System.nanoTime();
            String id =
                    "frontend-"
                            + switch (path) {
                                case "/" -> "home";
                                case "/admin" -> "admin";
                                case "/api/admin/session" -> "session";
                                case "/robots.txt" -> "robots";
                                default -> "sitemap";
                            };
            try {
                Observation o = transport.get(origin.resolve(path), addresses.clone());
                String status =
                        path.equals("/api/admin/session")
                                ? (o.status() == 401 || o.status() == 403
                                        ? "pass"
                                        : o.status() >= 500 ? "unknown" : "fail")
                                : path.equals("/admin")
                                        ? (o.status() == 401 || o.status() == 403 ? "pass" : "warn")
                                        : o.status() == 200 ? "pass" : "warn";
                results.add(
                        check(
                                id,
                                status,
                                "Observed this fixed anonymous endpoint only; redirects were not"
                                    + " followed and authenticated access was not tested.",
                                elapsed(start),
                                o.status()));
                if (path.equals("/") || path.equals("/admin")) {
                    results.add(
                            check(
                                    id + "-headers",
                                    o.csp() && o.nosniff() ? "pass" : "warn",
                                    "Checked CSP presence and nosniff on this response only; policy"
                                        + " effectiveness is not certified.",
                                    0,
                                    o.status()));
                    if (path.equals("/admin"))
                        results.add(
                                check(
                                        id + "-privacy",
                                        o.noindex() && o.noStore() ? "pass" : "warn",
                                        "Checked noindex and no-store on this response only; this"
                                            + " does not establish authorization.",
                                        0,
                                        o.status()));
                }
            } catch (Exception ignored) {
                results.add(
                        check(
                                id,
                                "unknown",
                                "The bounded request failed or timed out; no response content or"
                                    + " exception was retained.",
                                elapsed(start),
                                null));
            }
        }
        return results;
    }

    static boolean publicAddress(InetAddress a) {
        if (a.isAnyLocalAddress()
                || a.isLoopbackAddress()
                || a.isLinkLocalAddress()
                || a.isSiteLocalAddress()
                || a.isMulticastAddress()) return false;
        byte[] b = a.getAddress();
        int first = b[0] & 255;
        if (b.length == 16) return (first & 0xfe) != 0xfc && first != 0 && first != 0xff;
        int second = b[1] & 255;
        return first != 0
                && first != 10
                && first != 127
                && first < 224
                && !(first == 100 && second >= 64 && second <= 127)
                && !(first == 169 && second == 254)
                && !(first == 172 && second >= 16 && second <= 31)
                && !(first == 192 && (second == 168 || second == 0))
                && !(first == 198 && (second == 18 || second == 19));
    }

    static Observation fetch(URI target, InetAddress[] pinned) throws Exception {
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
            request.setHeader("Accept", "text/html,application/json,text/plain");
            var deadline = DEADLINES.schedule(request::cancel, 2, TimeUnit.SECONDS);
            try {
                var response = client.execute(request);
                try {
                    return new Observation(
                            response.getCode(),
                            Arrays.stream(response.getHeaders("Content-Security-Policy"))
                                    .anyMatch(h -> !h.getValue().isBlank()),
                            has(response, "X-Content-Type-Options", "nosniff"),
                            has(response, "X-Robots-Tag", "noindex"),
                            has(response, "Cache-Control", "no-store"));
                } finally {
                    response.close(CloseMode.IMMEDIATE);
                } // Zero body bytes consumed, including error bodies.
            } finally {
                deadline.cancel(false);
            }
        }
    }

    static boolean has(
            org.apache.hc.core5.http.HttpResponse response, String name, String expected) {
        return Arrays.stream(response.getHeaders(name))
                .anyMatch(
                        header -> {
                            if (name.equalsIgnoreCase("X-Content-Type-Options"))
                                return header.getValue().trim().equalsIgnoreCase(expected);
                            return Arrays.stream(header.getValue().split(","))
                                    .anyMatch(token -> token.trim().equalsIgnoreCase(expected));
                        });
    }

    private long elapsed(long begin) {
        return (System.nanoTime() - begin) / 1_000_000;
    }

    private SecurityAuditRun.Check check(
            String id, String status, String evidence, long duration, Integer code) {
        return new SecurityAuditRun.Check(
                id,
                "frontend",
                "Frontend fixed endpoint observation",
                status,
                "runtime-http",
                evidence,
                "Review the observed endpoint and trusted deployment configuration.",
                Instant.now(),
                duration,
                code);
    }
}
