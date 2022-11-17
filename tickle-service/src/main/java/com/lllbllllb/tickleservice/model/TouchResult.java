package com.lllbllllb.tickleservice.model;

public record TouchResult(
    long responseTime,

    Status status,
    long attemptNumber,

    long successCount,

    long timeoutCount,

    long errorCount

) {

    public enum Status {
        SUCCESS,
        TIMEOUT,
        UNEXPECTED_STATUS
    }
}
