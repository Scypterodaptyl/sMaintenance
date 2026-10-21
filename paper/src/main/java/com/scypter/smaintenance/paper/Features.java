package com.scypter.smaintenance.paper;

import java.util.Set;

public final class Features {
    private static boolean bossbar;
    private static boolean hexColors;
    private static boolean paperPing;
    private static boolean folia;
    private static boolean adventureNative;
    private static boolean protocolLib;

    private Features() {
    }

    public static void detect(Set<String> disabled) {
        bossbar = !disabled.contains("bossbar") && hasClass("org.bukkit.boss.BossBar");
        hexColors = !disabled.contains("hexColors") && VersionUtil.isAtLeast(1, 16);
        paperPing = !disabled.contains("paperPing") && hasClass("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
        folia = !disabled.contains("folia") && hasClass("io.papermc.paper.threadedregions.RegionizedServer");
        adventureNative = !disabled.contains("adventureNative") && hasClass("net.kyori.adventure.audience.Audience")
                && hasClass("io.papermc.paper.text.PaperComponents");
        protocolLib = !disabled.contains("protocolLib") && hasClass("com.comphenix.protocol.ProtocolLibrary");
    }

    public static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean bossbar() { return bossbar; }
    public static boolean hexColors() { return hexColors; }
    public static boolean paperPing() { return paperPing; }
    public static boolean folia() { return folia; }
    public static boolean adventureNative() { return adventureNative; }
    public static boolean protocolLib() { return protocolLib; }

    public static String summary() {
        return "bossbar=" + bossbar + ", hexColors=" + hexColors + ", paperPing=" + paperPing
                + ", folia=" + folia + ", adventureNative=" + adventureNative + ", protocolLib=" + protocolLib;
    }
}
