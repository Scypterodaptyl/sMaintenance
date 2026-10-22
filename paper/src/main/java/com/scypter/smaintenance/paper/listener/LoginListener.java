package com.scypter.smaintenance.paper.listener;

import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.paper.Features;
import com.scypter.smaintenance.paper.PaperRuntime;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

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
    public void onPreLoginCheck(AsyncPlayerPreLoginEvent e) {
        if (!Features.asyncLogin() || e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        if (!rt.manager.isEnabled() || rt.viewOnly()) {
            return;
        }
        if (rt.hasBypass(e.getUniqueId())) {
            if (bypassLimitReached()) {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, rt.text.legacyKick(rt.messages.raw("bypass-limit", Messages.ph())));
                rt.debug("bypass limit reached for " + e.getName());
            }
            return;
        }
        rt.debug("denied " + e.getName());
        Map<String, String> ph = rt.placeholders(rt.manager.snapshot());
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, rt.text.legacyKick(rt.kickMessage("kick-blocked", null, ph)));
        notifyAttempt(e.getUniqueId(), e.getName());
    }

    boolean bypassLimitReached() {
        int limit = rt.plugin.getConfig().getInt("bypass-limit", 0);
        return limit > 0 && bypassOnline() >= limit;
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

    void notifyAttempt(UUID id, String name) {
        long now = System.currentTimeMillis();
        long cooldown = rt.plugin.getConfig().getLong("notify-cooldown-seconds", 30) * 1000L;
        Long last = lastNotify.get(id);
        if (last != null && now - last < cooldown) {
            return;
        }
        lastNotify.put(id, now);
        final Component c = rt.messages.get("notify-attempt", Messages.ph(
                "player", name,
                "player_prefix", rt.extras.prefix(id),
                "player_suffix", rt.extras.suffix(id)));
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
