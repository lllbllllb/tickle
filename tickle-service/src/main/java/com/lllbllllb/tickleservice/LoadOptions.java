package com.lllbllllb.tickleservice;

import lombok.With;

public record LoadOptions(

    @With
    int rps,

    boolean stopWhenDisconnect,

    int loadTimeSec
) {

}
