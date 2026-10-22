package com.scypter.smaintenance.paper;

import com.scypter.smaintenance.core.schedule.RecurringRunner;
import com.scypter.smaintenance.core.schedule.RecurringSchedule;
import com.scypter.smaintenance.core.update.UpdateChecker;
import com.scypter.smaintenance.paper.command.MaintCommand;
import com.scypter.smaintenance.paper.listener.FreezeListener;
import com.scypter.smaintenance.paper.listener.LoginListener;
import com.scypter.smaintenance.paper.ping.PingRegistrar;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.Set;

public final class SMaintenancePaper extends JavaPlugin {
    private PaperRuntime runtime;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (ProxyGuard.isBehindProxy(this, getConfig().getBoolean("force-standalone", false))) {
            getLogger().severe("Server is behind a proxy. Install sMaintenance on the proxy (Velocity) instead.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Set<String> disabled = new HashSet<String>(getConfig().getStringList("features.disable"));
        Features.detect(disabled);
        getLogger().info("Features: " + Features.summary());
        runtime = new PaperRuntime(this);
        getServer().getPluginManager().registerEvents(new LoginListener(runtime), this);
        PingRegistrar.register(runtime);
        getServer().getPluginManager().registerEvents(new FreezeListener(runtime), this);
        getServer().getPluginManager().registerEvents(runtime.gui, this);
        com.scypter.smaintenance.api.SMaintenanceAPI.init(runtime);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            com.scypter.smaintenance.paper.papi.PapiSupport.enable(runtime);
        }
        try {
            com.scypter.smaintenance.paper.integration.BStatsSupport.enable(this, 34860);
        } catch (Throwable t) {
            getLogger().warning("bStats could not start: " + t.getMessage());
        }
        MaintCommand command = new MaintCommand(runtime);
        getCommand("maint").setExecutor(command);
        getCommand("maint").setTabCompleter(command);
        final PaperRuntime rt = runtime;
        if (getConfig().getBoolean("enable-on-startup", false) && !rt.manager.isEnabled()) {
            rt.manager.enable("startup", 0, getConfig().getString("startup-reason", "Server startup"));
        }
        final RecurringRunner recurring = buildRecurring();
        checkUpdates();
        runtime.tasks.runTimer(new Runnable() {
            public void run() {
                rt.manager.tick();
                recurring.tick(System.currentTimeMillis(), new RecurringRunner.Managers() {
                    public com.scypter.smaintenance.core.MaintenanceManager get(String scope) {
                        return rt.manager;
                    }
                });
                rt.showCountdown();
            }
        }, 20L);
    }

    private RecurringRunner buildRecurring() {
        List<RecurringSchedule> list = new ArrayList<RecurringSchedule>();
        for (String line : getConfig().getStringList("recurring")) {
            try {
                list.add(RecurringSchedule.parse(line, "server", false));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Bad 'recurring' entry \"" + line + "\": " + e.getMessage());
            }
        }
        String zone = getConfig().getString("timezone", "");
        TimeZone tz = zone.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(zone);
        return new RecurringRunner(list, tz, getConfig().getLong("recurring-lead-seconds", 300) * 1000L);
    }

    private void checkUpdates() {
        if (!getConfig().getBoolean("update-check", true)) {
            return;
        }
        final String slug = getConfig().getString("modrinth-slug", "smaintenance");
        runtime.tasks.runAsync(new Runnable() {
            public void run() {
                String latest = UpdateChecker.fetchLatest(slug);
                if (UpdateChecker.isNewer(getDescription().getVersion(), latest)) {
                    runtime.updateLatest = latest;
                    getLogger().info("A new version is available: " + latest);
                }
            }
        });
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            com.scypter.smaintenance.api.SMaintenanceAPI.shutdown();
            runtime.extras.close();
            runtime.saveNow();
            runtime.tasks.cancelAll();
            runtime.text.close();
        }
    }
}
