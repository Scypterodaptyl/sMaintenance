package com.scypter.smaintenance.paper.papi;

import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.time.TimeFormatter;
import com.scypter.smaintenance.paper.PaperRuntime;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public final class SMaintExpansion extends PlaceholderExpansion {
    private final PaperRuntime rt;

    public SMaintExpansion(PaperRuntime rt) {
        this.rt = rt;
    }

    @Override
    public String getIdentifier() {
        return "smaint";
    }

    @Override
    public String getAuthor() {
        return "ScypterV";
    }

    @Override
    public String getVersion() {
        return rt.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        MaintenanceState s = rt.manager.snapshot();
        Map<String, String> ph = rt.placeholders(s);
        String key = params.toLowerCase();
        if (key.equals("enabled")) return String.valueOf(s.isEnabled());
        if (key.equals("enabled_formatted")) {
            return s.isEnabled()
                    ? rt.plugin.getConfig().getString("papi-enabled-text", "On")
                    : rt.plugin.getConfig().getString("papi-disabled-text", "Off");
        }
        if (key.equals("reason")) return ph.get("reason");
        if (key.equals("time_left")) return ph.get("time_left");
        if (key.equals("end_time")) return ph.get("end_time");
        if (key.equals("enabled_by")) return ph.get("enabled_by");
        if (key.equals("scheduled")) return String.valueOf(s.getScheduledStart() > 0);
        if (key.equals("starts_in")) {
            long in = rt.manager.startsIn();
            return in > 0 ? TimeFormatter.format(in) : "-";
        }
        if (player == null) {
            return null;
        }
        UUID id = player.getUniqueId();
        if (key.equals("whitelisted")) return String.valueOf(rt.whitelist.contains(id));
        if (key.equals("bypass")) {
            boolean perm = player.isOnline() && player.getPlayer().hasPermission("smaintenance.bypass");
            return String.valueOf(perm || rt.whitelist.contains(id) || rt.extras.inBypassGroup(id));
        }
        return null;
    }
}
