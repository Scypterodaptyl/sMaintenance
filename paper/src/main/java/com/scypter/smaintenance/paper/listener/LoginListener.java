package com.scypter.smaintenance.paper.listener;

import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.paper.PaperRuntime;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LoginListener implements Listener {
    private final PaperRuntime rt;
    private final Map<UUID, Long> lastNotify = new ConcurrentHashMap<UUID, Long>();

    public LoginListener(PaperRuntime rt) {
        this.rt = rt;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (rt.extras.available() && rt.manager.isEnabled()) {
            rt.extras.preload(e.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent e) {
        if (!rt.manager.isEnabled()) {
            return;
        }
        if (rt.viewOnly()) {
            return;
        }
        Player p = e.getPlayer();
        if (rt.hasBypass(p)) {
            int limit = rt.plugin.getConfig().getInt("bypass-limit", 0);
            if (limit > 0 && bypassOnline() >= limit) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, rt.text.legacyKick(rt.messages.raw("bypass-limit", Messages.ph())));
                rt.debug("bypass limit reached for " + p.getName());
            }
            return;
        }
        rt.debug("denied " + p.getName());
        Map<String, String> ph = rt.placeholders(rt.manager.snapshot());
        e.disallow(PlayerLoginEvent.Result.KICK_OTHER, rt.text.legacyKick(rt.kickMessage("kick-blocked", p, ph)));
        notifyAttempt(p);
    }

    private int bypassOnline() {
        int n = 0;
        for (Player o : rt.plugin.getServer().getOnlinePlayers()) {
            if (o.hasPermission("smaintenance.bypass") || rt.whitelist.contains(o.getUniqueId()) || rt.extras.inBypassGroup(o.getUniqueId())) {
                n++;
            }
        }
        return n;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String latest = rt.updateLatest;
        if (latest != null && e.getPlayer().hasPermission("smaintenance.notify")) {
            rt.text.message(e.getPlayer(), rt.messages.get("update-available", Messages.ph("latest", latest,
                    "current", rt.plugin.getDescription().getVersion())));
        }
    }

    private void notifyAttempt(Player attempted) {
        long now = System.currentTimeMillis();
        long cooldown = rt.plugin.getConfig().getLong("notify-cooldown-seconds", 30) * 1000L;
        Long last = lastNotify.get(attempted.getUniqueId());
        if (last != null && now - last < cooldown) {
            return;
        }
        lastNotify.put(attempted.getUniqueId(), now);
        final Component c = rt.messages.get("notify-attempt", Messages.ph(
                "player", attempted.getName(),
                "player_prefix", rt.extras.prefix(attempted.getUniqueId()),
                "player_suffix", rt.extras.suffix(attempted.getUniqueId())));
        rt.tasks.runMain(new Runnable() {
            public void run() {
                for (Player p : rt.plugin.getServer().getOnlinePlayers()) {
                    if (p.hasPermission("smaintenance.notify")) {
                        rt.text.message(p, c);
                    }
                }
            }
        });
    }
}
