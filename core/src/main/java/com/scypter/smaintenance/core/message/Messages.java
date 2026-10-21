package com.scypter.smaintenance.core.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class Messages {
    private final Map<String, String> templates;
    private final String prefix;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public Messages(Map<String, String> templates, String prefix) {
        this.templates = templates;
        this.prefix = prefix == null ? "" : prefix;
    }

    public static Map<String, String> ph(String... kv) {
        Map<String, String> out = new HashMap<String, String>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out.put(kv[i], kv[i + 1]);
        }
        return out;
    }

    public Component get(String key, Map<String, String> placeholders, UnaryOperator<String> pre) {
        String template = templates.get(key);
        if (template == null) {
            return missing(key);
        }
        return mini.deserialize(prefix + fill(pre.apply(template), placeholders));
    }

    public Component raw(String key, Map<String, String> placeholders, UnaryOperator<String> pre) {
        String template = templates.get(key);
        if (template == null) {
            return missing(key);
        }
        return mini.deserialize(fill(pre.apply(template), placeholders));
    }

    public Component get(String key, Map<String, String> placeholders) {
        String template = templates.get(key);
        if (template == null) {
            return missing(key);
        }
        return mini.deserialize(prefix + fill(template, placeholders));
    }

    public Component raw(String key, Map<String, String> placeholders) {
        String template = templates.get(key);
        if (template == null) {
            return missing(key);
        }
        return mini.deserialize(fill(template, placeholders));
    }

    public String plain(String key, String fallback) {
        String template = templates.get(key);
        if (template == null) {
            return fallback;
        }
        return mini.stripTags(template);
    }

    private Component missing(String key) {
        return Component.text("missing message: " + key, NamedTextColor.RED);
    }

    private String fill(String template, Map<String, String> placeholders) {
        String out = template;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            String value = e.getValue() == null ? "" : e.getValue();
            out = out.replace("{" + e.getKey() + "}", mini.escapeTags(value));
        }
        return out;
    }
}
