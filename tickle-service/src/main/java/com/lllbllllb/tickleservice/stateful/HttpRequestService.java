package com.lllbllllb.tickleservice.stateful;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.lllbllllb.tickleservice.model.Prey;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpRequestService implements Initializable, Finalizable {

    // see jdk.internal.net.http.common.Utils.ALLOWED_HEADERS
    // -Djdk.httpclient.allowRestrictedHeaders=Connection
    private static final Set<String> RESTRICTED_HEADERS = Set.of("connection", "content-length", "expect", "host", "upgrade");
    private final Map<String, HttpRequest> preyNameToHttpRequestMap = new ConcurrentHashMap<>();

    public HttpRequest getHttpRequest(String preyName) {
        var httpRequest = preyNameToHttpRequestMap.get(preyName);

        if (httpRequest != null) {
            return httpRequest;
        }

        throw new IllegalStateException("[HttpRequest] for [%s] not found".formatted(preyName));
    }

    @Override
    @SneakyThrows
    public void initialize(Prey prey) {
        var uri = new URI("%s?%s".formatted(prey.path(), prey.requestParameters()));
        var method = prey.method().name();
        var timeout = Duration.ofMillis(prey.timeoutMs());
        var requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(timeout);
        var requestBody = prey.requestBody();

        if (requestBody != null) {
            requestBuilder.method(method, ofString(requestBody));
        } else {
            requestBuilder.method(method, noBody());
        }

        prey.headers().entrySet().stream()
            .filter(entry -> !RESTRICTED_HEADERS.contains(entry.getKey().toLowerCase()))
            .forEach(entry -> requestBuilder.header(entry.getKey(), entry.getValue()));

        var request = requestBuilder.build();


        var name = prey.name();

        preyNameToHttpRequestMap.put(name, request);

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
