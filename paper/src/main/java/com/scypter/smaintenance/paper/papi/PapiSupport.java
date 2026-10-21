package com.scypter.smaintenance.paper.papi;

import com.scypter.smaintenance.paper.PaperRuntime;
import com.scypter.smaintenance.paper.integration.TextHook;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public final class PapiSupport {
    private PapiSupport() {
    }

    public static void enable(PaperRuntime rt) {
        rt.texts = new TextHook() {
            public String apply(Player p, String text) {
                return PlaceholderAPI.setPlaceholders(p, text);
            }
        };
        new SMaintExpansion(rt).register();
    }
}
