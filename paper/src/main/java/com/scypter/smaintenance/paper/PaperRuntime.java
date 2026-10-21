package com.scypter.smaintenance.paper;

import com.scypter.smaintenance.core.MaintenanceManager;
import com.scypter.smaintenance.core.TimeSource;
import com.scypter.smaintenance.api.MaintenanceDisableEvent;
import com.scypter.smaintenance.api.MaintenanceEnableEvent;
import com.scypter.smaintenance.core.luckperms.BypassGroups;
import com.scypter.smaintenance.core.luckperms.Extras;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.notify.AuditLog;
import com.scypter.smaintenance.core.notify.Notifier;
import com.scypter.smaintenance.paper.integration.BukkitConfigView;
import com.scypter.smaintenance.core.luckperms.ExtrasFactory;
import com.scypter.smaintenance.paper.integration.TextHook;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.state.StateStore;
import com.scypter.smaintenance.core.state.StoredData;
import com.scypter.smaintenance.core.time.TimeFormatter;
import com.scypter.smaintenance.core.whitelist.Whitelist;
import com.scypter.smaintenance.paper.bar.ActionBarSender;
import com.scypter.smaintenance.paper.bar.BarSender;
import com.scypter.smaintenance.paper.bar.BossBarSender;
import com.scypter.smaintenance.paper.listener.PaperMaintenanceListener;
import com.scypter.smaintenance.paper.sched.BukkitTaskRunner;
import com.scypter.smaintenance.paper.sched.FoliaTaskRunner;
import com.scypter.smaintenance.paper.sched.TaskRunner;
import com.scypter.smaintenance.paper.text.TextSender;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PaperRuntime {
    public final JavaPlugin plugin;
    public final StateStore store;
    public final StoredData data;
    public final Whitelist whitelist;
    public final TextSender text;
    public final TaskRunner tasks;
    public final BarSender bar;
    public final MaintenanceManager manager;
    public volatile Messages messages;
    public volatile TextHook texts = new TextHook() {
        public String apply(Player p, String text) {
            return text;
        }
    };
    public final BypassGroups groups;
    public final com.scypter.smaintenance.core.history.HistoryLog history;
    public volatile String updateLatest;
    public final com.scypter.smaintenance.paper.gui.GuiMenu gui;
    public final Extras extras;
    public final Notifier notifier;

    public PaperRuntime(JavaPlugin plugin) {
        this.plugin = plugin;
        this.store = new StateStore(new File(plugin.getDataFolder(), "data.json").toPath());
        this.data = store.load();
        this.whitelist = new Whitelist(data.whitelist);
        this.text = new TextSender(plugin, Features.hexColors());
        if (Features.folia()) {
            this.tasks = new FoliaTaskRunner(plugin);
        } else {
            this.tasks = new BukkitTaskRunner(plugin);
        }
        if (Features.bossbar()) {
            this.bar = new BossBarSender();
        } else {
            this.bar = new ActionBarSender(text);
        }
        reloadMessages();
        this.history = new com.scypter.smaintenance.core.history.HistoryLog(data.history, 50);
        this.gui = new com.scypter.smaintenance.paper.gui.GuiMenu(this);
        java.util.Set<String> initialGroups = new LinkedHashSet<String>(plugin.getConfig().getStringList("bypass-groups"));
        initialGroups.addAll(data.bypassGroups);
        this.groups = new BypassGroups(new ArrayList<String>(initialGroups), new Runnable() {
            public void run() {
                PaperRuntime.this.saveAsync();
            }
        });
        this.extras = ExtrasFactory.create(groups, new java.util.function.BooleanSupplier() {
            public boolean getAsBoolean() {
                return PaperRuntime.this.manager != null && PaperRuntime.this.manager.isEnabled();
            }
        }, new java.util.function.Supplier<Collection<String>>() {
            public Collection<String> get() {
                return null;
            }
        });
        if (extras.available()) {
            plugin.getLogger().info("LuckPerms found, extended features enabled");
        } else {
            plugin.getLogger().info("LuckPerms not found, extended features disabled");
        }
        this.notifier = new Notifier(new java.util.concurrent.Executor() {
            public void execute(Runnable r) {
                PaperRuntime.this.tasks.runAsync(r);
            }
        }, new java.util.function.Consumer<String>() {
            public void accept(String m) {
                PaperRuntime.this.plugin.getLogger().warning(m);
            }
        }, new AuditLog(new File(plugin.getDataFolder(), "logs/maintenance.log").toPath()), new java.util.function.Supplier<com.scypter.smaintenance.core.config.ConfigView>() {
            public com.scypter.smaintenance.core.config.ConfigView get() {
                return new BukkitConfigView(PaperRuntime.this.plugin);
            }
        }, new java.util.function.Supplier<Messages>() {
            public Messages get() {
                return PaperRuntime.this.messages;
            }
        });
        List<Integer> marks = plugin.getConfig().getIntegerList("warning-marks");
        long[] warn = new long[marks.size()];
        for (int i = 0; i < warn.length; i++) {
            warn[i] = marks.get(i);
        }
        this.manager = new MaintenanceManager(new TimeSource() {
            public long now() {
                return System.currentTimeMillis();
            }
        }, data.state, new PaperMaintenanceListener(this), warn);
    }

    public void reloadMessages() {
        String lang = plugin.getConfig().getString("language", "ru");
        Map<String, String> map = new HashMap<String, String>();
        loadInto(map, plugin.getResource("lang/ru.yml"));
        loadInto(map, plugin.getResource("lang/" + lang + ".yml"));
        File override = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");
        if (override.isFile()) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(override);
            for (String k : yml.getKeys(false)) {
                map.put(k, yml.getString(k));
            }
        }
        this.messages = new Messages(map, plugin.getConfig().getString("prefix", ""));
    }

    private void loadInto(Map<String, String> map, InputStream in) {
        if (in == null) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        for (String k : yml.getKeys(false)) {
            map.put(k, yml.getString(k));
        }
    }

    private StoredData snapshotData() {
        StoredData copy = new StoredData();
        copy.state = manager.snapshot();
        copy.bypassGroups = groups.list();
        copy.history = history.snapshot();
        for (Map.Entry<java.util.UUID, String> e : whitelist.entries().entrySet()) {
            copy.whitelist.put(e.getKey().toString(), e.getValue());
        }
        return copy;
    }

    public void saveAsync() {
        final StoredData copy = snapshotData();
        tasks.runAsync(new Runnable() {
            public void run() {
                try {
                    store.save(copy);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not save data.json: " + e.getMessage());
                }
            }
        });
    }

    public void debug(String msg) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[debug] " + msg);
        }
    }

    public boolean viewOnly() {
        return plugin.getConfig().getBoolean("view-only", false);
    }

    public void saveNow() {
        store.save(snapshotData());
    }

    public Map<String, String> placeholders(MaintenanceState s) {
        String def = plugin.getConfig().getString("default-reason", "Technical works");
        String reason = s.getReason() == null || s.getReason().isEmpty() ? def : s.getReason();
        long left = s.getEndAt() > 0 ? Math.max(0, s.getEndAt() - System.currentTimeMillis()) : 0;
        Map<String, String> m = new HashMap<String, String>();
        m.put("reason", reason);
        m.put("time_left", s.getEndAt() > 0 ? TimeFormatter.format(left) : "-");
        m.put("enabled_by", s.getEnabledBy() == null ? "-" : s.getEnabledBy());
        m.put("end_time", s.getEndAt() > 0 ? new Date(s.getEndAt()).toString() : "-");
        m.put("server", plugin.getServer().getName());
        return m;
    }

    public boolean hasBypass(Player p) {
        return p.hasPermission("smaintenance.bypass") || whitelist.contains(p.getUniqueId()) || extras.inBypassGroup(p.getUniqueId());
    }

    public boolean tryEnable(String by, long durationMillis, String reason) {
        MaintenanceEnableEvent ev = new MaintenanceEnableEvent(by, durationMillis, reason);
        plugin.getServer().getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return false;
        }
        return manager.enable(by, ev.getDurationMillis(), ev.getReason());
    }

    public boolean tryDisable(String by) {
        MaintenanceDisableEvent ev = new MaintenanceDisableEvent(by);
        plugin.getServer().getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return false;
        }
        return manager.disable(by);
    }

    public Component kickMessage(String key, final Player p, Map<String, String> ph) {
        return messages.raw(key, ph, new java.util.function.UnaryOperator<String>() {
            public String apply(String template) {
                return texts.apply(p, template);
            }
        });
    }

    public void showCountdown() {
        long startsIn = manager.startsIn();
        if (startsIn <= 0) {
            return;
        }
        float progress = Math.min(1f, startsIn / 300_000f);
        Component c = messages.raw("warning", Messages.ph("time_left", TimeFormatter.format(startsIn)));
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            bar.show(p, c, progress);
        }
    }
}
