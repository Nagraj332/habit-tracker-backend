package com.habittracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (the reminder alarm job uses this). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
