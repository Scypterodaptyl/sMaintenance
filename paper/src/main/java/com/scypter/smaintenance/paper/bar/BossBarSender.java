package com.scypter.smaintenance.paper.bar;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossBarSender implements BarSender {
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<UUID, BossBar>();
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

    public void show(Player p, Component text, float progress) {
        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
            bar.addPlayer(p);
            bars.put(p.getUniqueId(), bar);
        }
        bar.setTitle(legacy.serialize(text));
        bar.setProgress(Math.max(0f, Math.min(1f, progress)));
    }

    public void clear(Player p) {
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }
}
