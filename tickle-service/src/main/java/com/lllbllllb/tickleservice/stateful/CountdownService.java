package com.lllbllllb.tickleservice.stateful;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.lllbllllb.tickleservice.model.CountdownTick;
import com.lllbllllb.tickleservice.model.TickleOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class CountdownService implements Finalizable, Resettable {

    private final Map<String, Disposable> preyNameToLoadTimerDisposable = new ConcurrentHashMap<>();

    public void runCountdown(
        String preyName,
        TickleOptions tickleOptions,
        Consumer<CountdownTick> countdownTickConsumer,
        Runnable finallyCallback
    ) {
        var loadTime = tickleOptions.loadTimeSec();
        var disposable = Flux.range(1, loadTime)
            .delayElements(Duration.ofSeconds(1))
            .doFinally(signal -> finallyCallback.run())
            .subscribe(tick -> countdownTickConsumer.accept(new CountdownTick(loadTime, loadTime - tick)));

        preyNameToLoadTimerDisposable.put(preyName, disposable);
    }

    @Override
    public void finalize(String preyName) {
        var disposable = preyNameToLoadTimerDisposable.remove(preyName);

        if (disposable != null) {
            disposable.dispose();
        } else {
            log.info("Countdown for [{}] was already stopped", preyName);
        }
    }

    @Override
    public void reset(String preyName) {
        finalize(preyName);
    }
}
