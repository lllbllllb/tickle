package com.lllbllllb.tickleservice.model;

public record TickleOptions(

    int rps,

    boolean stopWhenDisconnect,

    int loadTimeSec,

    boolean watchLive
) {

}
