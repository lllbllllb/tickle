package com.lllbllllb.tickleservice.stateful;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.lllbllllb.tickleservice.model.TouchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.lllbllllb.tickleservice.model.TouchResult.Status.SUCCESS;
import static com.lllbllllb.tickleservice.model.TouchResult.Status.TIMEOUT;
import static com.lllbllllb.tickleservice.model.TouchResult.Status.UNEXPECTED_STATUS;

@Slf4j
@Service
public class HitResultService implements Finalizable, Resettable {

    private final Map<String, AtomicLong> preyNameToSuccessCountMap = new ConcurrentHashMap<>();

    private final Map<String, AtomicLong> preyNameToTimeoutCountMap = new ConcurrentHashMap<>();

    private final Map<String, AtomicLong> preyNameToErrorCountMap = new ConcurrentHashMap<>();

    public TouchResult applySuccess(String preyName, long attemptNumber, long responseTime) {
        var successCount = preyNameToSuccessCountMap.get(preyName).incrementAndGet();
        var timeoutCount = preyNameToTimeoutCountMap.get(preyName).get();
        var errorCount = preyNameToErrorCountMap.get(preyName).get();

        return new TouchResult(responseTime, SUCCESS, attemptNumber, successCount, timeoutCount, errorCount);
    }

    public TouchResult applyTimeout(String preyName, long attemptNumber, long responseTime) {
        var successCount = preyNameToSuccessCountMap.get(preyName).get();
        var timeoutCount = preyNameToTimeoutCountMap.get(preyName).incrementAndGet();
        var errorCount = preyNameToErrorCountMap.get(preyName).get();

        return new TouchResult(responseTime, TIMEOUT, attemptNumber, successCount, timeoutCount, errorCount);
    }

    public TouchResult applyError(String preyName, long attemptNumber, long responseTime) {
        var successCount = preyNameToSuccessCountMap.get(preyName).get();
        var timeoutCount = preyNameToTimeoutCountMap.get(preyName).get();
        var errorCount = preyNameToErrorCountMap.get(preyName).incrementAndGet();

        return new TouchResult(responseTime, UNEXPECTED_STATUS, attemptNumber, successCount, timeoutCount, errorCount);
    }

    @Override
    public void reset(String preyName) {
        preyNameToSuccessCountMap.put(preyName, new AtomicLong(0));
        preyNameToTimeoutCountMap.put(preyName, new AtomicLong(0));
        preyNameToErrorCountMap.put(preyName, new AtomicLong(0));

        log.info("Attempts counters for [{}] was reset successfully", preyName);
    }

    @Override
    public void finalize(String preyName) {
        preyNameToSuccessCountMap.remove(preyName);
        preyNameToTimeoutCountMap.remove(preyName);
        preyNameToErrorCountMap.remove(preyName);

        log.info("Attempts counters for [{}] was successfully finalized", preyName);

    }
}
