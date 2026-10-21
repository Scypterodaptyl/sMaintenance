package com.scypter.smaintenance.core.proxy;

import com.scypter.smaintenance.core.MaintenanceListener;
import com.scypter.smaintenance.core.MaintenanceManager;
import com.scypter.smaintenance.core.TimeSource;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.time.TimeFormatter;
import com.scypter.smaintenance.core.whitelist.Whitelist;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ProxyMaintenance {
    public static final String GLOBAL = "global";

    private final TimeSource time;
    private final ProxyData data;
    private final ProxyListener listener;
    private final long[] warningSeconds;
    private final String defaultReason;
    private final Map<String, MaintenanceManager> managers = new LinkedHashMap<String, MaintenanceManager>();
    private final Map<String, Whitelist> whitelists = new LinkedHashMap<String, Whitelist>();
    private final com.scypter.smaintenance.core.history.HistoryLog history;
    private volatile BypassCheck groupBypass;
    private volatile com.scypter.smaintenance.core.luckperms.BypassGroups bypassGroups;

    public interface BypassCheck {
        boolean has(UUID id);
    }

    public void setBypassGroups(com.scypter.smaintenance.core.luckperms.BypassGroups groups) {
        this.bypassGroups = groups;
    }

    public List<String> savedGroups() {
        return new ArrayList<String>(data.bypassGroups);
    }

    public void setGroupBypass(BypassCheck check) {
        this.groupBypass = check;
    }

    public ProxyMaintenance(TimeSource time, ProxyData data, ProxyListener listener, long[] warningSeconds, String defaultReason) {
        this.time = time;
        this.data = data;
        this.listener = listener;
        this.warningSeconds = warningSeconds;
        this.defaultReason = defaultReason;
        if (data.history == null) data.history = new ArrayList<com.scypter.smaintenance.core.history.HistoryEntry>();
        this.history = new com.scypter.smaintenance.core.history.HistoryLog(data.history, 50);
        for (String scope : new ArrayList<String>(data.states.keySet())) {
            manager(scope);
        }
    }

    public com.scypter.smaintenance.core.history.HistoryLog history() {
        return history;
    }

    public void changed() {
        listener.onChanged();
    }

    public static String normalize(String scope) {
        return scope.toLowerCase(Locale.ROOT);
    }

    public synchronized MaintenanceManager manager(String rawScope) {
        final String scope = normalize(rawScope);
        MaintenanceManager m = managers.get(scope);
        if (m == null) {
            MaintenanceState state = data.states.get(scope);
            if (state == null) {
                state = new MaintenanceState();
                data.states.put(scope, state);
            }
            m = new MaintenanceManager(time, state, new MaintenanceListener() {
                public void onEnabled(MaintenanceState s) {
                    history.add(new com.scypter.smaintenance.core.history.HistoryEntry(time.now(), scope, s.getEnabledBy(), s.getReason(), s.getEndAt() > 0 ? s.getEndAt() - time.now() : 0));
                    listener.onEnabled(scope, s);
                }
                public void onDisabled(MaintenanceState s, boolean auto, String by) { listener.onDisabled(scope, s, auto, by); }
                public void onScheduled(MaintenanceState s) { listener.onScheduled(scope, s); }
                public void onScheduleCancelled() { listener.onScheduleCancelled(scope); }
                public void onWarning(long left) { listener.onWarning(scope, left); }
                public void onChanged() { listener.onChanged(); }
            }, warningSeconds);
            managers.put(scope, m);
        }
        return m;
    }

    public synchronized Whitelist whitelist(String rawScope) {
        String scope = normalize(rawScope);
        Whitelist w = whitelists.get(scope);
        if (w == null) {
            Map<String, String> backing = data.whitelists.get(scope);
            if (backing == null) {
                backing = new LinkedHashMap<String, String>();
                data.whitelists.put(scope, backing);
            }
            w = new Whitelist(backing);
            whitelists.put(scope, w);
        }
        return w;
    }

    public boolean hasBypass(String scope, UUID id, PermissionChecker perms) {
        String s = normalize(scope);
        return perms.has("smaintenance.bypass")
                || perms.has("smaintenance.bypass." + s)
                || (groupBypass != null && groupBypass.has(id))
                || whitelist(GLOBAL).contains(id)
                || whitelist(s).contains(id);
    }

    public String closingScope(String server, UUID id, PermissionChecker perms) {
        String globalBlock = globalBlock(id, perms);
        if (globalBlock != null) {
            return globalBlock;
        }
        String s = normalize(server);
        if (manager(s).isEnabled() && !hasBypass(s, id, perms)) {
            return s;
        }
        return null;
    }

    public String globalBlock(UUID id, PermissionChecker perms) {
        if (manager(GLOBAL).isEnabled() && !hasBypass(GLOBAL, id, perms)) {
            return GLOBAL;
        }
        return null;
    }

    public String fallbackFor(List<String> fallbacks, String exclude, UUID id, PermissionChecker perms) {
        for (String f : fallbacks) {
            if (f.equalsIgnoreCase(exclude)) {
                continue;
            }
            if (closingScope(f, id, perms) == null) {
                return f;
            }
        }
        return null;
    }

    private synchronized Map<String, MaintenanceManager> managersCopy() {
        return new LinkedHashMap<String, MaintenanceManager>(managers);
    }

    private synchronized Map<String, Whitelist> whitelistsCopy() {
        return new LinkedHashMap<String, Whitelist>(whitelists);
    }

    public List<String> enabledScopes() {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, MaintenanceManager> e : managersCopy().entrySet()) {
            if (e.getValue().isEnabled()) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    public List<String> scheduledScopes() {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, MaintenanceManager> e : managersCopy().entrySet()) {
            if (e.getValue().startsIn() > 0) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    public void tick() {
        for (MaintenanceManager m : managersCopy().values()) {
            m.tick();
        }
    }

    public ProxyData snapshot() {
        ProxyData copy = new ProxyData();
        com.scypter.smaintenance.core.luckperms.BypassGroups bg = bypassGroups;
        copy.history = history.snapshot();
        copy.bypassGroups = bg != null ? bg.list() : savedGroups();
        for (Map.Entry<String, MaintenanceManager> e : managersCopy().entrySet()) {
            copy.states.put(e.getKey(), e.getValue().snapshot());
        }
        for (Map.Entry<String, Whitelist> e : whitelistsCopy().entrySet()) {
            Map<String, String> m = new LinkedHashMap<String, String>();
            for (Map.Entry<UUID, String> w : e.getValue().entries().entrySet()) {
                m.put(w.getKey().toString(), w.getValue());
            }
            copy.whitelists.put(e.getKey(), m);
        }
        return copy;
    }

    public Map<String, String> placeholders(String rawScope) {
        String scope = normalize(rawScope);
        MaintenanceState s = manager(scope).snapshot();
        return placeholders(scope, s);
    }

    public Map<String, String> placeholders(String scope, MaintenanceState s) {
        String reason = s.getReason() == null || s.getReason().isEmpty() ? defaultReason : s.getReason();
        long left = s.getEndAt() > 0 ? Math.max(0, s.getEndAt() - time.now()) : 0;
        Map<String, String> m = new HashMap<String, String>();
        m.put("reason", reason);
        m.put("time_left", s.getEndAt() > 0 ? TimeFormatter.format(left) : "-");
        m.put("enabled_by", s.getEnabledBy() == null ? "-" : s.getEnabledBy());
        m.put("end_time", s.getEndAt() > 0 ? new Date(s.getEndAt()).toString() : "-");
        m.put("server", scope);
        return m;
    }
}
