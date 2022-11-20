package com.lllbllllb.tickleservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ObjectMapperService {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public String toJson(Object attemptResult) {
        return objectMapper.writeValueAsString(attemptResult);
    }

}
