package com.scypter.smaintenance.core.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class NoExtras implements Extras {
    public boolean available() { return false; }

    public void preload(UUID id) { }

    public boolean inBypassGroup(UUID id) { return false; }

    public boolean hasPermission(UUID id, String node) { return false; }

    public String prefix(UUID id) { return ""; }

    public String suffix(UUID id) { return ""; }

    public CompletableFuture<UUID> lookup(String name) { return null; }

    public void contextChanged(Object subject) { }

    public void close() { }
}
