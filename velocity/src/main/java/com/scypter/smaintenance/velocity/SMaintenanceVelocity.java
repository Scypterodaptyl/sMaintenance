package com.scypter.smaintenance.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.scypter.smaintenance.core.TimeSource;
import com.scypter.smaintenance.core.luckperms.BypassGroups;
import com.scypter.smaintenance.core.luckperms.Extras;
import com.scypter.smaintenance.core.luckperms.ExtrasFactory;
import com.scypter.smaintenance.core.notify.AuditLog;
import com.scypter.smaintenance.core.notify.Notifier;
import com.scypter.smaintenance.core.proxy.ProxyData;
import com.scypter.smaintenance.core.proxy.ServerQueue;
import com.scypter.smaintenance.core.schedule.RecurringRunner;
import com.scypter.smaintenance.core.schedule.RecurringSchedule;
import com.scypter.smaintenance.core.update.UpdateChecker;
import org.bstats.velocity.Metrics;
import com.scypter.smaintenance.core.config.FlatConfig;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.proxy.MojangUuidResolver;
import com.scypter.smaintenance.core.proxy.ProxyCommandHandler;
import com.scypter.smaintenance.core.proxy.ProxyMaintenance;
import com.scypter.smaintenance.core.proxy.ProxyStateStore;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Plugin(id = "smaintenance", name = "sMaintenance", version = "0.1.0",
        description = "Maintenance mode for the network", authors = {"ScypterV"},
        dependencies = {
                @com.velocitypowered.api.plugin.Dependency(id = "luckperms", optional = true),
                @com.velocitypowered.api.plugin.Dependency(id = "miniplaceholders", optional = true)
        })
public final class SMaintenanceVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dir;

    private volatile FlatConfig config = new FlatConfig();
    private volatile Messages messages;
    private ProxyStateStore store;
    private ProxyMaintenance pm;
    private VelocityActions actions;
    private BypassGroups groups;
    private Extras extras;
    private Notifier notifier;
    private RecurringRunner recurring;
    private final ServerQueue queue = new ServerQueue();
    private final Metrics.Factory metricsFactory;

    @Inject
    public SMaintenanceVelocity(ProxyServer server, Logger logger, @DataDirectory Path dir, Metrics.Factory metricsFactory) {
        this.metricsFactory = metricsFactory;
        this.server = server;
        this.logger = logger;
        this.dir = dir;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent e) {
        loadConfig();
        store = new ProxyStateStore(dir.resolve("data.json"));
        actions = new VelocityActions(this);
        List<Integer> marks = new ArrayList<Integer>();
        for (String s : config.getList("warning-marks")) {
            try {
                marks.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
            }
        }
        long[] warn = new long[marks.size()];
        for (int i = 0; i < warn.length; i++) {
            warn[i] = marks.get(i);
        }
        ProxyData data = store.load();
        java.util.Set<String> initial = new java.util.LinkedHashSet<String>(config.getList("bypass-groups"));
        initial.addAll(data.bypassGroups);
        groups = new BypassGroups(new ArrayList<String>(initial), new Runnable() {
            public void run() {
                actions.onChanged();
            }
        });
        extras = ExtrasFactory.create(groups, () -> !pm.enabledScopes().isEmpty(), () -> pm.enabledScopes());
        logger.info(extras.available() ? "LuckPerms found, extended features enabled" : "LuckPerms not found, extended features disabled");
        pm = new ProxyMaintenance(new TimeSource() {
            public long now() {
                return System.currentTimeMillis();
            }
        }, data, actions, warn, config.getString("default-reason", "Technical works"));
        pm.setBypassGroups(groups);
        pm.setGroupBypass(id -> extras.inBypassGroup(id));
        notifier = new Notifier(r -> server.getScheduler().buildTask(this, r).schedule(), m -> logger.warn(m),
                new AuditLog(dir.resolve("logs/maintenance.log")), () -> config, () -> messages);
        metricsFactory.make(this, 34861);
        if (server.getPluginManager().isLoaded("miniplaceholders")) {
            MiniPlaceholdersSupport.enable(this);
        }
        recurring = buildRecurring();
        if (config.getBoolean("enable-on-startup", false) && !pm.manager(ProxyMaintenance.GLOBAL).isEnabled()) {
            pm.manager(ProxyMaintenance.GLOBAL).enable("startup", 0, config.getString("startup-reason", "Proxy startup"));
        }
        checkUpdates();
        server.getEventManager().register(this, new VelocityEvents(this));
        registerCommand();
        server.getScheduler().buildTask(this, new Runnable() {
            public void run() {
                pm.tick();
                recurring.tick(System.currentTimeMillis(), scope -> pm.manager(scope));
                actions.showCountdowns();
            }
        }).repeat(1, TimeUnit.SECONDS).schedule();
        logger.info("sMaintenance enabled");
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent e) {
        if (extras != null) {
            extras.close();
        }
        if (store != null && pm != null) {
            store.save(pm.snapshot());
        }
    }

    private void registerCommand() {
        final ProxyCommandHandler handler = new ProxyCommandHandler(pm, () -> messages, new ProxyCommandHandler.ServerDirectory() {
            public Collection<String> names() {
                List<String> out = new ArrayList<String>();
                server.getAllServers().forEach(s -> out.add(s.getServerInfo().getName()));
                return out;
            }
        }, new MojangUuidResolver(new Executor() {
            public void execute(Runnable r) {
                server.getScheduler().buildTask(SMaintenanceVelocity.this, r).schedule();
            }
        }, extras), this::reload, groups, () -> extras.available());
        CommandMeta meta = server.getCommandManager().metaBuilder("maint").aliases("maintenance", "tech").plugin(this).build();
        server.getCommandManager().register(meta, new VelocityCommand(handler));
    }

    private RecurringRunner buildRecurring() {
        List<RecurringSchedule> list = new ArrayList<RecurringSchedule>();
        for (String line : config.getList("recurring")) {
            try {
                list.add(RecurringSchedule.parse(line, ProxyMaintenance.GLOBAL, true));
            } catch (IllegalArgumentException e) {
                logger.warn("Bad recurring entry \"{}\": {}", line, e.getMessage());
            }
        }
        String zone = config.getString("timezone", "");
        java.util.TimeZone tz = zone.isEmpty() ? java.util.TimeZone.getDefault() : java.util.TimeZone.getTimeZone(zone);
        return new RecurringRunner(list, tz, config.getInt("recurring-lead-seconds", 300) * 1000L);
    }

    private void checkUpdates() {
        if (!config.getBoolean("update-check", true)) {
            return;
        }
        final String slug = config.getString("modrinth-slug", "smaintenance");
        server.getScheduler().buildTask(this, () -> {
            String latest = UpdateChecker.fetchLatest(slug);
            if (UpdateChecker.isNewer("0.1.0", latest)) {
                logger.info("A new sMaintenance version is available: {}", latest);
            }
        }).schedule();
    }

    private void reload() {
        loadConfig();
    }

    private void loadConfig() {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve("config.yml");
            if (!Files.exists(file)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) {
                        Files.copy(in, file);
                    }
                }
            }
            FlatConfig cfg = FlatConfig.parse(new String(Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8));
            this.config = cfg;
            FlatConfig lang = readLang("ru");
            String selected = cfg.getString("language", "ru");
            if (!selected.equals("ru")) {
                lang.putAll(readLang(selected));
            }
            this.messages = new Messages(lang.strings(), cfg.getString("prefix", ""));
        } catch (IOException ex) {
            logger.error("Could not load config", ex);
            if (messages == null) {
                messages = new Messages(new java.util.HashMap<String, String>(), "");
            }
        }
    }

    private FlatConfig readLang(String code) throws IOException {
        InputStream in = getClass().getResourceAsStream("/lang/" + code + ".yml");
        return FlatConfig.read(in);
    }

    public ProxyServer server() { return server; }
    public Logger logger() { return logger; }
    public FlatConfig config() { return config; }
    public Messages messages() { return messages; }
    public ProxyMaintenance pm() { return pm; }
    public ProxyStateStore store() { return store; }
    public Extras extras() { return extras; }
    public ServerQueue queue() { return queue; }
    public Notifier notifier() { return notifier; }
}
