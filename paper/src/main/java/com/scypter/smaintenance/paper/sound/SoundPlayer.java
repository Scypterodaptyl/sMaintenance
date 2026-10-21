package com.scypter.smaintenance.paper.sound;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class SoundPlayer {
    private static Class<?> soundClass;
    private static Method valueOf;
    private static Method playSound;
    private static boolean failed;

    private SoundPlayer() {
    }

    public static void play(Player p, String... candidates) {
        if (failed) {
            return;
        }
        try {
            if (soundClass == null) {
                soundClass = Class.forName("org.bukkit.Sound");
                valueOf = soundClass.getMethod("valueOf", String.class);
                playSound = Player.class.getMethod("playSound", Location.class, soundClass, float.class, float.class);
            }
            for (String name : candidates) {
                try {
                    Object sound = valueOf.invoke(null, name);
                    playSound.invoke(p, p.getLocation(), sound, 1f, 1f);
                    return;
                } catch (java.lang.reflect.InvocationTargetException ignored) {
                }
            }
        } catch (Throwable t) {
            failed = true;
        }
    }
}
