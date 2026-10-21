package com.scypter.smaintenance.paper.ping;

import com.scypter.smaintenance.paper.PaperRuntime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.Map;

public final class SpigotPingHandler implements Listener {
    private final PaperRuntime rt;

    public SpigotPingHandler(PaperRuntime rt) {
        this.rt = rt;
    }

    @EventHandler
    public void onPing(ServerListPingEvent e) {
        if (!rt.manager.isEnabled()) {
            return;
        }
        Map<String, String> ph = rt.placeholders(rt.manager.snapshot());
        String l1 = rt.text.legacyKick(rt.messages.raw("motd-line1", ph));
        String l2 = rt.text.legacyKick(rt.messages.raw("motd-line2", ph));
        e.setMotd(l1 + "\n" + l2);
    }
}
