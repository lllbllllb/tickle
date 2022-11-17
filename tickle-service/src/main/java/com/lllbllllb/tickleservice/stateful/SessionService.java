package com.lllbllllb.tickleservice.stateful;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.lllbllllb.tickleservice.model.Prey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SessionService implements Initializable, Finalizable {

    private final Map<String, Prey> preyNameToPreyMap = new ConcurrentHashMap<>();

    @Override
    public void initialize(Prey prey) {
        var preyName = prey.name();

        preyNameToPreyMap.put(preyName, prey);

        log.info("Session for [{}] was initialized", prey);
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

    }
}
