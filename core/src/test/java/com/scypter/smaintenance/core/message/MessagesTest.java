package com.scypter.smaintenance.core.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessagesTest {
    private String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    @Test void replacesPlaceholders() {
        Map<String, String> t = new HashMap<String, String>();
        t.put("kick", "<red>Closed: {reason} ({time_left})");
        Messages m = new Messages(t, "[M] ");
        assertEquals("[M] Closed: upgrade (5m)", plain(m.get("kick", Messages.ph("reason", "upgrade", "time_left", "5m"))));
    }

    @Test void escapesInjectedTags() {
        Map<String, String> t = new HashMap<String, String>();
        t.put("kick", "Closed: {reason}");
        Messages m = new Messages(t, "");
        assertEquals("Closed: <red>hack</red>", plain(m.raw("kick", Messages.ph("reason", "<red>hack</red>"))));
    }

    @Test void missingKeyDoesNotThrow() {
        Messages m = new Messages(new HashMap<String, String>(), "");
        assertTrue(plain(m.raw("nope", Messages.ph())).contains("nope"));
    }
}
