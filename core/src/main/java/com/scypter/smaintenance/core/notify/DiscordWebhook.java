package com.scypter.smaintenance.core.notify;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DiscordWebhook {
    private DiscordWebhook() {
    }

    public static String buildJson(String title, String description, int color, String roleId) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", description);
        embed.addProperty("color", color);
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        JsonObject root = new JsonObject();
        if (roleId != null && !roleId.trim().isEmpty()) {
            root.addProperty("content", "<@&" + roleId.trim() + ">");
            JsonObject mentions = new JsonObject();
            JsonArray roles = new JsonArray();
            roles.add(new com.google.gson.JsonPrimitive(roleId.trim()));
            mentions.add("roles", roles);
            root.add("allowed_mentions", mentions);
        }
        root.add("embeds", embeds);
        return root.toString();
    }

    public static void post(String url, String json) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("User-Agent", "sMaintenance");
        OutputStream out = c.getOutputStream();
        try {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("discord returned HTTP " + code);
        }
    }
}
