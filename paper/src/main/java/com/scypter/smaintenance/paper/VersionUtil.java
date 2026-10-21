package com.scypter.smaintenance.paper;

import org.bukkit.Bukkit;

public final class VersionUtil {
    private VersionUtil() {
    }

    public static int[] parse(String bukkitVersion) {
        try {
            String[] parts = bukkitVersion.split("-")[0].split("[.]");
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (Exception e) {
            return new int[]{1, 8};
        }
    }

    public static boolean isAtLeast(int major, int minor) {
        int[] v = parse(Bukkit.getBukkitVersion());
        return v[0] > major || (v[0] == major && v[1] >= minor);
    }
}
