package com.scypter.smaintenance.core.notify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.scypter.smaintenance.core.config.ConfigView;
import com.scypter.smaintenance.core.config.FlatConfig;
import com.scypter.smaintenance.core.state.MaintenanceState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class NotifierTest {
    @TempDir Path dir;

    @Test void jsonHasEmbedAndRoleMention() {
        JsonObject o = new JsonParser().parse(DiscordWebhook.buildJson("T", "D", 255, "123")).getAsJsonObject();
        assertEquals("<@&123>", o.get("content").getAsString());
        assertEquals(255, o.getAsJsonArray("embeds").get(0).getAsJsonObject().get("color").getAsInt());
    }

    @Test void jsonWithoutRoleHasNoContent() {
        JsonObject o = new JsonParser().parse(DiscordWebhook.buildJson("T", "D", 1, "")).getAsJsonObject();
        assertFalse(o.has("content"));
    }

    @Test void parsesColors() {
        assertEquals(0xff0000, Notifier.parseColor("0xff0000"));
        assertEquals(0x00ff00, Notifier.parseColor("#00ff00"));
        assertEquals(10, Notifier.parseColor("10"));
        assertEquals(0x2ecc71, Notifier.parseColor("bad"));
    }

    @Test void writesLogAndSkipsDiscordWhenDisabled() throws Exception {
        final List<Runnable> queued = new ArrayList<Runnable>();
        Path file = dir.resolve("logs/maintenance.log");
        final FlatConfig cfg = FlatConfig.parse("discord-enabled: false\n");
        Notifier n = new Notifier(new Executor() {
            public void execute(Runnable r) { queued.add(r); }
        }, new Consumer<String>() {
            public void accept(String s) { }
        }, new AuditLog(file), new Supplier<ConfigView>() {
            public ConfigView get() { return cfg; }
        });
        MaintenanceState s = new MaintenanceState();
        s.setEnabledBy("Admin");
        s.setReason("upgrade");
        n.enabled("global", s);
        n.disabled("global", true, null);
        String text = new String(Files.readAllBytes(file), "UTF-8");
        assertTrue(text.contains("ENABLED scope=global by=Admin"));
        assertTrue(text.contains("DISABLED scope=global by=timer"));
        assertTrue(queued.isEmpty());
    }

    @Test void queuesDiscordWhenEnabled() {
        final List<Runnable> queued = new ArrayList<Runnable>();
        final FlatConfig cfg = FlatConfig.parse("discord-enabled: true\ndiscord-url: http://localhost:1/x\n");
        Notifier n = new Notifier(new Executor() {
            public void execute(Runnable r) { queued.add(r); }
        }, new Consumer<String>() {
            public void accept(String s) { }
        }, new AuditLog(dir.resolve("a.log")), new Supplier<ConfigView>() {
            public ConfigView get() { return cfg; }
        });
        n.enabled("global", new MaintenanceState());
        assertEquals(1, queued.size());
    }
}
