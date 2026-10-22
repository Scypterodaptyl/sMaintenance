package com.scypter.smaintenance.paper.listener;

import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.paper.PaperRuntime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Map;

public final class LegacyLoginListener implements Listener {
    private final PaperRuntime rt;
    private final LoginListener login;

    public LegacyLoginListener(PaperRuntime rt, LoginListener login) {
        this.rt = rt;
        this.login = login;
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
            if (login.bypassLimitReached()) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, rt.text.legacyKick(rt.messages.raw("bypass-limit", Messages.ph())));
                rt.debug("bypass limit reached for " + p.getName());
            }
            return;
        }
        rt.debug("denied " + p.getName());
        Map<String, String> ph = rt.placeholders(rt.manager.snapshot());
        e.disallow(PlayerLoginEvent.Result.KICK_OTHER, rt.text.legacyKick(rt.kickMessage("kick-blocked", p, ph)));
        login.notifyAttempt(p.getUniqueId(), p.getName());
    }
}
