package com.scypter.smaintenance.core.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;

public final class StateStore {
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public StateStore(Path file) {
        this.file = file;
    }

    public StoredData load() {
        try {
            if (!Files.exists(file)) {
                return new StoredData();
            }
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            StoredData data = gson.fromJson(json, StoredData.class);
            if (data == null) {
                return new StoredData();
            }
            if (data.state == null) data.state = new MaintenanceState();
            if (data.whitelist == null) data.whitelist = new LinkedHashMap<String, String>();
            return data;
        } catch (Exception e) {
            return new StoredData();
        }
    }

    public synchronized void save(StoredData data) {
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
