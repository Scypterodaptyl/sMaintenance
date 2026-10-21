package com.scypter.smaintenance.paper.integration;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class BStatsSupport {
    private BStatsSupport() {
    }

    public static void enable(JavaPlugin plugin, int id) {
        new Metrics(plugin, id);
    }
}
