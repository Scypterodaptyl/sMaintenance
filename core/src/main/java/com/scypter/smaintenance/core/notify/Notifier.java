package com.scypter.smaintenance.core.notify;

import com.scypter.smaintenance.core.config.ConfigView;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.time.TimeFormatter;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Notifier {
    private final Executor async;
    private final Consumer<String> warn;
    private final AuditLog log;
    private final Supplier<ConfigView> config;
    private final Supplier<Messages> messages;

    public Notifier(Executor async, Consumer<String> warn, AuditLog log, Supplier<ConfigView> config) {
        this(async, warn, log, config, null);
    }

    public Notifier(Executor async, Consumer<String> warn, AuditLog log, Supplier<ConfigView> config, Supplier<Messages> messages) {
        this.async = async;
        this.warn = warn;
        this.log = log;
        this.config = config;
        this.messages = messages;
    }

    private String t(String key, String fallback) {
        Messages m = messages == null ? null : messages.get();
        return m == null ? fallback : m.plain(key, fallback);
    }

    private String field(String key, String fallback, String value) {
        return t(key, fallback) + ": " + value;
    }

    public void enabled(String scope, MaintenanceState s) {
        String duration = s.getEndAt() > 0 ? TimeFormatter.format(s.getEndAt() - System.currentTimeMillis()) : "-";
        String line = "ENABLED scope=" + scope + " by=" + s.getEnabledBy() + " duration=" + duration + " reason=" + s.getReason();
        record(line);
        embed("discord-color-on", t("discord-title-enabled", "Maintenance enabled"),
                field("discord-field-server", "Server", scope) + "\n"
                        + field("discord-field-by", "Enabled by", String.valueOf(s.getEnabledBy())) + "\n"
                        + field("discord-field-duration", "Duration", duration) + "\n"
                        + field("discord-field-reason", "Reason", reason(s.getReason())));
    }

    public void disabled(String scope, boolean automatic, String by) {
        record("DISABLED scope=" + scope + " by=" + (automatic ? "timer" : by));
        embed("discord-color-off", t("discord-title-disabled", "Maintenance disabled"),
                field("discord-field-server", "Server", scope) + "\n"
                        + field("discord-field-by-off", "Disabled by", automatic ? t("discord-timer", "timer") : String.valueOf(by)));
    }

    public void scheduled(String scope, MaintenanceState s) {
        long in = s.getScheduledStart() - System.currentTimeMillis();
        record("SCHEDULED scope=" + scope + " by=" + s.getScheduledBy() + " in=" + TimeFormatter.format(in) + " reason=" + s.getScheduledReason());
        embed("discord-color-schedule", t("discord-title-scheduled", "Maintenance scheduled"),
                field("discord-field-server", "Server", scope) + "\n"
                        + field("discord-field-starts-in", "Starts in", TimeFormatter.format(in)) + "\n"
                        + field("discord-field-reason", "Reason", reason(s.getScheduledReason())));
    }

    private String reason(String r) {
        return r == null || r.isEmpty() ? "-" : r;
    }

    private void record(String line) {
        log.write(line);
    }

    private void embed(String colorKey, String title, String description) {
        ConfigView c = config.get();
        if (!c.getBoolean("discord-enabled", false)) {
            return;
        }
        final String url = c.getString("discord-url", "");
        if (url.isEmpty()) {
            return;
        }
        int color = parseColor(c.getString(colorKey, "0x2ecc71"));
        String heading = c.getString("discord-title", "sMaintenance") + ": " + title;
        final String json = DiscordWebhook.buildJson(heading, description, color, c.getString("discord-role-id", ""));
        async.execute(new Runnable() {
            public void run() {
                try {
                    DiscordWebhook.post(url, json);
                } catch (Exception e) {
                    warn.accept("Discord webhook failed: " + e.getMessage());
                }
            }
        });
    }

    static int parseColor(String raw) {
        try {
            String t = raw.trim();
            if (t.startsWith("0x") || t.startsWith("0X")) {
                return Integer.parseInt(t.substring(2), 16);
            }
            if (t.startsWith("#")) {
                return Integer.parseInt(t.substring(1), 16);
            }
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return 0x2ecc71;
        }
    }
}
