package com.scypter.smaintenance.paper.ping;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.scypter.smaintenance.paper.PaperRuntime;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;

public final class PaperPingHandler implements Listener {
    private final PaperRuntime rt;

    public PaperPingHandler(PaperRuntime rt) {
        this.rt = rt;
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPing(PaperServerListPingEvent e) {
        if (!rt.manager.isEnabled()) {
            return;
        }
        Map<String, String> ph = rt.placeholders(rt.manager.snapshot());
        String l1 = rt.text.legacyKick(rt.messages.raw("motd-line1", ph));
        String l2 = rt.text.legacyKick(rt.messages.raw("motd-line2", ph));
        e.setMotd(l1 + "\n" + l2);
        e.setVersion(rt.text.legacyKick(rt.messages.raw("motd-version", ph)));
        e.setProtocolVersion(-1);
        if (!rt.plugin.getConfig().getBoolean("show-real-online", false)) {
            e.setNumPlayers(0);
            e.setMaxPlayers(0);
        }
        e.getPlayerSample().clear();
        for (String line : rt.text.legacyKick(rt.messages.raw("motd-hover", ph)).split("\n")) {
            PlayerProfile profile = profile(line);
            if (profile != null) {
                e.getPlayerSample().add(profile);
            }
        }
    }

    private static PlayerProfile profile(String line) {
        try {
            return Bukkit.createProfile(UUID.randomUUID(), line);
        } catch (IllegalArgumentException invalidName) {
            return null;
        }
    }
}
