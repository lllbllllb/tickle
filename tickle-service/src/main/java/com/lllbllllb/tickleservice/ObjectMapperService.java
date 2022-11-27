package com.lllbllllb.tickleservice;

import java.util.Collection;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ObjectMapperService {

    @SneakyThrows
    public String toJson(Object attemptResult) {
        return attemptResult.toString();
    }

    @SneakyThrows
    public String toJson(Collection<?> attemptResult) {
        return attemptResult.stream()
            .map(Object::toString)
            .collect(Collectors.joining(",", "[", "]"));
    }

}
