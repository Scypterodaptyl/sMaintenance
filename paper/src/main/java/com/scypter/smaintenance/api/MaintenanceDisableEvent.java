package com.scypter.smaintenance.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class MaintenanceDisableEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String by;
    private boolean cancelled;

    public MaintenanceDisableEvent(String by) {
        this.by = by;
    }

    public String getBy() {
        return by;
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
