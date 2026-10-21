package com.scypter.smaintenance.core.whitelist;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Whitelist {
    private final Map<String, String> backing;

    public Whitelist(Map<String, String> backing) {
        this.backing = backing;
    }

    public synchronized boolean add(UUID id, String name) {
        String key = id.toString();
        if (backing.containsKey(key)) {
            return false;
        }
        backing.put(key, name);
        return true;
    }

    public synchronized boolean remove(UUID id) {
        return backing.remove(id.toString()) != null;
    }

    public synchronized UUID findByName(String name) {
        for (Map.Entry<String, String> e : backing.entrySet()) {
            if (e.getValue() != null && e.getValue().equalsIgnoreCase(name)) {
                return UUID.fromString(e.getKey());
            }
        }
        return null;
    }

    public synchronized UUID removeByName(String name) {
        UUID id = findByName(name);
        if (id != null) {
            backing.remove(id.toString());
        }
        return id;
    }

    public synchronized boolean contains(UUID id) {
        return backing.containsKey(id.toString());
    }

    public synchronized Map<UUID, String> entries() {
        Map<UUID, String> out = new LinkedHashMap<UUID, String>();
        for (Map.Entry<String, String> e : backing.entrySet()) {
            out.put(UUID.fromString(e.getKey()), e.getValue());
        }
        return out;
    }

    public synchronized void clear() {
        backing.clear();
    }

    public synchronized int size() {
        return backing.size();
    }
}
