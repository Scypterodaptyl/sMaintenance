package com.scypter.smaintenance.core.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateChecker {
    private UpdateChecker() {
    }

    public static String fetchLatest(String slug) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/" + slug + "/version").openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.setRequestProperty("User-Agent", "sMaintenance");
            if (c.getResponseCode() != 200) {
                return null;
            }
            JsonArray arr = new JsonParser().parse(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8)).getAsJsonArray();
            if (arr.size() == 0) {
                return null;
            }
            return arr.get(0).getAsJsonObject().get("version_number").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isNewer(String current, String latest) {
        if (current == null || latest == null) {
            return false;
        }
        int[] a = numbers(current);
        int[] b = numbers(latest);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (y != x) {
                return y > x;
            }
        }
        return false;
    }

    private static int[] numbers(String v) {
        String[] parts = v.replaceAll("[^0-9.]", "").split("[.]");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                out[i] = 0;
            }
        }
        return out;
    }
}
