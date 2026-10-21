package com.scypter.smaintenance.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.proxy.PermissionChecker;
import com.scypter.smaintenance.core.proxy.ProxyListener;
import com.scypter.smaintenance.core.proxy.ProxyMaintenance;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.time.TimeFormatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VelocityActions implements ProxyListener {
    private final SMaintenanceVelocity plugin;

    public VelocityActions(SMaintenanceVelocity plugin) {
        this.plugin = plugin;
    }

    public static PermissionChecker perms(final Player p) {
        return new PermissionChecker() {
            public boolean has(String node) {
                return p.hasPermission(node);
            }
        };
    }

    public String currentServer(Player p) {
        return p.getCurrentServer().map(c -> c.getServerInfo().getName()).orElse(null);
    }

    public void onEnabled(String scope, MaintenanceState state) {
        plugin.notifier().enabled(scope, state);
        notifyAdmins(plugin.messages().get("enabled", plugin.pm().placeholders(scope, state)));
        signalContexts();
        enforce();
    }

    public void onDisabled(String scope, MaintenanceState state, boolean automatic, String by) {
        plugin.notifier().disabled(scope, automatic, by);
        openQueue(scope);
        signalContexts();
        notifyAdmins(plugin.messages().get(automatic ? "disabled-auto" : "disabled", Messages.ph("player", by == null ? "-" : by, "server", scope)));
    }

    public void onScheduled(String scope, MaintenanceState state) {
        plugin.notifier().scheduled(scope, state);
    }

    private void openQueue(String scope) {
        Optional<RegisteredServer> target = plugin.server().getServer(scope);
        if (!target.isPresent()) {
            plugin.queue().drain(scope);
            return;
        }
        for (java.util.UUID id : plugin.queue().drain(scope)) {
            Optional<Player> p = plugin.server().getPlayer(id);
            if (p.isPresent() && !scope.equalsIgnoreCase(currentServer(p.get()))) {
                p.get().sendMessage(plugin.messages().get("queue-open", Messages.ph("server", scope)));
                p.get().createConnectionRequest(target.get()).fireAndForget();
            }
        }
    }

    private void signalContexts() {
        for (Player p : plugin.server().getAllPlayers()) {
            plugin.extras().contextChanged(p);
        }
    }

    public void onScheduleCancelled(String scope) {
    }

    public void onWarning(String scope, long remainingMillis) {
        Map<String, String> ph = Messages.ph("time_left", TimeFormatter.format(remainingMillis), "server", scope);
        Component chat = plugin.messages().get("warning", ph);
        Component title = plugin.messages().raw("warning", ph);
        for (Player p : audience(scope)) {
            p.sendMessage(chat);
            p.showTitle(Title.title(title, Component.empty()));
        }
    }

    public void onChanged() {
        plugin.server().getScheduler().buildTask(plugin, new Runnable() {
            public void run() {
                try {
                    plugin.store().save(plugin.pm().snapshot());
                } catch (Exception e) {
                    plugin.logger().warn("Could not save data.json: {}", e.getMessage());
                }
            }
        }).schedule();
    }

    private List<Player> audience(String scope) {
        List<Player> out = new ArrayList<Player>();
        for (Player p : plugin.server().getAllPlayers()) {
            if (scope.equals(ProxyMaintenance.GLOBAL) || scope.equalsIgnoreCase(currentServer(p))) {
                out.add(p);
            }
        }
        return out;
    }

    public void showCountdowns() {
        for (String scope : plugin.pm().scheduledScopes()) {
            long left = plugin.pm().manager(scope).startsIn();
            if (left <= 0) {
                continue;
            }
            Component c = plugin.messages().raw("warning", Messages.ph("time_left", TimeFormatter.format(left), "server", scope));
            for (Player p : audience(scope)) {
                p.sendActionBar(c);
            }
        }
    }

    public void notifyAdmins(Component c) {
        for (Player p : plugin.server().getAllPlayers()) {
            if (p.hasPermission("smaintenance.notify")) {
                p.sendMessage(c);
            }
        }
        plugin.server().getConsoleCommandSource().sendMessage(c);
    }

    public void enforce() {
        boolean move = !"kick".equalsIgnoreCase(plugin.config().getString("closed-server-action", "move"));
        List<String> fallbacks = plugin.config().getList("fallback-servers");
        for (Player p : new ArrayList<Player>(plugin.server().getAllPlayers())) {
            String current = currentServer(p);
            String block = current != null
                    ? plugin.pm().closingScope(current, p.getUniqueId(), perms(p))
                    : plugin.pm().globalBlock(p.getUniqueId(), perms(p));
            if (block == null) {
                continue;
            }
            Map<String, String> ph = plugin.pm().placeholders(block);
            if (block.equals(ProxyMaintenance.GLOBAL) || !move) {
                p.disconnect(plugin.messages().raw("kick", ph));
                continue;
            }
            String target = plugin.pm().fallbackFor(fallbacks, current, p.getUniqueId(), perms(p));
            Optional<RegisteredServer> rs = target == null ? Optional.<RegisteredServer>empty() : plugin.server().getServer(target);
            if (rs.isPresent()) {
                p.createConnectionRequest(rs.get()).fireAndForget();
                p.sendMessage(plugin.messages().get("fallback-moved", ph));
            } else {
                p.disconnect(plugin.messages().raw("kick", ph));
            }
        }
    }
}
