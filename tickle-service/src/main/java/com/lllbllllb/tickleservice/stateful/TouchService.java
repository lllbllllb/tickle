package com.lllbllllb.tickleservice.stateful;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import com.lllbllllb.tickleservice.model.Prey;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static reactor.core.scheduler.Schedulers.DEFAULT_POOL_SIZE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TouchService implements Initializable, Finalizable {

    // see jdk.internal.net.http.common.Utils.ALLOWED_HEADERS
    // -Djdk.httpclient.allowRestrictedHeaders=Connection
    private static final Set<String> RESTRICTED_HEADERS = Set.of("connection", "content-length", "expect", "host", "upgrade");
    private final Map<String, HttpRequest> preyNameToHttpRequestMap = new ConcurrentHashMap<>();

    private final Map<String, HttpClient> preyNameToHttpClientMap = new ConcurrentHashMap<>();

    public HttpRequest getTouch(String preyName) {
        var httpRequest = preyNameToHttpRequestMap.get(preyName);

        if (httpRequest != null) {
            return httpRequest;
        }

        throw new IllegalStateException("[HttpRequest] for [%s] not found".formatted(preyName));
    }

    public HttpClient getClient(String preyName) {
        var client = preyNameToHttpClientMap.get(preyName);

        if (client != null) {
            return client;
        }

        throw new IllegalStateException("[HttpClient] for [%s] not found".formatted(preyName));
    }

    @Override
    @SneakyThrows
    public void initialize(Prey prey) {
        var uri = new URI("%s?%s".formatted(prey.path(), prey.requestParameters()));
        var method = prey.method().name();
        var timeout = Duration.ofMillis(prey.timeoutMs());
        var requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .version(HttpClient.Version.HTTP_1_1)
            .timeout(timeout);
        var requestBody = prey.requestBody();

        requestBuilder.method(method, requestBody != null ? ofString(requestBody) : noBody());

        prey.headers().forEach((name, value) -> {
            if (!RESTRICTED_HEADERS.contains(name.toLowerCase())) {
                requestBuilder.header(name, value);
            }
        });

        var request = requestBuilder.build();
        var name = prey.name();
        var httpClient = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .executor(Executors.newFixedThreadPool(DEFAULT_POOL_SIZE))
            .build();

        preyNameToHttpRequestMap.put(name, request);
        preyNameToHttpClientMap.put(name, httpClient);

        log.info("[HttpRequest] for [{}] was initialized", prey);
    }

    @Override
    public void finalize(String preyName) {
        var httpRequest = preyNameToHttpRequestMap.remove(preyName);

        if (httpRequest != null) {
            log.warn("[HttpRequest] for [{}] was finalized", preyName);
        } else {
            log.warn("[HttpRequest] for [{}] was already finalized", preyName);
        }
    }
}
