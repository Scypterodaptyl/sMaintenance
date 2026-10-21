package com.scypter.smaintenance.paper.bar;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface BarSender {
    void show(Player p, Component text, float progress);

    void clear(Player p);
}
