package com.habittracker.config;

import java.util.regex.Pattern;

import com.habittracker.exception.BadRequestException;

/** Reads the X-Owner-Id header: a random id the app creates once per install/browser. */
public final class Owner {

    public static final String HEADER = "X-Owner-Id";
    // same rule the frontend uses when you type in another device's code
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{16,64}$");

    private Owner() {}

    public static String require(String raw) {
        String id = raw == null ? "" : raw.trim();
        if (!VALID.matcher(id).matches()) {
            throw new BadRequestException("Missing or invalid " + HEADER + " header.");
        }
        return id;
    }
}
