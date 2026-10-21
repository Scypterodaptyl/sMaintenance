package com.scypter.smaintenance.paper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ProxyGuard {
    private ProxyGuard() {
    }

    public static boolean isBehindProxy(JavaPlugin plugin, boolean forceStandalone) {
        if (forceStandalone) {
            return false;
        }
        File root = plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
        if (readBoolean(new File(root, "spigot.yml"), "settings.bungeecord")) {
            return true;
        }
        return readBoolean(new File(root, "config/paper-global.yml"), "proxies.velocity.enabled");
    }

    private static boolean readBoolean(File file, String path) {
        if (!file.isFile()) {
            return false;
        }
        try {
            return YamlConfiguration.loadConfiguration(file).getBoolean(path, false);
        } catch (Exception e) {
            return false;
        }
    }
}
