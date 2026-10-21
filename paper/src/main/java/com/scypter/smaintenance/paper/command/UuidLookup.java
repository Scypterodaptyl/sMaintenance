package com.scypter.smaintenance.paper.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.scypter.smaintenance.paper.PaperRuntime;
import org.bukkit.OfflinePlayer;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class UuidLookup {
    public interface Callback {
        void done(UUID id, String exactName);
    }

    private UuidLookup() {
    }

    @SuppressWarnings("deprecation")
    public static void resolve(final PaperRuntime rt, final String name, final Callback cb) {
        rt.tasks.runMain(new Runnable() {
            public void run() {
                OfflinePlayer op = rt.plugin.getServer().getOfflinePlayer(name);
                if (op.hasPlayedBefore() || op.isOnline()) {
                    cb.done(op.getUniqueId(), op.getName() == null ? name : op.getName());
                    return;
                }
                rt.tasks.runAsync(new Runnable() {
                    public void run() {
                        UUID id = null;
                        try {
                            if (rt.extras.available()) {
                                java.util.concurrent.CompletableFuture<UUID> f = rt.extras.lookup(name);
                                id = f == null ? null : f.get(5, java.util.concurrent.TimeUnit.SECONDS);
                            }
                        } catch (Exception ignored) {
                        }
                        cb.done(id != null ? id : fromMojang(name), name);
                    }
                });
            }
        });
    }

    private static UUID fromMojang(String name) {
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
