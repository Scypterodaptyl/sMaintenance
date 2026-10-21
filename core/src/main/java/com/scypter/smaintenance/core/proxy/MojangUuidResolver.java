package com.scypter.smaintenance.core.proxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class MojangUuidResolver implements ProxyCommandHandler.UuidResolver {
    private final Executor executor;
    private final com.scypter.smaintenance.core.luckperms.Extras extras;

    public MojangUuidResolver(Executor executor, com.scypter.smaintenance.core.luckperms.Extras extras) {
        this.executor = executor;
        this.extras = extras;
    }

    public void resolve(final String name, final Callback cb) {
        executor.execute(new Runnable() {
            public void run() {
                UUID id = null;
                try {
                    if (extras.available()) {
                        java.util.concurrent.CompletableFuture<UUID> f = extras.lookup(name);
                        id = f == null ? null : f.get(5, java.util.concurrent.TimeUnit.SECONDS);
                    }
                } catch (Exception ignored) {
                }
                cb.done(id != null ? id : lookup(name), name);
            }
        });
    }

    static UUID lookup(String name) {
        try {
            if (!name.matches("[A-Za-z0-9_]{1,16}")) {
                return null;
            }
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            if (c.getResponseCode() != 200) {
                return null;
            }
            JsonObject o = new JsonParser().parse(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
            String id = o.get("id").getAsString();
            return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20));
        } catch (Exception e) {
            return null;
        }
    }
}
