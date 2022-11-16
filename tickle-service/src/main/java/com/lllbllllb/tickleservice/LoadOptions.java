package com.lllbllllb.tickleservice;

public record LoadOptions(

    int rps,

    boolean stopWhenDisconnect,

    int loadTimeSec
) {

}
