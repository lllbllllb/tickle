package com.lllbllllb.tickleservice.model;

public record TouchResult(
    long responseTime,

    Status status,

    long attemptNumber,

    long successCount,

    long timeoutCount,

    long errorCount

) {

    @Override
    public String toString() {
         return """
             {
                 "responseTime": %s,
                 "status": "%s",
                 "attemptNumber": %s,
                 "successCount": %s,
                 "timeoutCount": %s,
                 "errorCount": %s
             }
             """.formatted(responseTime, status, attemptNumber, successCount, timeoutCount, errorCount);
    }

    public enum Status {
        SUCCESS,
        TIMEOUT,
        UNEXPECTED_STATUS
    }
}
