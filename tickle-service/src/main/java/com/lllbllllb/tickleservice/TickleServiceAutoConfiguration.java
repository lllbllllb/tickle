package com.lllbllllb.tickleservice;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
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

import static org.springframework.web.reactive.function.server.RequestPredicates.DELETE;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.noContent;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

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
        LoaderService loaderService,
        ObjectMapperService objectMapperService
    ) {
        return session -> {
            var preyName = session.getHandshakeInfo().getUri().getQuery();

            if (preyName == null) {
                throw new IllegalArgumentException("No serviceName present");
            }

            var out = loaderService.getLoadEventStream(preyName)
                .map(objectMapperService::toJson)
                .map(session::textMessage)
                .as(session::send)
                .doOnCancel(() -> loaderService.disconnectPrey(preyName));
            var in = session.receive()
                .doOnNext(webSocketMessage -> {
                    var json = webSocketMessage.getPayloadAsText();
                    var incomeEvent = objectMapperService.fromJson(json);

                    loaderService.load(preyName, incomeEvent);
                })
                .doOnError(err -> log.error(err.getMessage(), err))
                .then();

            return Mono.zip(in, out).then();
        };
    }

    @Bean
    WebSocketHandler countdownWebSocketHandler(
        LoaderService loaderService,
        ObjectMapperService objectMapperService
    ) {
        return session -> {
            var preyName = session.getHandshakeInfo().getUri().getQuery();

            if (preyName == null) {
                throw new IllegalArgumentException("No serviceName present");
            }

            return loaderService.getTimerEventStream(preyName)
                .map(objectMapperService::toJson)
                .map(session::textMessage)
                .as(session::send)
                .doFinally(signalType -> log.info("Countdown WS handler was disconnected by reason [{}]", signalType));
        };
    }

    @Bean
    RouterFunction<ServerResponse> loaderRestController(LoaderService loaderService) {
        var urlPrey = "/prey";
        var urlRps = "/loadParameters";

        return route(POST(urlPrey), request -> request.bodyToMono(Prey.class)
            .flatMap(prey -> {
                loaderService.registerPrey(prey);

                return noContent().build();
            }))
            .and(route(GET(urlPrey), request -> ok().body(Flux.fromIterable(loaderService.getAllPreys()), Prey.class)))
            .and(route(DELETE(urlPrey + "/{name}"), request -> {
                var name = request.pathVariable("name");

                loaderService.finalizePrey(name);

                return noContent().build();
            }))
            .and(route(GET(urlRps), request -> ok().body(Mono.fromCallable(loaderService::getLoadConfiguration), LoadOptions.class)));
    }

    @Bean
    HttpClient httpClient() {
        return java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions.resources("/**", new FileSystemResource("/templates/"));
    }

    @Bean
    public RouterFunction<ServerResponse> htmlRouter(@Value("classpath:templates/index.html") Resource html) {
        return route(GET("/"), request -> ok()
            .contentType(MediaType.TEXT_HTML)
            .header("Content-Type", "text/css")
            .header("Content-Type", "text/javascript")
            .bodyValue(html));
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
