package com.scypter.smaintenance.core.proxy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerQueue {
    private final Map<String, Set<UUID>> queues = new ConcurrentHashMap<String, Set<UUID>>();

    public void add(String scope, UUID id) {
        String key = scope.toLowerCase(Locale.ROOT);
        Set<UUID> set = queues.get(key);
        if (set == null) {
            set = java.util.Collections.synchronizedSet(new LinkedHashSet<UUID>());
            Set<UUID> prev = queues.putIfAbsent(key, set);
            if (prev != null) {
                set = prev;
            }
        }
        set.add(id);
    }

    public List<UUID> drain(String scope) {
        Set<UUID> set = queues.remove(scope.toLowerCase(Locale.ROOT));
        if (set == null) {
            return new ArrayList<UUID>();
        }
        synchronized (set) {
            return new ArrayList<UUID>(set);
        }
    }

    public void remove(UUID id) {
        for (Set<UUID> s : queues.values()) {
            s.remove(id);
        }
    }
}
