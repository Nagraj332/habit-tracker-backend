package com.habittracker.config;

import java.util.regex.Pattern;

/** Validates the X-Owner-Id header value (a random id the browser generates for itself). */
public final class OwnerIds {

    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{16,64}$");

    private OwnerIds() {}

    public static String require(String raw) {
        String id = raw == null ? "" : raw.trim();
        if (!VALID.matcher(id).matches()) {
            throw new IllegalArgumentException("Missing or invalid X-Owner-Id header");
        }
        return id;
    }
}
