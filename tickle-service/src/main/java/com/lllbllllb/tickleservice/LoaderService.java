package com.lllbllllb.tickleservice;

import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoaderService {

    private final HttpClient httpClient;

    private final Clock clock;

    private final HttpRequestService httpRequestService;

    private final SessionService sessionService;

    private final HitResultService hitResultService;

    private final LoadService loadService;

    private final LoadOptionsService loadOptionsService;

    private final CountdownService countdownService;

    private final List<Finalizable> finalizables;

    private final List<Resettable> resettables;

    private final List<Initializable> initializables;

    public void load(String preyName, LoadOptions loadOptions) {
        resettables.forEach(resettable -> resettable.reset(preyName));
        loadOptionsService.updateLoadOptions(preyName, loadOptions);
        loadOptionsService.getLoadInterval(preyName)
            .doOnNext(interval -> countdownService.runCountdown(
                preyName,
                loadOptions,
                countdownTick -> sessionService.publishACountdownTick(preyName, countdownTick),
                () -> resettables.forEach(resettable -> resettable.reset(preyName))
            ))
            .map(interval -> Flux.interval(interval)
                .onBackpressureDrop(dropped -> log.warn("Tick {} was dropped due to lack of requests", dropped))// https://stackoverflow.com/a/60092653
                .parallel().runOn(Schedulers.newParallel(preyName))
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
                                return hitResultService.applyError(preyName, number, responseTime);
                            }
                        }));
                }, false, loadOptionsService.getMaxConcurrency(preyName), 1)
                .subscribe(
                    event -> sessionService.publishAttemptResult(preyName, event),
                    throwable -> finalizePrey(preyName),
                    () -> finalizePrey(preyName)
                )
            )
            .subscribe(disposable -> loadService.registerActiveLoaderDisposable(preyName, disposable));
    }

    public Flux<HitResult> getLoadEventStream(String preyName) {
        return sessionService.subscribeToAttemptResultStream(preyName);
    }

    public Flux<CountdownTick> getTimerEventStream(String preyName) {
        return sessionService.subscribeToCountdownTickStream(preyName);
    }

    public void registerPrey(Prey prey) {
        initializables.forEach(initializable -> initializable.initialize(prey));

        log.info("Initialization for [{}] was successfully completed", prey.name());
    }

    public void disconnectPrey(String preyName) {
        var stopWhenDisconnect = getLoadConfiguration().stopWhenDisconnect();
        var sessionsLeft = sessionService.handleUnsubscribeFromAttemptResultStream(preyName);

        if (sessionsLeft < 1 && stopWhenDisconnect) {
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

    public LoadOptions getLoadConfiguration() {
        return loadOptionsService.getLoadOptions();
    }
}
