package com.scypter.smaintenance.core.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FlatConfig implements ConfigView {
    private final Map<String, String> values = new LinkedHashMap<String, String>();

    public static FlatConfig parse(String text) {
        FlatConfig c = new FlatConfig();
        for (String line : text.split("\r?\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            int idx = t.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String key = t.substring(0, idx).trim();
            String raw = t.substring(idx + 1).trim();
            if (raw.isEmpty()) {
                continue;
            }
            c.values.put(key, raw);
        }
        return c;
    }

    public static FlatConfig read(InputStream in) throws IOException {
        if (in == null) {
            return new FlatConfig();
        }
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return parse(sb.toString());
        } finally {
            in.close();
        }
    }

    public void putAll(FlatConfig other) {
        values.putAll(other.values);
    }

    public String getString(String key, String def) {
        String raw = values.get(key);
        return raw == null ? def : unquote(raw);
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(getString(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        String v = getString(key, String.valueOf(def)).trim();
        if (v.equalsIgnoreCase("true")) return true;
        if (v.equalsIgnoreCase("false")) return false;
        return def;
    }

    public List<String> getList(String key) {
        List<String> out = new ArrayList<String>();
        String raw = values.get(key);
        if (raw == null) {
            return out;
        }
        String t = raw.trim();
        if (t.startsWith("[") && t.endsWith("]")) {
            t = t.substring(1, t.length() - 1);
        }
        for (String part : t.split(",")) {
            String p = unquote(part.trim());
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    public Map<String, String> strings() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            out.put(e.getKey(), unquote(e.getValue()));
        }
        return out;
    }

    private static String unquote(String raw) {
        String t = raw.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            String body = t.substring(1, t.length() - 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < body.length(); i++) {
                char c = body.charAt(i);
                if (c == '\\' && i + 1 < body.length()) {
                    char n = body.charAt(++i);
                    if (n == 'n') sb.append('\n');
                    else sb.append(n);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
        if (t.length() >= 2 && t.startsWith("'") && t.endsWith("'")) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }
}
