package com.scypter.smaintenance.core.proxy;

import com.scypter.smaintenance.core.TimeSource;
import com.scypter.smaintenance.core.state.MaintenanceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProxyMaintenanceTest {
    ProxyMaintenance pm;
    int changes;
    long now = 1_000_000L;

    static PermissionChecker perms(String... nodes) {
        final Set<String> set = new HashSet<String>(Arrays.asList(nodes));
        return new PermissionChecker() {
            public boolean has(String node) { return set.contains(node); }
        };
    }

    @BeforeEach void setUp() {
        pm = new ProxyMaintenance(new TimeSource() {
            public long now() { return now; }
        }, new ProxyData(), new ProxyListener() {
            public void onEnabled(String scope, MaintenanceState s) { }
            public void onDisabled(String scope, MaintenanceState s, boolean auto, String by) { }
            public void onScheduled(String scope, MaintenanceState s) { }
            public void onScheduleCancelled(String scope) { }
            public void onWarning(String scope, long left) { }
            public void onChanged() { changes++; }
        }, new long[]{}, "Technical works");
    }

    @Test void openWhenNothingEnabled() {
        assertNull(pm.closingScope("survival", UUID.randomUUID(), perms()));
    }

    @Test void serverScopeClosesOnlyThatServer() {
        pm.manager("survival").enable("a", 0, "x");
        UUID id = UUID.randomUUID();
        assertEquals("survival", pm.closingScope("Survival", id, perms()));
        assertNull(pm.closingScope("lobby", id, perms()));
    }

    @Test void globalClosesEverything() {
        pm.manager("global").enable("a", 0, "x");
        UUID id = UUID.randomUUID();
        assertEquals("global", pm.closingScope("lobby", id, perms()));
        assertEquals("global", pm.globalBlock(id, perms()));
    }

    @Test void bypassByPermissionAndWhitelist() {
        pm.manager("survival").enable("a", 0, "x");
        UUID id = UUID.randomUUID();
        assertNull(pm.closingScope("survival", id, perms("smaintenance.bypass")));
        assertNull(pm.closingScope("survival", id, perms("smaintenance.bypass.survival")));
        assertEquals("survival", pm.closingScope("survival", id, perms("smaintenance.bypass.lobby")));
        pm.whitelist("survival").add(id, "Steve");
        assertNull(pm.closingScope("survival", id, perms()));
    }

    @Test void globalWhitelistBypassesServerScope() {
        pm.manager("survival").enable("a", 0, "x");
        UUID id = UUID.randomUUID();
        pm.whitelist("global").add(id, "Steve");
        assertNull(pm.closingScope("survival", id, perms()));
    }

    @Test void globalBypassPermissionOnlyForGlobal() {
        pm.manager("global").enable("a", 0, "x");
        UUID id = UUID.randomUUID();
        assertNull(pm.globalBlock(id, perms("smaintenance.bypass.global")));
        assertEquals("global", pm.globalBlock(id, perms("smaintenance.bypass.lobby")));
    }

    @Test void fallbackSkipsClosedAndExcluded() {
        pm.manager("survival").enable("a", 0, "x");
        pm.manager("hub").enable("a", 0, "x");
        UUID id = UUID.randomUUID();
        assertEquals("lobby2", pm.fallbackFor(Arrays.asList("hub", "lobby", "lobby2"), "lobby", id, perms()));
        assertNull(pm.fallbackFor(Arrays.asList("hub", "survival"), "x", id, perms()));
    }

    @Test void enabledScopesAndSnapshot() {
        pm.manager("survival").enable("a", 0, "why");
        pm.whitelist("global").add(UUID.randomUUID(), "A");
        assertEquals(Arrays.asList("survival"), pm.enabledScopes());
        ProxyData snap = pm.snapshot();
        assertTrue(snap.states.get("survival").isEnabled());
        assertEquals(1, snap.whitelists.get("global").size());
        assertTrue(changes > 0);
    }

    @Test void restoresFromData() {
        ProxyData d = new ProxyData();
        MaintenanceState s = new MaintenanceState();
        s.setEnabled(true);
        d.states.put("survival", s);
        ProxyMaintenance restored = new ProxyMaintenance(new TimeSource() {
            public long now() { return now; }
        }, d, new ProxyListener() {
            public void onEnabled(String scope, MaintenanceState st) { }
            public void onDisabled(String scope, MaintenanceState st, boolean auto, String by) { }
            public void onScheduled(String scope, MaintenanceState st) { }
            public void onScheduleCancelled(String scope) { }
            public void onWarning(String scope, long left) { }
            public void onChanged() { }
        }, new long[]{}, "x");
        assertEquals(Arrays.asList("survival"), restored.enabledScopes());
    }
}
