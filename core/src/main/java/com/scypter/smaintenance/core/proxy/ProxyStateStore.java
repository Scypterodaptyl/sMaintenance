package com.scypter.smaintenance.core.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;

public final class ProxyStateStore {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ProxyStateStore(Path file) {
        this.file = file;
    }

    public ProxyData load() {
        try {
            if (!Files.exists(file)) {
                return new ProxyData();
            }
            ProxyData data = gson.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8), ProxyData.class);
            if (data == null) {
                return new ProxyData();
            }
            if (data.states == null) data.states = new LinkedHashMap<String, com.scypter.smaintenance.core.state.MaintenanceState>();
            if (data.whitelists == null) data.whitelists = new LinkedHashMap<String, java.util.Map<String, String>>();
            return data;
        } catch (Exception e) {
            return new ProxyData();
        }
    }

    public synchronized void save(ProxyData data) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, gson.toJson(data).getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("cannot save " + file, e);
        }
    }
}
