package com.scypter.smaintenance.velocity;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.proxy.ProxyMaintenance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocityEvents {
    private final SMaintenanceVelocity plugin;
    private final Map<UUID, Long> lastNotify = new ConcurrentHashMap<UUID, Long>();

    public VelocityEvents(SMaintenanceVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onLogin(LoginEvent e) {
        Player p = e.getPlayer();
        if (plugin.extras().available() && !plugin.pm().enabledScopes().isEmpty()) {
            plugin.extras().preload(p.getUniqueId());
        }
        String block = plugin.pm().globalBlock(p.getUniqueId(), VelocityActions.perms(p));
        if (block == null) {
            return;
        }
        Map<String, String> ph = plugin.pm().placeholders(block);
        e.setResult(ResultedEvent.ComponentResult.denied(plugin.messages().raw("kick-blocked", ph)));
        notifyAttempt(p);
    }

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent e) {
        Optional<RegisteredServer> original = e.getResult().getServer();
        if (!original.isPresent()) {
            return;
        }
        Player p = e.getPlayer();
        String name = original.get().getServerInfo().getName();
        String block = plugin.pm().closingScope(name, p.getUniqueId(), VelocityActions.perms(p));
        if (block == null) {
            return;
        }
        Map<String, String> ph = plugin.pm().placeholders(block);
        ph.put("server", name);
        boolean initial = !p.getCurrentServer().isPresent();
        if (initial) {
            List<String> fallbacks = plugin.config().getList("fallback-servers");
            String target = plugin.pm().fallbackFor(fallbacks, name, p.getUniqueId(), VelocityActions.perms(p));
            Optional<RegisteredServer> rs = target == null ? Optional.<RegisteredServer>empty() : plugin.server().getServer(target);
            if (rs.isPresent()) {
                e.setResult(ServerPreConnectEvent.ServerResult.allowed(rs.get()));
                p.sendMessage(plugin.messages().get("fallback-moved", ph));
                return;
            }
        }
        if (!block.equals(ProxyMaintenance.GLOBAL)) {
            plugin.queue().add(block, p.getUniqueId());
        }
        e.setResult(ServerPreConnectEvent.ServerResult.denied());
        p.sendMessage(plugin.messages().get("connect-blocked", ph));
        if (initial) {
            p.disconnect(plugin.messages().raw("kick-blocked", ph));
        }
        notifyAttempt(p);
    }

    @Subscribe
    public void onPing(ProxyPingEvent e) {
        if (!plugin.pm().manager(ProxyMaintenance.GLOBAL).isEnabled()) {
            return;
        }
        Map<String, String> ph = plugin.pm().placeholders(ProxyMaintenance.GLOBAL);
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
        ServerPing.Builder b = e.getPing().asBuilder();
        Component l1 = plugin.messages().raw("motd-line1", ph);
        Component l2 = plugin.messages().raw("motd-line2", ph);
        b.description(l1.append(Component.newline()).append(l2));
        b.version(new ServerPing.Version(-1, legacy.serialize(plugin.messages().raw("motd-version", ph))));
        if (!plugin.config().getBoolean("show-real-online", false)) {
            b.onlinePlayers(0);
            b.maximumPlayers(0);
        }
        b.clearSamplePlayers();
        for (String line : legacy.serialize(plugin.messages().raw("motd-hover", ph)).split("\n")) {
            b.samplePlayers(new ServerPing.SamplePlayer(line, UUID.randomUUID()));
        }
        e.setPing(b.build());
    }

    private void notifyAttempt(Player attempted) {
        long now = System.currentTimeMillis();
        long cooldown = plugin.config().getInt("notify-cooldown-seconds", 30) * 1000L;
        Long last = lastNotify.get(attempted.getUniqueId());
        if (last != null && now - last < cooldown) {
            return;
        }
        lastNotify.put(attempted.getUniqueId(), now);
        plugin.server().getAllPlayers().forEach(p -> {
            if (p.hasPermission("smaintenance.notify")) {
                p.sendMessage(plugin.messages().get("notify-attempt", Messages.ph("player", attempted.getUsername(), "player_prefix", plugin.extras().prefix(attempted.getUniqueId()), "player_suffix", plugin.extras().suffix(attempted.getUniqueId()))));
            }
        });
    }
}
