package com.scypter.smaintenance.paper.sched;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaTaskRunner implements TaskRunner {
    private final JavaPlugin plugin;

    public FoliaTaskRunner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void runTimer(final Runnable r, long periodTicks) {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, t -> r.run(), periodTicks, periodTicks);
    }

    public void runAsync(final Runnable r) {
        plugin.getServer().getAsyncScheduler().runNow(plugin, t -> r.run());
    }

    public void runMain(final Runnable r) {
        plugin.getServer().getGlobalRegionScheduler().run(plugin, t -> r.run());
    }

    public void runEntity(Player p, final Runnable r) {
        p.getScheduler().run(plugin, t -> r.run(), null);
    }

    public void runLater(final Runnable r, long delayTicks) {
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t -> r.run(), Math.max(1, delayTicks));
    }

    public void cancelAll() {
        plugin.getServer().getGlobalRegionScheduler().cancelTasks(plugin);
        plugin.getServer().getAsyncScheduler().cancelTasks(plugin);
    }
}
