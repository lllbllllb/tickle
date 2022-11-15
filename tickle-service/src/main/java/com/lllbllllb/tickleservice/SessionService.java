package com.lllbllllb.tickleservice;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static com.lllbllllb.tickleservice.StreamUtils.CUSTOM_EMIT_FAILURE_HANDLER;
import static reactor.util.concurrent.Queues.SMALL_BUFFER_SIZE;

@Slf4j
@Service
public class SessionService implements Initializable, Finalizable {

    private final Map<String, Sinks.Many<HitResult>> preyNameToAttemptResultSink = new ConcurrentHashMap<>();

    private final Map<String, Sinks.Many<CountdownTick>> preyNameToCountdownTickSink = new ConcurrentHashMap<>();

    private final Map<String, Prey> preyNameToPreyMap = new ConcurrentHashMap<>();

    private final AtomicInteger sessions = new AtomicInteger(0);

    @Override
    public void initialize(Prey prey) {
        var preyName = prey.name();

        preyNameToPreyMap.put(preyName, prey);

        var attemptResultSink = Sinks.many().multicast().<HitResult>onBackpressureBuffer(Integer.MAX_VALUE, false);

        preyNameToAttemptResultSink.put(preyName, attemptResultSink);

        var countdownTickSink = Sinks.many().multicast().<CountdownTick>onBackpressureBuffer(SMALL_BUFFER_SIZE, false);

        preyNameToCountdownTickSink.put(preyName, countdownTickSink);

        log.info("Session for [{}] was initialized", prey);
    }

    public Flux<HitResult> subscribeToAttemptResultStream(String preyName) {
        var attemptResultSink = preyNameToAttemptResultSink.get(preyName);

        if (attemptResultSink == null) {
            throw new IllegalStateException("[AttemptResult] stream for [%s] already closed".formatted(preyName));
        }

        sessions.incrementAndGet();

        var stream = attemptResultSink.asFlux();

        log.info("[AttemptResult] stream for [{}] got a subscriber", preyName);

        return stream;
    }

    public Flux<CountdownTick> subscribeToCountdownTickStream(String preyName) {
        var countdownTickSink = preyNameToCountdownTickSink.get(preyName);

        if (countdownTickSink == null) {
            throw new IllegalStateException("[CountdownTick] steam for [%s] already closed".formatted(preyName));
        }

        var stream = countdownTickSink.asFlux();

        log.info("[CountdownTick] stream for [{}] got a subscriber", preyName);

        return stream;
    }

    public int handleUnsubscribeFromAttemptResultStream(String preyName) {
        var sessionsCount = sessions.decrementAndGet();

        log.info("Output stream [{}] lost a subscriber. [{}] session(s) left", preyName, sessionsCount);

        return sessionsCount;
    }

    public List<Prey> getAllPreys() {
        return preyNameToPreyMap.values().stream()
            .sorted(Comparator.comparing(Prey::name))
            .collect(Collectors.toList());
    }

    @Override
    public void finalize(String preyName) {
        var prey = preyNameToPreyMap.remove(preyName);

        if (prey != null) {
            log.warn("Prey [{}] is already finalized", preyName);
        }

        var attemptResultSink = preyNameToAttemptResultSink.remove(preyName);

        if (attemptResultSink != null) {
            attemptResultSink.emitComplete(CUSTOM_EMIT_FAILURE_HANDLER);
        } else {
            log.warn("[AttemptResult] stream for [{}] was already finalized", preyName);
        }

        var countdownTickSink = preyNameToCountdownTickSink.remove(preyName);

        if (countdownTickSink != null) {
            countdownTickSink.emitComplete(CUSTOM_EMIT_FAILURE_HANDLER);
        } else {
            log.warn("[CountdownTick] stream for [{}] was already finalized", preyName);
        }

    }

    public void publishAttemptResult(String preyName, HitResult hitResult) {
        var sink = preyNameToAttemptResultSink.get(preyName);

        if (sink == null) {
            throw new IllegalStateException("[AttemptResult] sink for [%s] not found".formatted(preyName));
        }

        sink.emitNext(hitResult, CUSTOM_EMIT_FAILURE_HANDLER);
    }

    public void publishACountdownTick(String preyName, CountdownTick countdownTick) {
        var sink = preyNameToCountdownTickSink.get(preyName);

        if (sink == null) {
            throw new IllegalStateException("[CountdownTick] sink for [%s] not found".formatted(preyName));
        }

        sink.emitNext(countdownTick, CUSTOM_EMIT_FAILURE_HANDLER);
    }
}
