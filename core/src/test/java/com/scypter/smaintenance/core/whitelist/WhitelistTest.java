package com.scypter.smaintenance.core.whitelist;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WhitelistTest {
    @Test void addContainsRemove() {
        Whitelist w = new Whitelist(new LinkedHashMap<String, String>());
        UUID id = UUID.randomUUID();
        assertTrue(w.add(id, "Steve"));
        assertFalse(w.add(id, "Steve"));
        assertTrue(w.contains(id));
        assertEquals(1, w.size());
        assertTrue(w.remove(id));
        assertFalse(w.contains(id));
    }

    @Test void removeByNameIgnoresCase() {
        Whitelist w = new Whitelist(new LinkedHashMap<String, String>());
        UUID id = UUID.randomUUID();
        w.add(id, "Steve");
        assertEquals(id, w.removeByName("sTeVe"));
        assertNull(w.removeByName("Steve"));
    }

    @Test void clearEmpties() {
        Whitelist w = new Whitelist(new LinkedHashMap<String, String>());
        w.add(UUID.randomUUID(), "A");
        w.clear();
        assertEquals(0, w.size());
    }
}
