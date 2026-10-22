package com.scypter.smaintenance.core.time;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {
    private static final Pattern FULL = Pattern.compile("([0-9]+[smhd])+");
    private static final Pattern PART = Pattern.compile("([0-9]+)([smhd])");

    public static final long MAX_MILLIS = 3650L * 86_400_000L;

    private TimeParser() {
    }

    public static long parseMillis(String input) {
        if (input == null) {
            throw new IllegalArgumentException("time is empty");
        }
        String text = input.trim().toLowerCase(Locale.ROOT);
        if (!FULL.matcher(text).matches()) {
            throw new IllegalArgumentException("invalid time: " + input);
        }
        long total = 0;
        Matcher m = PART.matcher(text);
        while (m.find()) {
            try {
                total = Math.addExact(total, Math.multiplyExact(Long.parseLong(m.group(1)), unit(m.group(2).charAt(0))));
            } catch (ArithmeticException | NumberFormatException e) {
                throw new IllegalArgumentException("time too large: " + input);
            }
        }
        if (total > MAX_MILLIS) {
            throw new IllegalArgumentException("time too large: " + input);
        }
        if (total <= 0) {
            throw new IllegalArgumentException("time must be positive: " + input);
        }
        return total;
    }

    private static long unit(char c) {
        switch (c) {
            case 's': return 1_000L;
            case 'm': return 60_000L;
            case 'h': return 3_600_000L;
            default: return 86_400_000L;
        }
    }
}
