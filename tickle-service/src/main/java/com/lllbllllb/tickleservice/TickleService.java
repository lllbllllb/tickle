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
import com.lllbllllb.tickleservice.stateful.Initializable;
import com.lllbllllb.tickleservice.stateful.OutputStreamService;
import com.lllbllllb.tickleservice.stateful.Resettable;
import com.lllbllllb.tickleservice.stateful.SessionService;
import com.lllbllllb.tickleservice.stateful.TickleOptionsService;
import com.lllbllllb.tickleservice.stateful.TouchResultService;
import com.lllbllllb.tickleservice.stateful.TouchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TickleService {

    private final HttpClient httpClient;

    private final Clock clock;

    private final TouchService touchService;

    private final SessionService sessionService;

    private final TouchResultService touchResultService;

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
        tickleOptionsService.updateLoadOptions(tickleOptions);

        return Mono.fromRunnable(() -> tickleOptionsService.getLoadInterval().ifPresent(
            interval -> {
                countdownService.runCountdown(
                    preyName,
                    tickleOptions,
                    countdownTick -> outputStreamService.pushCountdownTick(preyName, countdownTick),
//                    () -> resettables.forEach(resettable -> resettable.reset(preyName))
                    () -> currentTickleService.reset(preyName)
                );

                var prey = sessionService.getPrey(preyName);
                var disposable = Flux.interval(interval)
                    .onBackpressureDrop(dropped -> log.warn("Tick {} was dropped due to lack of requests", dropped)) // https://stackoverflow.com/a/60092653
                    .flatMap(i -> {
                        var number = i + 1;
                        var start = clock.millis();
                        var touch = touchService.getTouch(preyName);

                        return Mono.fromFuture(() -> httpClient.sendAsync(touch, responseInfo -> {
                                var responseTime = clock.millis() - start;

                                if (responseInfo.statusCode() == prey.expectedResponseStatusCode()) {
                                    return HttpResponse.BodySubscribers.replacing(touchResultService.applySuccess(preyName, number, responseTime));
                                } else {
                                    return HttpResponse.BodySubscribers.replacing(touchResultService.applyError(preyName, number, responseTime));
                                }
                            })
                            .thenApply(HttpResponse::body)
                            .exceptionally(throwable -> {
                                var responseTime = clock.millis() - start;

                                if (CompletionException.class.equals(throwable.getClass())
                                    && (HttpTimeoutException.class.equals(throwable.getCause().getClass())
                                    || HttpConnectTimeoutException.class.equals(throwable.getCause().getClass())
                                    || CancellationException.class.equals(throwable.getCause().getClass()))) {
                                    return touchResultService.applyTimeout(preyName, number, responseTime);
                                } else {
                                    log.debug(throwable.getMessage(), throwable);

                                    return touchResultService.applyError(preyName, number, responseTime);
                                }
                            }));

                    }, tickleOptionsService.getMaxConcurrency(), 1)
                    .subscribe(
                        touchResult -> outputStreamService.pushTouchResult(preyName, touchResult),
                        throwable -> resettables.forEach(resettable -> resettable.reset(preyName)),
//                        () -> resettables.forEach(resettable -> resettable.reset(preyName))
                        () -> log.info("Load for [{}] is over", preyName)
                    );

                currentTickleService.registerActiveLoaderDisposable(preyName, disposable);
            }
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
        var stopWhenDisconnect = getTickleOptions().stopWhenDisconnect();
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

    public TickleOptions getTickleOptions() {
        return tickleOptionsService.getTickleOptions();
    }

    public void restore(List<Prey> preys) {
        sessionService.getAllPreys().forEach(prey -> finalizables.forEach(finalizable -> finalizable.finalize(prey.name())));
        preys.forEach(prey -> initializables.forEach(initializable -> initializable.initialize(prey)));
    }
}
