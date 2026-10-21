package com.scypter.smaintenance.core.proxy;

import com.scypter.smaintenance.core.TimeSource;
import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.core.state.MaintenanceState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ProxyCommandHandlerTest {
    ProxyMaintenance pm;
    ProxyCommandHandler h;
    List<String> out;
    boolean allowed;
    int saves;
    int reloads;
    boolean lp;

    ProxyCommandHandler.CommandSource src = new ProxyCommandHandler.CommandSource() {
        public String name() { return "Admin"; }
        public boolean has(String node) { return allowed; }
        public void send(Component c) { out.add(PlainTextComponentSerializer.plainText().serialize(c)); }
    };

    @BeforeEach void setUp() {
        allowed = true;
        out = new ArrayList<String>();
        saves = 0;
        reloads = 0;
        pm = new ProxyMaintenance(new TimeSource() {
            public long now() { return 1_000_000L; }
        }, new ProxyData(), new ProxyListener() {
            public void onEnabled(String scope, MaintenanceState s) { }
            public void onDisabled(String scope, MaintenanceState s, boolean auto, String by) { }
            public void onScheduled(String scope, MaintenanceState s) { }
            public void onScheduleCancelled(String scope) { }
            public void onWarning(String scope, long left) { }
            public void onChanged() { saves++; }
        }, new long[]{}, "Technical works");
        final Map<String, String> t = new HashMap<String, String>();
        t.put("status-on", "ON {server} {reason}");
        t.put("status-off", "OFF {server}");
        t.put("unknown-scope", "unknown {server}");
        t.put("no-permission", "denied");
        t.put("invalid-time", "badtime");
        t.put("list-empty", "empty");
        t.put("list-line", "line {server}");
        t.put("whitelist-added", "added {player}");
        t.put("whitelist-unknown-player", "nobody {player}");
        t.put("whitelist-list", "wl {count}");
        t.put("scheduled", "sched {server} {time_left}");
        t.put("reloaded", "reloaded");
        t.put("already-enabled", "already");
        t.put("extended", "extended {time_left}");
        t.put("not-enabled", "notenabled");
        h = new ProxyCommandHandler(pm, new Supplier<Messages>() {
            public Messages get() { return new Messages(t, ""); }
        }, new ProxyCommandHandler.ServerDirectory() {
            public Collection<String> names() { return Arrays.asList("survival", "lobby"); }
        }, new ProxyCommandHandler.UuidResolver() {
            public void resolve(String name, Callback cb) {
                cb.done(name.equals("Ghost") ? null : UUID.nameUUIDFromBytes(name.getBytes()), name);
            }
        }, new Runnable() {
            public void run() { reloads++; }
        }, new com.scypter.smaintenance.core.luckperms.BypassGroups(new ArrayList<String>(), new Runnable() {
            public void run() { }
        }), new java.util.function.BooleanSupplier() {
            public boolean getAsBoolean() { return lp; }
        });
    }

    @Test void enableServerAndStatus() {
        h.execute(src, new String[]{"survival", "on", "10m", "upgrade", "now"});
        assertTrue(pm.manager("survival").isEnabled());
        assertEquals("upgrade now", pm.manager("survival").snapshot().getReason());
        h.execute(src, new String[]{"survival", "status"});
        assertEquals("ON survival upgrade now", out.get(out.size() - 1));
        assertFalse(pm.manager("lobby").isEnabled());
    }

    @Test void globalOffAndList() {
        h.execute(src, new String[]{"list"});
        assertEquals("empty", out.get(0));
        h.execute(src, new String[]{"global", "on"});
        h.execute(src, new String[]{"list"});
        assertEquals("line global", out.get(out.size() - 1));
        h.execute(src, new String[]{"global", "off"});
        assertFalse(pm.manager("global").isEnabled());
    }

    @Test void unknownServerRejected() {
        h.execute(src, new String[]{"nope", "on"});
        assertEquals("unknown nope", out.get(0));
        assertTrue(pm.enabledScopes().isEmpty());
    }

    @Test void invalidTimeRejectedWithoutChange() {
        h.execute(src, new String[]{"survival", "on", "0s"});
        h.execute(src, new String[]{"survival", "on", "-5m"});
        assertEquals(Arrays.asList("badtime", "badtime"), out);
        assertFalse(pm.manager("survival").isEnabled());
    }

    @Test void extendRejectedWhenOff() {
        h.execute(src, new String[]{"survival", "extend", "5m"});
        assertEquals("notenabled", out.get(0));
    }

    @Test void noPermissionBlocks() {
        allowed = false;
        h.execute(src, new String[]{"survival", "on"});
        assertEquals("denied", out.get(0));
        assertFalse(pm.manager("survival").isEnabled());
    }

    @Test void scheduleAndWhitelist() {
        h.execute(src, new String[]{"schedule", "lobby", "30s", "1m", "why"});
        assertEquals("sched lobby 30s", out.get(0));
        assertEquals(31_000L - 1_000L, pm.manager("lobby").startsIn());
        h.execute(src, new String[]{"whitelist", "global", "add", "Steve"});
        h.execute(src, new String[]{"whitelist", "global", "add", "Ghost"});
        h.execute(src, new String[]{"whitelist", "global", "list"});
        assertEquals(Arrays.asList("sched lobby 30s", "added Steve", "nobody Ghost", "wl 1"), out);
        assertTrue(saves > 0);
    }

    @Test void groupCommandsNeedLuckPerms() {
        h.execute(src, new String[]{"whitelist", "addgroup", "admin"});
        assertEquals("missing message: luckperms-missing", out.get(0));
    }

    @Test void groupCommandsWorkWithLuckPerms() {
        lp = true;
        h.execute(src, new String[]{"whitelist", "addgroup", "admin"});
        h.execute(src, new String[]{"whitelist", "removegroup", "admin"});
        h.execute(src, new String[]{"whitelist", "removegroup", "admin"});
        assertEquals(Arrays.asList("missing message: group-added", "missing message: group-removed", "missing message: group-missing"), out);
    }

    @Test void reloadRuns() {
        h.execute(src, new String[]{"reload"});
        assertEquals(1, reloads);
    }

    @Test void tabCompletion() {
        assertTrue(h.complete(src, new String[]{"su"}).contains("survival"));
        assertTrue(h.complete(src, new String[]{"survival", "o"}).containsAll(Arrays.asList("on", "off")));
        assertTrue(h.complete(src, new String[]{"whitelist", "global", "a"}).contains("add"));
    }
}
