package com.lllbllllb.tickleservice;

import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import com.lllbllllb.tickleservice.model.CountdownTick;
import com.lllbllllb.tickleservice.model.Prey;
import com.lllbllllb.tickleservice.model.TickleOptions;
import com.lllbllllb.tickleservice.model.TouchResult;
import com.lllbllllb.tickleservice.stateful.CountdownService;
import com.lllbllllb.tickleservice.stateful.CurrentTickleService;
import com.lllbllllb.tickleservice.stateful.Finalizable;
import com.lllbllllb.tickleservice.stateful.HitResultService;
import com.lllbllllb.tickleservice.stateful.HttpRequestService;
import com.lllbllllb.tickleservice.stateful.Initializable;
import com.lllbllllb.tickleservice.stateful.OutputStreamService;
import com.lllbllllb.tickleservice.stateful.Resettable;
import com.lllbllllb.tickleservice.stateful.SessionService;
import com.lllbllllb.tickleservice.stateful.TickleOptionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class TickleService {

    private final HttpClient httpClient;

    private final Clock clock;

    private final HttpRequestService httpRequestService;

    private final SessionService sessionService;

    private final HitResultService hitResultService;

    private final CurrentTickleService currentTickleService;

    private final TickleOptionsService tickleOptionsService;

    private final CountdownService countdownService;

    private final OutputStreamService outputStreamService;

    private final List<Finalizable> finalizables;

    private final List<Resettable> resettables;

    private final List<Initializable> initializables;


    public Mono<Void> load(TickleOptions tickleOptions) {
        return Flux.fromIterable(sessionService.getAllEnabledPreys())
            .concatMap(prey -> load(prey.name(), tickleOptions))
            .then();
    }

    public Mono<Void> load(String preyName, TickleOptions tickleOptions) {
        resettables.forEach(resettable -> resettable.reset(preyName));
        tickleOptionsService.updateLoadOptions(preyName, tickleOptions);

        return Mono.fromRunnable(() -> tickleOptionsService.getLoadInterval(preyName).ifPresentOrElse(
            interval -> {
                countdownService.runCountdown(
                    preyName,
                    tickleOptions,
                    countdownTick -> outputStreamService.pushACountdownTick(preyName, countdownTick),
                    () -> resettables.forEach(resettable -> resettable.reset(preyName))
                );

                var disposable = Flux.interval(interval)
                    .onBackpressureDrop(dropped -> log.warn("Tick {} was dropped due to lack of requests", dropped)) // https://stackoverflow.com/a/60092653
//                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(i -> {
                        var number = i + 1;
                        var httpRequest = httpRequestService.getHttpRequest(preyName);
                        var start = clock.millis();

                        return Mono.fromFuture(httpClient.sendAsync(httpRequest, responseInfo -> {
                                var responseTime = clock.millis() - start;

                                if (responseInfo.statusCode() == 200) {
                                    return HttpResponse.BodySubscribers.replacing(hitResultService.applySuccess(preyName, number, responseTime));
                                } else {
                                    return HttpResponse.BodySubscribers.replacing(hitResultService.applyError(preyName, number, responseTime));
                                }
                            })
                            .thenApply(HttpResponse::body)
                            .exceptionally(throwable -> {
                                var responseTime = clock.millis() - start;

                                if (CompletionException.class.equals(throwable.getClass())
                                    && (HttpTimeoutException.class.equals(throwable.getCause().getClass())
                                    || HttpConnectTimeoutException.class.equals(throwable.getCause().getClass())
                                    || CancellationException.class.equals(throwable.getCause().getClass()))) {
                                    return hitResultService.applyTimeout(preyName, number, responseTime);
                                } else {
                                    log.error(throwable.getMessage(), throwable);

                                    return hitResultService.applyError(preyName, number, responseTime);
                                }
                            }));
                    }, tickleOptionsService.getMaxConcurrency(preyName), 1)
                    .subscribe(
                        touchResult -> outputStreamService.pushTouchResult(preyName, touchResult),
                        throwable -> finalizePrey(preyName),
                        () -> finalizePrey(preyName)
                    );

                currentTickleService.registerActiveLoaderDisposable(preyName, disposable);
            },
            () -> resettables.forEach(resettable -> resettable.reset(preyName))
        ));
    }

    public Flux<TouchResult> getTouchResultStream(String preyName) {
        return outputStreamService.getTouchResultStream(preyName);
    }

    public Flux<CountdownTick> getCountdownTickStream(String preyName) {
        return outputStreamService.getCountdownTickStream(preyName);
    }

    public void registerPrey(Prey prey) {
        initializables.forEach(initializable -> initializable.initialize(prey));

        log.info("Initialization for [{}] was successfully completed", prey.name());
    }

    public void patchPrey(String preyName, Prey patch) {
        sessionService.patchPrey(preyName, patch);
    }

    public void disconnectPrey(String preyName) {
        var stopWhenDisconnect = getLoadConfiguration().stopWhenDisconnect();
        var hasSubscribers = outputStreamService.hasSubscribers(preyName);

        if (!hasSubscribers && stopWhenDisconnect) {
            resettables.forEach(resettable -> resettable.reset(preyName));
        }
    }

    public void finalizePrey(String preyName) {
        finalizables.forEach(finalizable -> finalizable.finalize(preyName));

        log.info("Finalization for [{}] was successfully completed", preyName);
    }

    public List<Prey> getAllPreys() {
        return sessionService.getAllPreys();
    }

    public TickleOptions getLoadConfiguration() {
        return tickleOptionsService.getLoadOptions();
    }
}
