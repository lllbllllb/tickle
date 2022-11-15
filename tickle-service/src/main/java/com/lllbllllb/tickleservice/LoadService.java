package com.lllbllllb.tickleservice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadService implements Finalizable, Resettable {

    private final Map<String, Disposable> preyNameToDisposableMap = new ConcurrentHashMap<>();

    public void registerActiveLoaderDisposable(String preyName, Disposable loaderDisposable) {
        preyNameToDisposableMap.put(preyName, loaderDisposable);
    }

    @Override
    public void finalize(String preyName) {
        reset(preyName);
    }

    @Override
    public void reset(String preyName) {
        var loadDisposable = preyNameToDisposableMap.remove(preyName);

        if (loadDisposable != null && !loadDisposable.isDisposed()) {
            loadDisposable.dispose();
        } else {
            log.info("Load disposable for [{}] was already finalized", preyName);
        }
    }
}
