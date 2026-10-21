package com.scypter.smaintenance.core.schedule;

import com.scypter.smaintenance.core.time.TimeParser;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class RecurringSchedule {
    public final String scope;
    public final int dayOfWeek;
    public final int hour;
    public final int minute;
    public final long durationMillis;
    public final String reason;

    private RecurringSchedule(String scope, int dayOfWeek, int hour, int minute, long durationMillis, String reason) {
        this.scope = scope;
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
        this.minute = minute;
        this.durationMillis = durationMillis;
        this.reason = reason;
    }

    public static RecurringSchedule parse(String line, String defaultScope, boolean withScope) {
        String[] t = line.trim().split("\\s+");
        int i = 0;
        String scope = defaultScope;
        if (withScope) {
            if (t.length < 4) {
                throw new IllegalArgumentException("expected: <scope> <day> HH:mm <duration> [reason]");
            }
            scope = t[i++].toLowerCase(Locale.ROOT);
        } else if (t.length < 3) {
            throw new IllegalArgumentException("expected: <day> HH:mm <duration> [reason]");
        }
        int day = day(t[i++]);
        String[] hm = t[i++].split(":");
        if (hm.length != 2) {
            throw new IllegalArgumentException("bad time: " + line);
        }
        int hour;
        int minute;
        try {
            hour = Integer.parseInt(hm[0]);
            minute = Integer.parseInt(hm[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad time: " + line);
        }
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException("bad time: " + line);
        }
        long duration = TimeParser.parseMillis(t[i++]);
        StringBuilder reason = new StringBuilder();
        for (; i < t.length; i++) {
            if (reason.length() > 0) reason.append(' ');
            reason.append(t[i]);
        }
        return new RecurringSchedule(scope, day, hour, minute, duration, reason.toString());
    }

    private static int day(String s) {
        String d = s.toLowerCase(Locale.ROOT);
        if (d.equals("daily")) return 0;
        if (d.equals("mon")) return Calendar.MONDAY;
        if (d.equals("tue")) return Calendar.TUESDAY;
        if (d.equals("wed")) return Calendar.WEDNESDAY;
        if (d.equals("thu")) return Calendar.THURSDAY;
        if (d.equals("fri")) return Calendar.FRIDAY;
        if (d.equals("sat")) return Calendar.SATURDAY;
        if (d.equals("sun")) return Calendar.SUNDAY;
        throw new IllegalArgumentException("bad day: " + s);
    }

    public long nextStart(long nowMillis, TimeZone tz) {
        for (int offset = 0; offset <= 8; offset++) {
            Calendar c = Calendar.getInstance(tz);
            c.setTimeInMillis(nowMillis);
            c.add(Calendar.DAY_OF_MONTH, offset);
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, minute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            boolean dayOk = dayOfWeek == 0 || c.get(Calendar.DAY_OF_WEEK) == dayOfWeek;
            if (dayOk && c.getTimeInMillis() > nowMillis) {
                return c.getTimeInMillis();
            }
        }
        throw new IllegalStateException("no next start");
    }
}
