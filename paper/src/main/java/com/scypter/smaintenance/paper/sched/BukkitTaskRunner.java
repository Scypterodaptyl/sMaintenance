package com.scypter.smaintenance.paper.sched;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitTaskRunner implements TaskRunner {
    private final JavaPlugin plugin;

    public BukkitTaskRunner(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void runTimer(Runnable r, long periodTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, r, periodTicks, periodTicks);
    }

    public void runAsync(Runnable r) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, r);
    }

    public void runMain(Runnable r) {
        plugin.getServer().getScheduler().runTask(plugin, r);
    }

    public void runEntity(Player p, Runnable r) {
        runMain(r);
    }

    public void runLater(Runnable r, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, r, delayTicks);
    }

    public void cancelAll() {
        plugin.getServer().getScheduler().cancelTasks(plugin);
    }
}
