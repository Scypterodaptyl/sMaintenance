package com.scypter.smaintenance.core;

import com.scypter.smaintenance.core.state.MaintenanceState;

public final class MaintenanceManager {
    private final TimeSource time;
    private final MaintenanceState state;
    private final MaintenanceListener listener;
    private final long[] warningSeconds;
    private long lastWarnedSeconds = Long.MAX_VALUE;

    public MaintenanceManager(TimeSource time, MaintenanceState state, MaintenanceListener listener, long[] warningSeconds) {
        this.time = time;
        this.state = state;
        this.listener = listener;
        this.warningSeconds = warningSeconds;
    }

    public synchronized boolean enable(String by, long durationMillis, String reason) {
        if (state.isEnabled()) {
            return false;
        }
        applyEnable(by, durationMillis, reason);
        return true;
    }

    private void applyEnable(String by, long durationMillis, String reason) {
        state.setEnabled(true);
        state.setEnabledBy(by);
        state.setReason(reason);
        state.setEndAt(durationMillis > 0 ? time.now() + durationMillis : 0);
        listener.onEnabled(state.copy());
        listener.onChanged();
    }

    public synchronized boolean disable(String by) {
        if (!state.isEnabled()) {
            return false;
        }
        applyDisable(by, false);
        return true;
    }

    private void applyDisable(String by, boolean automatic) {
        state.setEnabled(false);
        state.setEndAt(0);
        listener.onDisabled(state.copy(), automatic, by);
        listener.onChanged();
    }

    public synchronized boolean schedule(String by, long delayMillis, long durationMillis, String reason) {
        if (delayMillis <= 0) {
            return false;
        }
        state.setScheduledStart(time.now() + delayMillis);
        state.setScheduledDuration(durationMillis);
        state.setScheduledReason(reason);
        state.setScheduledBy(by);
        lastWarnedSeconds = Long.MAX_VALUE;
        listener.onScheduled(state.copy());
        listener.onChanged();
        return true;
    }

    public synchronized boolean cancelSchedule() {
        if (state.getScheduledStart() <= 0) {
            return false;
        }
        clearSchedule();
        listener.onScheduleCancelled();
        listener.onChanged();
        return true;
    }

    private void clearSchedule() {
        state.setScheduledStart(0);
        state.setScheduledDuration(0);
        state.setScheduledReason(null);
        state.setScheduledBy(null);
    }

    public synchronized boolean extend(long millis) {
        if (!state.isEnabled() || state.getEndAt() <= 0 || millis <= 0) {
            return false;
        }
        long next = state.getEndAt() + millis;
        if (next < state.getEndAt()) {
            return false;
        }
        state.setEndAt(next);
        listener.onChanged();
        return true;
    }

    public synchronized void tick() {
        long now = time.now();
        if (state.isEnabled() && state.getEndAt() > 0 && now >= state.getEndAt()) {
            applyDisable(null, true);
        }
        long start = state.getScheduledStart();
        if (start > 0) {
            long left = start - now;
            if (left <= 0) {
                String by = state.getScheduledBy();
                String reason = state.getScheduledReason();
                long duration = state.getScheduledDuration();
                clearSchedule();
                if (!state.isEnabled()) {
                    applyEnable(by, duration, reason);
                } else {
                    listener.onChanged();
                }
            } else {
                warn(left);
            }
        }
    }

    private void warn(long leftMillis) {
        long leftSeconds = (leftMillis + 999) / 1000;
        long best = -1;
        for (long mark : warningSeconds) {
            if (leftSeconds <= mark && mark < lastWarnedSeconds && (best < 0 || mark < best)) {
                best = mark;
            }
        }
        if (best >= 0) {
            lastWarnedSeconds = best;
            listener.onWarning(leftMillis);
        }
    }

    public synchronized MaintenanceState snapshot() {
        return state.copy();
    }

    public synchronized boolean isEnabled() {
        return state.isEnabled();
    }

    public synchronized long timeLeft() {
        if (!state.isEnabled() || state.getEndAt() <= 0) {
            return 0;
        }
        return Math.max(0, state.getEndAt() - time.now());
    }

    public synchronized long startsIn() {
        if (state.getScheduledStart() <= 0) {
            return 0;
        }
        return Math.max(0, state.getScheduledStart() - time.now());
    }
}
