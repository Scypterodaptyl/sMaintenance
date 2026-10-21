package com.scypter.smaintenance.core.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StateStoreTest {
    @TempDir Path dir;

    @Test void roundTrip() {
        StateStore store = new StateStore(dir.resolve("data.json"));
        StoredData data = new StoredData();
        data.state.setEnabled(true);
        data.state.setReason("upgrade");
        data.state.setEndAt(12345L);
        data.whitelist.put("00000000-0000-0000-0000-000000000001", "Steve");
        store.save(data);
        StoredData loaded = store.load();
        assertTrue(loaded.state.isEnabled());
        assertEquals("upgrade", loaded.state.getReason());
        assertEquals(12345L, loaded.state.getEndAt());
        assertEquals("Steve", loaded.whitelist.get("00000000-0000-0000-0000-000000000001"));
    }

    @Test void missingFileGivesDefaults() {
        StoredData loaded = new StateStore(dir.resolve("none.json")).load();
        assertFalse(loaded.state.isEnabled());
        assertTrue(loaded.whitelist.isEmpty());
    }

    @Test void corruptFileGivesDefaults() throws Exception {
        Path f = dir.resolve("data.json");
        Files.write(f, "{not json".getBytes(StandardCharsets.UTF_8));
        StoredData loaded = new StateStore(f).load();
        assertFalse(loaded.state.isEnabled());
    }

    @Test void emptyFileGivesDefaults() throws Exception {
        Path f = dir.resolve("data.json");
        Files.write(f, new byte[0]);
        assertFalse(new StateStore(f).load().state.isEnabled());
    }
}
