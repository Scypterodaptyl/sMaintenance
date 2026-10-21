package com.scypter.smaintenance.paper.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.time.TimeFormatter;
import com.scypter.smaintenance.core.time.TimeParser;
import com.scypter.smaintenance.paper.PaperRuntime;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MaintCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUB = Arrays.asList("on", "off", "status", "schedule", "cancel", "extend", "whitelist", "history", "gui", "reload");
    private static final List<String> WL = Arrays.asList("add", "remove", "list", "clear", "import", "addgroup", "removegroup");
    private static final List<String> TIMES = Arrays.asList("10m", "30m", "1h", "2h", "1d");

    private final PaperRuntime rt;

    public MaintCommand(PaperRuntime rt) {
        this.rt = rt;
    }

    private void send(CommandSender to, String key, Map<String, String> ph) {
        rt.text.message(to, rt.messages.get(key, ph));
    }

    private boolean can(CommandSender s, String node) {
        if (s.hasPermission("smaintenance.command." + node)) {
            return true;
        }
        send(s, "no-permission", Messages.ph());
        return false;
    }

    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            send(s, "usage", Messages.ph());
            return true;
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        try {
            if (sub.equals("on")) return on(s, a);
            if (sub.equals("off")) return off(s);
            if (sub.equals("status")) return status(s);
            if (sub.equals("schedule")) return schedule(s, a);
            if (sub.equals("cancel")) return cancel(s);
            if (sub.equals("extend")) return extend(s, a);
            if (sub.equals("whitelist")) return whitelist(s, a);
            if (sub.equals("reload")) return reload(s);
            if (sub.equals("history")) return history(s, a);
            if (sub.equals("gui")) return gui(s);
        } catch (IllegalArgumentException e) {
            send(s, "invalid-time", Messages.ph());
            return true;
        }
        send(s, "usage", Messages.ph());
        return true;
    }

    private boolean isTime(String t) {
        try {
            TimeParser.parseMillis(t);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String join(String[] a, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < a.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private boolean on(CommandSender s, String[] a) {
        if (!can(s, "toggle")) return true;
        long duration = 0;
        int reasonFrom = 1;
        if (a.length > 1 && isTime(a[1])) {
            duration = TimeParser.parseMillis(a[1]);
            reasonFrom = 2;
        } else if (a.length > 1 && a[1].matches("[-0-9].*")) {
            TimeParser.parseMillis(a[1]);
        }
        if (rt.manager.isEnabled()) {
            send(s, "already-enabled", Messages.ph());
        } else if (!rt.tryEnable(s.getName(), duration, join(a, reasonFrom))) {
            send(s, "action-cancelled", Messages.ph());
        }
        return true;
    }

    private boolean off(CommandSender s) {
        if (!can(s, "toggle")) return true;
        if (!rt.manager.isEnabled()) {
            send(s, "status-off", Messages.ph());
        } else if (!rt.tryDisable(s.getName())) {
            send(s, "action-cancelled", Messages.ph());
        }
        return true;
    }

    private boolean status(CommandSender s) {
        if (!can(s, "status")) return true;
        MaintenanceState st = rt.manager.snapshot();
        send(s, st.isEnabled() ? "status-on" : "status-off", rt.placeholders(st));
        if (rt.manager.startsIn() > 0) {
            send(s, "scheduled", Messages.ph("time_left", TimeFormatter.format(rt.manager.startsIn()),
                    "reason", st.getScheduledReason() == null ? "" : st.getScheduledReason()));
        }
        return true;
    }

    private boolean schedule(CommandSender s, String[] a) {
        if (!can(s, "schedule")) return true;
        if (a.length < 2) {
            send(s, "usage", Messages.ph());
            return true;
        }
        long delay = TimeParser.parseMillis(a[1]);
        long duration = 0;
        int reasonFrom = 2;
        if (a.length > 2 && isTime(a[2])) {
            duration = TimeParser.parseMillis(a[2]);
            reasonFrom = 3;
        }
        String reason = join(a, reasonFrom);
        rt.manager.schedule(s.getName(), delay, duration, reason);
        send(s, "scheduled", Messages.ph("time_left", TimeFormatter.format(delay), "reason", reason));
        return true;
    }

    private boolean cancel(CommandSender s) {
        if (!can(s, "schedule")) return true;
        if (!rt.manager.cancelSchedule()) {
            send(s, "not-enabled", Messages.ph());
        } else {
            send(s, "schedule-cancelled", Messages.ph());
        }
        return true;
    }

    private boolean extend(CommandSender s, String[] a) {
        if (!can(s, "toggle")) return true;
        if (a.length < 2) {
            send(s, "usage", Messages.ph());
            return true;
        }
        long add = TimeParser.parseMillis(a[1]);
        if (rt.manager.extend(add)) {
            send(s, "extended", Messages.ph("time_left", TimeFormatter.format(rt.manager.timeLeft())));
        } else {
            send(s, "not-enabled", Messages.ph());
        }
        return true;
    }

    private boolean history(CommandSender s, String[] a) {
        if (!can(s, "status")) return true;
        int n = 5;
        if (a.length > 1) {
            try {
                n = Math.max(1, Math.min(20, Integer.parseInt(a[1])));
            } catch (NumberFormatException ignored) {
            }
        }
        List<com.scypter.smaintenance.core.history.HistoryEntry> entries = rt.history.last(n);
        if (entries.isEmpty()) {
            send(s, "history-empty", Messages.ph());
            return true;
        }
        for (com.scypter.smaintenance.core.history.HistoryEntry e : entries) {
            send(s, "history-line", Messages.ph("time", new java.util.Date(e.at).toString(), "server", String.valueOf(e.scope),
                    "enabled_by", String.valueOf(e.by), "reason", e.reason == null || e.reason.isEmpty() ? "-" : e.reason,
                    "time_left", e.durationMs > 0 ? TimeFormatter.format(e.durationMs) : "-"));
        }
        return true;
    }

    private boolean gui(CommandSender s) {
        if (!(s instanceof Player)) {
            return true;
        }
        if (!can(s, "toggle")) return true;
        rt.gui.open((Player) s);
        return true;
    }

    private boolean reload(CommandSender s) {
        if (!can(s, "reload")) return true;
        rt.plugin.reloadConfig();
        rt.reloadMessages();
        send(s, "reloaded", Messages.ph());
        return true;
    }

    private boolean whitelist(final CommandSender s, String[] a) {
        if (!can(s, "whitelist")) return true;
        if (a.length < 2) {
            send(s, "usage", Messages.ph());
            return true;
        }
        String op = a[1].toLowerCase(Locale.ROOT);
        if (op.equals("list")) {
            List<String> names = new ArrayList<String>(rt.whitelist.entries().values());
            StringBuilder sb = new StringBuilder();
            for (String n : names) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(n);
            }
            send(s, "whitelist-list", Messages.ph("count", String.valueOf(names.size()), "list", names.isEmpty() ? "-" : sb.toString()));
            return true;
        }
        if (op.equals("clear")) {
            rt.whitelist.clear();
            rt.saveAsync();
            send(s, "whitelist-cleared", Messages.ph());
            return true;
        }
        if (op.equals("import")) {
            rt.tasks.runAsync(new Runnable() {
                public void run() {
                    final int count = importVanilla();
                    rt.tasks.runMain(new Runnable() {
                        public void run() {
                            send(s, "whitelist-imported", Messages.ph("count", String.valueOf(count)));
                        }
                    });
                }
            });
            return true;
        }
        if (a.length < 3) {
            send(s, "usage", Messages.ph());
            return true;
        }
        if (op.equals("addgroup") || op.equals("removegroup")) {
            return group(s, op, a[2]);
        }
        final String name = a[2];
        if (op.equals("remove")) {
            if (rt.whitelist.removeByName(name) != null) {
                rt.saveAsync();
                send(s, "whitelist-removed", Messages.ph("player", name));
            } else {
                send(s, "whitelist-missing", Messages.ph("player", name));
            }
            return true;
        }
        if (op.equals("add")) {
            UuidLookup.resolve(rt, name, new UuidLookup.Callback() {
                public void done(final UUID id, final String exact) {
                    rt.tasks.runMain(new Runnable() {
                        public void run() {
                            if (id == null) {
                                send(s, "whitelist-unknown-player", Messages.ph("player", name));
                                return;
                            }
                            rt.whitelist.add(id, exact);
                            rt.saveAsync();
                            send(s, "whitelist-added", Messages.ph("player", exact));
                        }
                    });
                }
            });
            return true;
        }
        send(s, "usage", Messages.ph());
        return true;
    }

    private boolean group(CommandSender s, String op, String g) {
        if (!rt.extras.available()) {
            send(s, "luckperms-missing", Messages.ph());
        } else if (op.equals("addgroup")) {
            rt.groups.add(g);
            send(s, "group-added", Messages.ph("group", g));
        } else if (rt.groups.remove(g)) {
            send(s, "group-removed", Messages.ph("group", g));
        } else {
            send(s, "group-missing", Messages.ph("group", g));
        }
        return true;
    }

    private int importVanilla() {
        File f = new File(rt.plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile(), "whitelist.json");
        if (!f.isFile()) {
            return 0;
        }
        int added = 0;
        try {
            JsonArray arr = new JsonParser().parse(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                if (rt.whitelist.add(UUID.fromString(o.get("uuid").getAsString()), o.get("name").getAsString())) {
                    added++;
                }
            }
            rt.saveAsync();
        } catch (Exception e) {
            rt.plugin.getLogger().warning("whitelist import failed: " + e.getMessage());
        }
        return added;
    }

    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<String>();
        String first = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "";
        if (a.length == 1) {
            filter(out, SUB, a[0]);
        } else if (a.length == 2 && first.equals("whitelist")) {
            filter(out, WL, a[1]);
        } else if (a.length == 2 && (first.equals("on") || first.equals("schedule") || first.equals("extend"))) {
            filter(out, TIMES, a[1]);
        } else if (a.length == 3 && first.equals("schedule")) {
            filter(out, TIMES, a[2]);
        } else if (a.length == 3 && first.equals("whitelist") && a[1].equalsIgnoreCase("remove")) {
            filter(out, new ArrayList<String>(rt.whitelist.entries().values()), a[2]);
        } else if (a.length == 3 && first.equals("whitelist") && a[1].equalsIgnoreCase("add")) {
            for (Player p : rt.plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[2].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }

    private void filter(List<String> out, List<String> options, String prefix) {
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                out.add(o);
            }
        }
    }
}
