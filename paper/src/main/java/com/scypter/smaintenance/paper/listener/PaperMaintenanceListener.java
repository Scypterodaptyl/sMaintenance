package com.scypter.smaintenance.paper.listener;

import com.scypter.smaintenance.core.MaintenanceListener;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.time.TimeFormatter;
import com.scypter.smaintenance.paper.PaperRuntime;
import com.scypter.smaintenance.paper.sound.SoundPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Map;

public final class PaperMaintenanceListener implements MaintenanceListener {
    private static final String SCOPE = "server";

    private final PaperRuntime rt;

    public PaperMaintenanceListener(PaperRuntime rt) {
        this.rt = rt;
    }

    public void onEnabled(final MaintenanceState s) {
        final Map<String, String> ph = rt.placeholders(s);
        rt.notifier.enabled(SCOPE, s);
        rt.history.add(new com.scypter.smaintenance.core.history.HistoryEntry(System.currentTimeMillis(), SCOPE, s.getEnabledBy(), s.getReason(),
                s.getEndAt() > 0 ? s.getEndAt() - System.currentTimeMillis() : 0));
        rt.debug("enabled by " + s.getEnabledBy());
        rt.tasks.runMain(new Runnable() {
            public void run() {
                notifyAdmins(rt.messages.get("enabled", ph));
                clearBars();
                signalContexts();
                if (rt.viewOnly()) {
                    for (Player p : rt.plugin.getServer().getOnlinePlayers()) {
                        rt.text.message(p, rt.messages.get("view-only", Messages.ph()));
                    }
                } else {
                    kickNonBypass(ph, rt.plugin.getConfig().getInt("kick-delay-seconds", 0));
                }
            }
        });
    }

    private void signalContexts() {
        for (Player p : rt.plugin.getServer().getOnlinePlayers()) {
            rt.extras.contextChanged(p);
        }
    }

    private void kickNonBypass(Map<String, String> ph, int delaySeconds) {
        for (final Player p : new ArrayList<Player>(rt.plugin.getServer().getOnlinePlayers())) {
            if (rt.hasBypass(p)) {
                continue;
            }
            if (delaySeconds <= 0) {
                kick(p, ph);
            } else {
                rt.text.title(p, rt.messages.raw("warning", Messages.ph("time_left", delaySeconds + "s")), Component.empty());
                rt.tasks.runLater(new Runnable() {
                    public void run() {
                        if (p.isOnline() && rt.manager.isEnabled() && !rt.hasBypass(p)) {
                            kick(p, rt.placeholders(rt.manager.snapshot()));
                        }
                    }
                }, delaySeconds * 20L);
            }
        }
    }

    private void kick(final Player p, Map<String, String> ph) {
        final String msg = rt.text.legacyKick(rt.kickMessage("kick", p, ph));
        rt.tasks.runEntity(p, new Runnable() {
            public void run() {
                p.kickPlayer(msg);
            }
        });
    }

    public void onDisabled(MaintenanceState s, boolean automatic, String by) {
        final Map<String, String> ph = Messages.ph("player", by == null ? "-" : by);
        final String key = automatic ? "disabled-auto" : "disabled";
        rt.notifier.disabled(SCOPE, automatic, by);
        rt.tasks.runMain(new Runnable() {
            public void run() {
                notifyAdmins(rt.messages.get(key, ph));
                clearBars();
                signalContexts();
            }
        });
    }

    public void onScheduled(MaintenanceState s) {
        rt.notifier.scheduled(SCOPE, s);
    }

    public void onScheduleCancelled() {
        rt.tasks.runMain(new Runnable() {
            public void run() {
                clearBars();
            }
        });
    }

    public void onWarning(long remainingMillis) {
        final Map<String, String> ph = Messages.ph("time_left", TimeFormatter.format(remainingMillis));
        rt.tasks.runMain(new Runnable() {
            public void run() {
                for (Player p : rt.plugin.getServer().getOnlinePlayers()) {
                    rt.text.message(p, rt.messages.get("warning", ph));
                    rt.text.title(p, rt.messages.raw("warning", ph), Component.empty());
                    SoundPlayer.play(p, "BLOCK_NOTE_BLOCK_PLING", "BLOCK_NOTE_PLING", "NOTE_PLING");
                }
            }
        });
    }

    public void onChanged() {
        rt.saveAsync();
    }

    private void clearBars() {
        for (Player p : rt.plugin.getServer().getOnlinePlayers()) {
            rt.bar.clear(p);
        }
    }

    private void notifyAdmins(Component c) {
        for (Player p : rt.plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("smaintenance.notify")) {
                rt.text.message(p, c);
            }
        }
        rt.text.message(rt.plugin.getServer().getConsoleSender(), c);
    }
}
