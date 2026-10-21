package com.scypter.smaintenance.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class MaintenanceEnableEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String by;
    private long durationMillis;
    private String reason;
    private boolean cancelled;

    public MaintenanceEnableEvent(String by, long durationMillis, String reason) {
        this.by = by;
        this.durationMillis = durationMillis;
        this.reason = reason;
    }

    public String getBy() {
        return by;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
