package com.scypter.smaintenance.core.luckperms;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class LuckPermsExtras implements Extras {
    private final LuckPermsHook hook;

    public LuckPermsExtras(BypassGroups groups, BooleanSupplier enabled, Supplier<Collection<String>> scopes) {
        this.hook = new LuckPermsHook(groups);
        this.hook.registerContexts(enabled, scopes);
    }

    public boolean available() { return true; }

    public void preload(UUID id) { hook.preload(id); }

    public boolean inBypassGroup(UUID id) { return hook.inBypassGroup(id); }

    public boolean hasPermission(UUID id, String node) { return hook.hasPermission(id, node); }

    public String prefix(UUID id) { return hook.prefix(id); }

    public String suffix(UUID id) { return hook.suffix(id); }

    public CompletableFuture<UUID> lookup(String name) { return hook.lookup(name); }

    public void contextChanged(Object subject) {
        try {
            hook.signalContextUpdate(subject);
        } catch (Exception ignored) {
        }
    }

    public void close() { hook.unregister(); }
}
