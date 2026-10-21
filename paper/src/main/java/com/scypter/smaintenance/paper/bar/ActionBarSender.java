package com.scypter.smaintenance.paper.bar;

import com.scypter.smaintenance.paper.text.TextSender;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class ActionBarSender implements BarSender {
    private final TextSender text;

    public ActionBarSender(TextSender text) {
        this.text = text;
    }

    public void show(Player p, Component c, float progress) {
        text.actionBar(p, c);
    }

    public void clear(Player p) {
        text.actionBar(p, Component.empty());
    }
}
