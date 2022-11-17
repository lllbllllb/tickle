package com.lllbllllb.tickleservice.stateful;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.lllbllllb.tickleservice.model.CountdownTick;
import com.lllbllllb.tickleservice.model.Prey;
import com.lllbllllb.tickleservice.model.TouchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import static com.lllbllllb.tickleservice.StreamUtils.CUSTOM_EMIT_FAILURE_HANDLER;
import static reactor.util.concurrent.Queues.SMALL_BUFFER_SIZE;

@Slf4j
@Service
public class OutputStreamService implements Initializable, Finalizable {

    private final Map<String, Sinks.Many<TouchResult>> preyNameToTouchResultSink = new ConcurrentHashMap<>();

    private final Map<String, Sinks.Many<CountdownTick>> preyNameToCountdownTickSink = new ConcurrentHashMap<>();

    @Override
    public void finalize(String preyName) {
        var attemptResultSink = preyNameToTouchResultSink.remove(preyName);

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

    @Override
    public void initialize(Prey prey) {
        var preyName = prey.name();
        var attemptResultSink = Sinks.many().multicast().<TouchResult>onBackpressureBuffer(Integer.MAX_VALUE, false);

        preyNameToTouchResultSink.put(preyName, attemptResultSink);

        var countdownTickSink = Sinks.many().multicast().<CountdownTick>onBackpressureBuffer(SMALL_BUFFER_SIZE, false);

        preyNameToCountdownTickSink.put(preyName, countdownTickSink);

        log.info("[OutputStream] for [{}] was initialized", preyName);
    }

    public Flux<TouchResult> getTouchResultStream(String preyName) {
        var attemptResultSink = preyNameToTouchResultSink.get(preyName);

        if (attemptResultSink == null) {
            throw new IllegalStateException("[AttemptResult] stream for [%s] already closed".formatted(preyName));
        }

        log.info("[AttemptResult] stream for [{}] got a subscriber", preyName);

        return attemptResultSink.asFlux();
    }

    public Flux<CountdownTick> getCountdownTickStream(String preyName) {
        var countdownTickSink = preyNameToCountdownTickSink.get(preyName);

        if (countdownTickSink == null) {
            throw new IllegalStateException("[CountdownTick] steam for [%s] already closed".formatted(preyName));
        }

        log.info("[CountdownTick] stream for [{}] got a subscriber", preyName);

        return countdownTickSink.asFlux();
    }


    public void pushTouchResult(String preyName, TouchResult touchResult) {
        var sink = preyNameToTouchResultSink.get(preyName);

        if (sink == null) {
            throw new IllegalStateException("[AttemptResult] sink for [%s] not found".formatted(preyName));
        }

        sink.emitNext(touchResult, CUSTOM_EMIT_FAILURE_HANDLER);
    }

    public void pushACountdownTick(String preyName, CountdownTick countdownTick) {
        var sink = preyNameToCountdownTickSink.get(preyName);

        if (sink == null) {
            throw new IllegalStateException("[CountdownTick] sink for [%s] not found".formatted(preyName));
        }

        sink.emitNext(countdownTick, CUSTOM_EMIT_FAILURE_HANDLER);
    }

    public boolean hasSubscribers(String preyName) {
        var touchResultSubscribersCount = Optional.ofNullable(preyNameToTouchResultSink.get(preyName))
            .map(Sinks.Many::currentSubscriberCount)
            .orElse(0);
        var countdownSubscribersCount = Optional.ofNullable(preyNameToCountdownTickSink.get(preyName))
            .map(Sinks.Many::currentSubscriberCount)
            .orElse(0);

        return touchResultSubscribersCount > 0 && countdownSubscribersCount > 0;
    }
}
