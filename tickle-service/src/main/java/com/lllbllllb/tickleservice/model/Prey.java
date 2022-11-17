package com.lllbllllb.tickleservice.model;

import java.util.Map;

import org.springframework.http.HttpMethod;

public record Prey(
    String name,

    String path,

    HttpMethod method,

    String requestParameters,

    Map<String, String> headers,

    String requestBody,

    long timeoutMs,

    int expectedResponseStatusCode
) {

}
