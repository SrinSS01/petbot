package me.srin.petbot.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Config {
    @Getter
    private long trainingSeconds;
    @Getter
    private String[] species;
    @Getter
    private int petLimit;
}
