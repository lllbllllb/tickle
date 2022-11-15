package com.lllbllllb.tickleservice;

import java.util.concurrent.locks.LockSupport;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Sinks;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class StreamUtils {

    public static final Sinks.EmitFailureHandler CUSTOM_EMIT_FAILURE_HANDLER = (signalType, emitResult) -> {
        if (emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
            LockSupport.parkNanos(10);
            return true;
        } else if (emitResult == Sinks.EmitResult.FAIL_TERMINATED || emitResult == Sinks.EmitResult.FAIL_OVERFLOW) {
            return Sinks.EmitFailureHandler.FAIL_FAST.onEmitFailure(signalType, emitResult);
        } else {
            return Sinks.EmitFailureHandler.FAIL_FAST.onEmitFailure(signalType, emitResult);
        }
    };
}
