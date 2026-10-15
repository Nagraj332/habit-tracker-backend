package com.habittracker;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HabitTrackerApplication {
    public static void main(String[] args) {
        // The server runs in UTC by default, which makes "today" start 5.5 hours late for users in India.
        // Habits, streaks and check-ins use this time zone; set APP_TIMEZONE on Render to use another one.
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata")));
        SpringApplication.run(HabitTrackerApplication.class, args);
    }
}
