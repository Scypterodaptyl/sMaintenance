package com.scypter.smaintenance.paper.text;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TextSender {
    private final BukkitAudiences audiences;
    private final LegacyComponentSerializer legacy;

    public TextSender(JavaPlugin plugin, boolean hex) {
        this.audiences = BukkitAudiences.create(plugin);
        LegacyComponentSerializer.Builder b = LegacyComponentSerializer.builder().character(LegacyComponentSerializer.SECTION_CHAR);
        if (hex) {
            b.hexColors();
        }
        this.legacy = b.build();
    }

    public void message(CommandSender to, Component c) {
        audiences.sender(to).sendMessage(c);
    }

    public void title(Player p, Component title, Component subtitle) {
        audiences.player(p).showTitle(Title.title(title, subtitle));
    }

    public void actionBar(Player p, Component c) {
        audiences.player(p).sendActionBar(c);
    }

    public String legacyKick(Component c) {
        return legacy.serialize(c);
    }

    public void close() {
        audiences.close();
    }
}
