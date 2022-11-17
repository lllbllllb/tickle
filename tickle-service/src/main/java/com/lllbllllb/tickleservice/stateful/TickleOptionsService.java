package com.lllbllllb.tickleservice.stateful;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.lllbllllb.tickleservice.model.TickleOptions;
import com.lllbllllb.tickleservice.model.Prey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class TickleOptionsService implements Initializable, Finalizable {

    private static final TickleOptions DEFAULT_LOAD_CONFIGURATION = new TickleOptions(0, false, 30);

    private static final long NANOS_PER_SECOND =  1000_000_000L;

    private final Map<String, TickleOptions> nameToLoadOptionsMap = new ConcurrentHashMap<>();

    public TickleOptions getLoadOptions() {
        if (nameToLoadOptionsMap.size() > 0) {
            return nameToLoadOptionsMap.values().iterator().next();
        } else {
            return DEFAULT_LOAD_CONFIGURATION;
        }
    }

    public TickleOptions getLoadOptions(String preyName) {
        var configuration = nameToLoadOptionsMap.get(preyName);

        if (configuration != null) {
            return configuration;
        }

        throw new IllegalStateException("Load configuration for [%s] not exists".formatted(preyName));
    }

    public void updateLoadOptions(String preyName, TickleOptions tickleOptions) {
        nameToLoadOptionsMap.put(preyName, tickleOptions);
    }

    public Mono<Duration> getLoadInterval(String preyName) {
        var rps = getLoadOptions(preyName).rps();

        if (rps > 0) {
            return Mono.just(Duration.ofNanos(NANOS_PER_SECOND / rps));
        }

        return Mono.empty();
    }

    public int getMaxConcurrency(String preyName) {
        var configuration = getLoadOptions(preyName).rps();

        return 9_999_999; // to achive SpscArrayQueue and avoid SpscLinkedArrayQueue
    }

    @Override
    public void finalize(String preyName) {
        nameToLoadOptionsMap.remove(preyName);
    }

    @Override
    public void initialize(Prey prey) {
        nameToLoadOptionsMap.put(prey.name(), DEFAULT_LOAD_CONFIGURATION);
    }
}
