package com.scypter.smaintenance.core.luckperms;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Extras {
    boolean available();

    void preload(UUID id);

    boolean inBypassGroup(UUID id);

    String prefix(UUID id);

    String suffix(UUID id);

    CompletableFuture<UUID> lookup(String name);

    void contextChanged(Object subject);

    void close();
}
