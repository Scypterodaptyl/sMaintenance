package com.scypter.smaintenance.core.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FlatConfigTest {
    @Test void readsScalars() {
        FlatConfig c = FlatConfig.parse("# c\nlanguage: ru\nkick-delay: 5\nflag: true\nprefix: \"<gray>[x] \"\n");
        assertEquals("ru", c.getString("language", "en"));
        assertEquals(5, c.getInt("kick-delay", 0));
        assertTrue(c.getBoolean("flag", false));
        assertEquals("<gray>[x] ", c.getString("prefix", ""));
    }

    @Test void defaultsWhenMissingOrBad() {
        FlatConfig c = FlatConfig.parse("n: abc\n");
        assertEquals(7, c.getInt("n", 7));
        assertEquals(3, c.getInt("missing", 3));
        assertFalse(c.getBoolean("missing", false));
    }

    @Test void readsListsAndEscapes() {
        FlatConfig c = FlatConfig.parse("fallback-servers: [lobby, \"hub\"]\nmsg: \"a\\nb: c\"\n");
        assertEquals(Arrays.asList("lobby", "hub"), c.getList("fallback-servers"));
        assertEquals("a\nb: c", c.getString("msg", ""));
    }

    @Test void emptyListWhenAbsent() {
        assertTrue(FlatConfig.parse("").getList("x").isEmpty());
    }
}
