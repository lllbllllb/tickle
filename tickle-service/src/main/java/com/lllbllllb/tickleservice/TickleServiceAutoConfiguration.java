package com.lllbllllb.tickleservice;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lllbllllb.tickleservice.model.Prey;
import com.lllbllllb.tickleservice.model.TickleOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.PATCH;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.noContent;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static reactor.core.scheduler.Schedulers.DEFAULT_POOL_SIZE;

@Slf4j
@ComponentScan
public class TickleServiceAutoConfiguration {

    @Bean
    CorsWebFilter corsWebFilter() {
        var corsConfig = new CorsConfiguration();

        corsConfig.setAllowedOrigins(List.of("*"));
        corsConfig.setMaxAge((Long) null);
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");

        var source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }

    @Bean
    HandlerAdapter wsHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    HandlerMapping webSocketMapping(
        WebSocketHandler loadWebSocketHandler,
        WebSocketHandler countdownWebSocketHandler
    ) {
        var map = Map.of(
            "/websocket/load", loadWebSocketHandler,
            "/websocket/countdown", countdownWebSocketHandler
        );
        var simpleUrlHandlerMapping = new SimpleUrlHandlerMapping();
        simpleUrlHandlerMapping.setUrlMap(map);
        simpleUrlHandlerMapping.setOrder(9);

        return simpleUrlHandlerMapping;
    }

    @Bean
    WebSocketHandler loadWebSocketHandler(
        TickleService tickleService,
        ObjectMapperService objectMapperService
    ) {
        return session -> {
            var preyName = session.getHandshakeInfo().getUri().getQuery();

            if (preyName == null) {
                throw new IllegalArgumentException("No serviceName present");
            }

            var in = session.receive()
                .map(webSocketMessage -> {
                    var frameSizeText = webSocketMessage.getPayloadAsText();

                    return Integer.parseInt(frameSizeText);
                })
                .doOnCancel(() -> tickleService.disconnectPrey(preyName));

            return tickleService.getTouchResultStream(preyName, in)
                .map(payload -> session.textMessage(objectMapperService.toJson(payload)))
                .as(session::send);
        };
    }

    @Bean
    WebSocketHandler countdownWebSocketHandler(
        TickleService tickleService,
        ObjectMapperService objectMapperService
    ) {
        return session -> {
            var preyName = session.getHandshakeInfo().getUri().getQuery();

            if (preyName == null) {
                throw new IllegalArgumentException("No serviceName present");
            }

            return tickleService.getCountdownTickStream(preyName)
                .map(payload -> session.textMessage(objectMapperService.toJson(payload)))
                .as(session::send)
                .doFinally(signalType -> log.info("Countdown WS handler was disconnected by reason [{}]", signalType));
        };
    }

    @Bean
    RouterFunction<ServerResponse> preyRestController(TickleService tickleService, ObjectMapper objectMapper) {
        var urlPrey = "/prey";
        var urlPreyName = urlPrey + "/{name}";
        var urlPreyFile = urlPrey + "/file";
        var tickleConfig = "tickle_config";

        return route(POST(urlPrey), request -> request.bodyToMono(Prey.class)
            .flatMap(prey -> {
                tickleService.registerPrey(prey);

                return noContent().build();
            }))
            .and(route(GET(urlPrey), request -> ok().body(Flux.fromIterable(tickleService.getAllPreys()), Prey.class)))
            .and(route(DELETE(urlPreyName), request -> {
                var name = request.pathVariable("name");

                tickleService.finalizePrey(name);

                return noContent().build();
            }))
            .and(route(PATCH(urlPreyName), request -> request.bodyToMono(Prey.class)
                .flatMap(prey -> {
                    var name = request.pathVariable("name");

                    tickleService.patchPrey(name, prey);

                    return noContent().build();
                })))
            .and(route(GET(urlPreyFile), request -> {
                try {
                    var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tickleService.getAllPreys());

                    return ServerResponse.ok()
                        .contentType(APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=%s.json".formatted(tickleConfig))
                        .contentLength(json.getBytes().length)
                        .bodyValue(json.getBytes());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }))
            .and(route(POST(urlPreyFile), request -> request.multipartData()
                .mapNotNull(multipart -> (FilePart) multipart.getFirst(tickleConfig))
                .flatMap(filePart -> filePart.content()
                    .map(dataBuffer -> {
                        var bytes = new byte[dataBuffer.readableByteCount()];

                        dataBuffer.read(bytes);

                        return bytes;
                    })
                    .reduce((b1, b2) -> {
                        var target = new byte[b1.length + b2.length];

                        System.arraycopy(b1, 0, target, 0, b1.length);
                        System.arraycopy(b2, 0, target, b1.length, b2.length);

                        return target;
                    }))
                .<List<Prey>>handle((bytes, sink) -> {
                    try {
                        sink.next(objectMapper.readValue(bytes, new TypeReference<List<Prey>>() {}));
                    } catch (Exception e) {
                        sink.error(e);
                    }
                })
                .doOnNext(tickleService::restore)
                .then(noContent().build())));
    }

    @Bean
    RouterFunction<ServerResponse> tickleRestController(TickleService tickleService) {
        var urlPreyLoad = "/prey/tickle";

        return route(PUT(urlPreyLoad), request -> request.bodyToMono(TickleOptions.class)
            .flatMap(tickleService::load)
            .then(noContent().build()));
    }


    @Bean
    RouterFunction<ServerResponse> tickleParametersRestController(TickleService tickleService) {
        var urlRps = "/tickleOptions";

        return route(GET(urlRps), request -> ok().body(Mono.fromCallable(tickleService::getTickleOptions), TickleOptions.class));
    }


    @Bean
    HttpClient httpClient() {
        return java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .executor(Executors.newFixedThreadPool(DEFAULT_POOL_SIZE))
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions.resources("/**", new FileSystemResource("/static/"))
            .and(RouterFunctions.resources("/**", new ClassPathResource("/static/")));
    }

    @Bean
    public RouterFunction<ServerResponse> htmlRouter(@Value("classpath:static/index.html") Resource html) {
        return route(GET("/"), request -> ok()
            .contentType(MediaType.TEXT_HTML)
            .header(CONTENT_TYPE, "text/css")
            .header(CONTENT_TYPE, "text/javascript")
            .bodyValue(html));
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
