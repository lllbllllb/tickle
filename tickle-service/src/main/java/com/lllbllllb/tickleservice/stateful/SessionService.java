package com.lllbllllb.tickleservice.stateful;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.lllbllllb.tickleservice.model.Prey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
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

    @Override
    public void finalize(String preyName) {
        var prey = preyNameToPreyMap.remove(preyName);

        if (prey != null) {
            log.warn("Prey [{}] is already finalized", preyName);
        }

    }

    public List<Prey> getAllEnabledPreys() {
        return preyNameToPreyMap.values().stream()
            .filter(Prey::enabled)
            .collect(Collectors.toList());
    }

    public List<Prey> getAllPreys() {
        return preyNameToPreyMap.values().stream()
            .sorted(Comparator.comparing(Prey::name))
            .collect(Collectors.toList());
    }

    public void patchPrey(String preyName, Prey patch) {
        var original = preyNameToPreyMap.get(preyName);

        if (original == null) {
            throw new IllegalStateException("No prey found by name [%s]".formatted(preyName));
        }

        var patched = new Prey(
            preyName,
            original.path(),
            original.method(),
            original.requestParameters(),
            original.headers(),
            original.requestBody(),
            original.timeoutMs(),
            original.expectedResponseStatusCode(),
            ObjectUtils.firstNonNull(patch.enabled(), original.enabled())
        );

        preyNameToPreyMap.put(preyName, patched);
    }
}
