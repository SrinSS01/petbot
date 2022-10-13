package me.srin.petbot.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties("bot")
@Getter @Setter
public class Config {
    private String token;
    private String[] petSpecies;
    private long trainingCooldownInSeconds;
    private int petLimit;
}
