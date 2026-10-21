package com.scypter.smaintenance.paper.integration;

import org.bukkit.entity.Player;

public interface TextHook {
    String apply(Player p, String text);
}
