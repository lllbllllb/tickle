package com.lllbllllb.tickleservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lllbllllb.tickleservice.model.TickleOptions;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ObjectMapperService {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public TickleOptions fromJson(String json) {
        return objectMapper.readValue(json, TickleOptions.class);
    }

    @SneakyThrows
    public String toJson(Object attemptResult) {
        return objectMapper.writeValueAsString(attemptResult);
    }

}
