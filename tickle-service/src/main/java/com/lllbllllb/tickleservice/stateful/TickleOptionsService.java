package com.lllbllllb.tickleservice.stateful;

import java.time.Duration;
import java.util.Optional;

import com.lllbllllb.tickleservice.model.Prey;
import com.lllbllllb.tickleservice.model.TickleOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TickleOptionsService implements Initializable, Finalizable {

    private static final TickleOptions DEFAULT_LOAD_CONFIGURATION = new TickleOptions(0, false, 30);

    private static final long NANOS_PER_SECOND = 1000_000_000L;

    private volatile TickleOptions tickleOptions = DEFAULT_LOAD_CONFIGURATION;

    public TickleOptions getTickleOptions() {
        return tickleOptions;
    }

    public void updateLoadOptions(TickleOptions tickleOptions) {
        this.tickleOptions = tickleOptions;
    }

    public Optional<Duration> getLoadInterval() {
        var rps = tickleOptions.rps();

        if (rps > 0) {
            return Optional.of(Duration.ofNanos(NANOS_PER_SECOND / rps));
        }

        return Optional.empty();
    }

    public int getMaxConcurrency() {
        return 9_999_999; // to achive SpscArrayQueue and avoid SpscLinkedArrayQueue
    }

    @Override
    public void finalize(String preyName) {
        tickleOptions = DEFAULT_LOAD_CONFIGURATION;
    }

    @Override
    public void initialize(Prey prey) {
        tickleOptions = DEFAULT_LOAD_CONFIGURATION;
    }
}
