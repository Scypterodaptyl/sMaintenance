package com.scypter.smaintenance.paper.integration;

import com.scypter.smaintenance.core.config.ConfigView;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitConfigView implements ConfigView {
    private final JavaPlugin plugin;

    public BukkitConfigView(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String getString(String key, String def) {
        return plugin.getConfig().getString(key, def);
    }

    public int getInt(String key, int def) {
        return plugin.getConfig().getInt(key, def);
    }

    public boolean getBoolean(String key, boolean def) {
        return plugin.getConfig().getBoolean(key, def);
    }
}
