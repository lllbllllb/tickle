package com.lllbllllb.tickleservice.model;

public record CountdownTick(
    int initial,
    int current
) {

    @Override
    public String toString() {
        return """
            {
            "initial": %s,
            "current": %s
            }
            """.formatted(initial, current);
    }
}
