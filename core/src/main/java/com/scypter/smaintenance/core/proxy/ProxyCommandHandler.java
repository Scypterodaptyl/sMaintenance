package com.scypter.smaintenance.core.proxy;

import com.scypter.smaintenance.core.MaintenanceManager;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.time.TimeFormatter;
import com.scypter.smaintenance.core.time.TimeParser;
import com.scypter.smaintenance.core.luckperms.BypassGroups;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class ProxyCommandHandler {
    public interface CommandSource {
        String name();

        boolean has(String node);

        void send(Component c);
    }

    public interface ServerDirectory {
        Collection<String> names();
    }

    public interface UuidResolver {
        interface Callback {
            void done(UUID id, String exactName);
        }

        void resolve(String name, Callback cb);
    }

    private static final List<String> TOP = Arrays.asList("list", "status", "history", "schedule", "whitelist", "reload", "global");
    private static final List<String> ACTIONS = Arrays.asList("on", "off", "status", "cancel", "extend");
    private static final List<String> WL = Arrays.asList("add", "remove", "list", "clear", "addgroup", "removegroup");
    private static final List<String> TIMES = Arrays.asList("10m", "30m", "1h", "2h", "1d");

    private final ProxyMaintenance pm;
    private final Supplier<Messages> messages;
    private final ServerDirectory servers;
    private final UuidResolver resolver;
    private final Runnable reload;
    private final BypassGroups groups;
    private final java.util.function.BooleanSupplier luckPerms;

    public ProxyCommandHandler(ProxyMaintenance pm, Supplier<Messages> messages, ServerDirectory servers, UuidResolver resolver, Runnable reload, BypassGroups groups, java.util.function.BooleanSupplier luckPerms) {
        this.pm = pm;
        this.messages = messages;
        this.servers = servers;
        this.resolver = resolver;
        this.reload = reload;
        this.groups = groups;
        this.luckPerms = luckPerms;
    }

    private void send(CommandSource s, String key, Map<String, String> ph) {
        s.send(messages.get().get(key, ph));
    }

    private boolean can(CommandSource s, String node) {
        if (s.has("smaintenance.command." + node)) {
            return true;
        }
        send(s, "no-permission", Messages.ph());
        return false;
    }

    private String scopeOf(CommandSource s, String raw) {
        String scope = ProxyMaintenance.normalize(raw);
        if (scope.equals(ProxyMaintenance.GLOBAL)) {
            return scope;
        }
        for (String n : servers.names()) {
            if (n.equalsIgnoreCase(scope)) {
                return scope;
            }
        }
        send(s, "unknown-scope", Messages.ph("server", raw));
        return null;
    }

    public void execute(CommandSource s, String[] a) {
        if (a.length == 0) {
            send(s, "proxy-usage", Messages.ph());
            return;
        }
        boolean scopeFirst = a.length >= 3 && (a[0].equalsIgnoreCase("global") || !TOP.contains(a[0].toLowerCase(Locale.ROOT)));
        if (scopeFirst && a[1].equalsIgnoreCase("whitelist") && a.length >= 4
                && (a[2].equalsIgnoreCase("addgroup") || a[2].equalsIgnoreCase("removegroup"))) {
            if (!a[0].equalsIgnoreCase(ProxyMaintenance.GLOBAL)) {
                send(s, "proxy-usage", Messages.ph());
                return;
            }
            a = new String[]{a[1], a[2], a[3]};
        } else if (scopeFirst && (a[1].equalsIgnoreCase("schedule") || a[1].equalsIgnoreCase("whitelist"))) {
            String[] swapped = a.clone();
            swapped[0] = a[1];
            swapped[1] = a[0];
            a = swapped;
        }
        String first = a[0].toLowerCase(Locale.ROOT);
        try {
            if (first.equals("list")) {
                list(s);
            } else if (first.equals("history")) {
                history(s, a);
            } else if (first.equals("reload")) {
                if (can(s, "reload")) {
                    reload.run();
                    send(s, "reloaded", Messages.ph());
                }
            } else if (first.equals("status")) {
                if (can(s, "status")) {
                    if (a.length > 1) {
                        String scope = scopeOf(s, a[1]);
                        if (scope != null) status(s, scope);
                    } else {
                        list(s);
                    }
                }
            } else if (first.equals("schedule")) {
                if (a.length < 3) {
                    send(s, "proxy-usage", Messages.ph());
                } else if (can(s, "schedule")) {
                    String scope = scopeOf(s, a[1]);
                    if (scope != null) schedule(s, scope, a);
                }
            } else if (first.equals("whitelist")) {
                if (a.length < 3) {
                    send(s, "proxy-usage", Messages.ph());
                } else if (can(s, "whitelist") && groupCommand(s, a)) {
                    return;
                } else if (can(s, "whitelist")) {
                    String scope = scopeOf(s, a[1]);
                    if (scope != null) whitelist(s, scope, a);
                }
            } else if (a.length >= 2) {
                String scope = scopeOf(s, a[0]);
                if (scope != null) action(s, scope, a);
            } else {
                send(s, "proxy-usage", Messages.ph());
            }
        } catch (IllegalArgumentException e) {
            send(s, "invalid-time", Messages.ph());
        }
    }

    private boolean groupCommand(CommandSource s, String[] a) {
        String op = a[1].toLowerCase(Locale.ROOT);
        if (!op.equals("addgroup") && !op.equals("removegroup")) {
            return false;
        }
        if (!luckPerms.getAsBoolean()) {
            send(s, "luckperms-missing", Messages.ph());
            return true;
        }
        String g = a[2];
        if (op.equals("addgroup")) {
            groups.add(g);
            send(s, "group-added", Messages.ph("group", g));
        } else if (groups.remove(g)) {
            send(s, "group-removed", Messages.ph("group", g));
        } else {
            send(s, "group-missing", Messages.ph("group", g));
        }
        return true;
    }

    private void history(CommandSource s, String[] a) {
        if (!can(s, "status")) {
            return;
        }
        int n = 5;
        if (a.length > 1) {
            try {
                n = Math.max(1, Math.min(20, Integer.parseInt(a[1])));
            } catch (NumberFormatException ignored) {
            }
        }
        List<com.scypter.smaintenance.core.history.HistoryEntry> entries = pm.history().last(n);
        if (entries.isEmpty()) {
            send(s, "history-empty", Messages.ph());
            return;
        }
        for (com.scypter.smaintenance.core.history.HistoryEntry e : entries) {
            send(s, "history-line", Messages.ph("time", new java.util.Date(e.at).toString(), "server", String.valueOf(e.scope),
                    "enabled_by", String.valueOf(e.by), "reason", e.reason == null || e.reason.isEmpty() ? "-" : e.reason,
                    "time_left", e.durationMs > 0 ? TimeFormatter.format(e.durationMs) : "-"));
        }
    }

    private void list(CommandSource s) {
        if (!can(s, "status")) {
            return;
        }
        List<String> on = pm.enabledScopes();
        if (on.isEmpty()) {
            send(s, "list-empty", Messages.ph());
            return;
        }
        for (String scope : on) {
            send(s, "list-line", pm.placeholders(scope));
        }
    }

    private void status(CommandSource s, String scope) {
        MaintenanceManager m = pm.manager(scope);
        MaintenanceState st = m.snapshot();
        send(s, st.isEnabled() ? "status-on" : "status-off", pm.placeholders(scope, st));
        if (m.startsIn() > 0) {
            send(s, "scheduled", Messages.ph("time_left", TimeFormatter.format(m.startsIn()),
                    "reason", st.getScheduledReason() == null ? "" : st.getScheduledReason(), "server", scope));
        }
    }

    private void action(CommandSource s, String scope, String[] a) {
        String act = a[1].toLowerCase(Locale.ROOT);
        MaintenanceManager m = pm.manager(scope);
        if (act.equals("status")) {
            if (can(s, "status")) status(s, scope);
        } else if (act.equals("on")) {
            if (!can(s, "toggle")) return;
            long duration = 0;
            int from = 2;
            if (a.length > 2 && isTime(a[2])) {
                duration = TimeParser.parseMillis(a[2]);
                from = 3;
            } else if (a.length > 2 && a[2].matches("[-0-9].*")) {
                TimeParser.parseMillis(a[2]);
            }
            if (!m.enable(s.name(), duration, join(a, from))) {
                send(s, "already-enabled", Messages.ph());
            }
        } else if (act.equals("off")) {
            if (!can(s, "toggle")) return;
            if (!m.disable(s.name())) {
                send(s, "status-off", pm.placeholders(scope));
            }
        } else if (act.equals("cancel")) {
            if (!can(s, "schedule")) return;
            if (m.cancelSchedule()) {
                send(s, "schedule-cancelled", Messages.ph());
            } else {
                send(s, "not-enabled", Messages.ph());
            }
        } else if (act.equals("extend")) {
            if (!can(s, "toggle")) return;
            if (a.length < 3) {
                send(s, "proxy-usage", Messages.ph());
                return;
            }
            if (m.extend(TimeParser.parseMillis(a[2]))) {
                send(s, "extended", Messages.ph("time_left", TimeFormatter.format(m.timeLeft())));
            } else {
                send(s, "not-enabled", Messages.ph());
            }
        } else {
            send(s, "proxy-usage", Messages.ph());
        }
    }

    private void schedule(CommandSource s, String scope, String[] a) {
        long delay = TimeParser.parseMillis(a[2]);
        long duration = 0;
        int from = 3;
        if (a.length > 3 && isTime(a[3])) {
            duration = TimeParser.parseMillis(a[3]);
            from = 4;
        }
        String reason = join(a, from);
        pm.manager(scope).schedule(s.name(), delay, duration, reason);
        send(s, "scheduled", Messages.ph("time_left", TimeFormatter.format(delay), "reason", reason, "server", scope));
    }

    private void whitelist(final CommandSource s, String scope, String[] a) {
        String op = a[2].toLowerCase(Locale.ROOT);
        if (op.equals("list")) {
            List<String> names = new ArrayList<String>(pm.whitelist(scope).entries().values());
            StringBuilder sb = new StringBuilder();
            for (String n : names) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(n);
            }
            send(s, "whitelist-list", Messages.ph("count", String.valueOf(names.size()), "list", names.isEmpty() ? "-" : sb.toString()));
        } else if (op.equals("clear")) {
            pm.whitelist(scope).clear();
            changed();
            send(s, "whitelist-cleared", Messages.ph());
        } else if (a.length < 4) {
            send(s, "proxy-usage", Messages.ph());
        } else if (op.equals("remove")) {
            if (pm.whitelist(scope).removeByName(a[3]) != null) {
                changed();
                send(s, "whitelist-removed", Messages.ph("player", a[3]));
            } else {
                send(s, "whitelist-missing", Messages.ph("player", a[3]));
            }
        } else if (op.equals("add")) {
            final String name = a[3];
            final String sc = scope;
            resolver.resolve(name, new UuidResolver.Callback() {
                public void done(UUID id, String exact) {
                    if (id == null) {
                        send(s, "whitelist-unknown-player", Messages.ph("player", name));
                        return;
                    }
                    pm.whitelist(sc).add(id, exact);
                    changed();
                    send(s, "whitelist-added", Messages.ph("player", exact));
                }
            });
        } else {
            send(s, "proxy-usage", Messages.ph());
        }
    }

    private void changed() {
        pm.changed();
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

    public List<String> complete(CommandSource s, String[] a) {
        List<String> out = new ArrayList<String>();
        List<String> scopes = new ArrayList<String>();
        scopes.add(ProxyMaintenance.GLOBAL);
        for (String n : servers.names()) scopes.add(n);
        String first = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "";
        if (a.length == 1) {
            filter(out, TOP, a[0]);
            filter(out, scopes, a[0]);
        } else if (a.length == 2 && (first.equals("status") || first.equals("schedule") || first.equals("whitelist"))) {
            filter(out, scopes, a[1]);
        } else if (a.length == 2) {
            filter(out, ACTIONS, a[1]);
        } else if (a.length == 3 && first.equals("whitelist")) {
            filter(out, WL, a[2]);
        } else if (a.length == 3 && (first.equals("schedule") || a[1].equalsIgnoreCase("on") || a[1].equalsIgnoreCase("extend"))) {
            filter(out, TIMES, a[2]);
        } else if (a.length == 4 && first.equals("whitelist") && a[2].equalsIgnoreCase("remove")) {
            if (s.has("smaintenance.command.whitelist") && containsIgnoreCase(scopes, a[1])) {
                filter(out, new ArrayList<String>(pm.whitelist(a[1]).entries().values()), a[3]);
            }
        }
        return out;
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        for (String x : list) {
            if (x.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void filter(List<String> out, List<String> options, String prefix) {
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)) && !out.contains(o)) {
                out.add(o);
            }
        }
    }
}
