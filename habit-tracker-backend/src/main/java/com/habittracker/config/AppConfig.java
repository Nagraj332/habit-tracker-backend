package com.habittracker.config;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** The time zone that decides what "today" means on the server. */
    @Bean
    public ZoneId appZone(@Value("${app.timezone}") String timezone) {
        return ZoneId.of(timezone);
    }
}
