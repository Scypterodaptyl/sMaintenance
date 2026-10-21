package com.scypter.smaintenance.velocity;

import com.scypter.smaintenance.core.proxy.ProxyMaintenance;
import com.scypter.smaintenance.core.state.MaintenanceState;
import com.scypter.smaintenance.core.time.TimeFormatter;
import io.github.miniplaceholders.api.Expansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;

import java.util.Map;

public final class MiniPlaceholdersSupport {
    private MiniPlaceholdersSupport() {
    }

    private static Tag text(String s) {
        return Tag.selfClosingInserting(Component.text(s));
    }

    public static void enable(final SMaintenanceVelocity plugin) {
        Expansion.builder("smaint")
                .globalPlaceholder("enabled", (queue, ctx) -> {
                    String scope = queue.hasNext() ? queue.pop().value() : ProxyMaintenance.GLOBAL;
                    return text(String.valueOf(plugin.pm().manager(scope).isEnabled()));
                })
                .globalPlaceholder("reason", (queue, ctx) -> text(value(plugin, queue.hasNext() ? queue.pop().value() : ProxyMaintenance.GLOBAL, "reason")))
                .globalPlaceholder("time_left", (queue, ctx) -> text(value(plugin, queue.hasNext() ? queue.pop().value() : ProxyMaintenance.GLOBAL, "time_left")))
                .globalPlaceholder("end_time", (queue, ctx) -> text(value(plugin, queue.hasNext() ? queue.pop().value() : ProxyMaintenance.GLOBAL, "end_time")))
                .globalPlaceholder("enabled_by", (queue, ctx) -> text(value(plugin, queue.hasNext() ? queue.pop().value() : ProxyMaintenance.GLOBAL, "enabled_by")))
                .globalPlaceholder("scheduled", (queue, ctx) -> {
                    String scope = queue.hasNext() ? queue.pop().value() : ProxyMaintenance.GLOBAL;
                    return text(String.valueOf(plugin.pm().manager(scope).startsIn() > 0));
                })
                .globalPlaceholder("starts_in", (queue, ctx) -> {
                    String scope = queue.hasNext() ? queue.pop().value() : ProxyMaintenance.GLOBAL;
                    long in = plugin.pm().manager(scope).startsIn();
                    return text(in > 0 ? TimeFormatter.format(in) : "-");
                })
                .build()
                .register();
    }

    private static String value(SMaintenanceVelocity plugin, String scope, String key) {
        MaintenanceState s = plugin.pm().manager(scope).snapshot();
        Map<String, String> ph = plugin.pm().placeholders(scope, s);
        String v = ph.get(key);
        return v == null ? "" : v;
    }
}
