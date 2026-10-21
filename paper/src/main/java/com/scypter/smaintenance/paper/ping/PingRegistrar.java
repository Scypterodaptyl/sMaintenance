package com.scypter.smaintenance.paper.ping;

import com.scypter.smaintenance.paper.Features;
import com.scypter.smaintenance.paper.PaperRuntime;
import org.bukkit.event.Listener;

public final class PingRegistrar {
    private PingRegistrar() {
    }

    public static void register(PaperRuntime rt) {
        Listener l;
        if (Features.paperPing()) {
            l = new PaperPingHandler(rt);
        } else {
            l = new SpigotPingHandler(rt);
        }
        rt.plugin.getServer().getPluginManager().registerEvents(l, rt.plugin);
    }
}
