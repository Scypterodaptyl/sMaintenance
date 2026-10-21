package com.scypter.smaintenance.api;

import com.scypter.smaintenance.paper.PaperRuntime;

import java.util.UUID;

public final class SMaintenanceAPI {
    private static volatile PaperRuntime runtime;

    private SMaintenanceAPI() {
    }

    public static void init(PaperRuntime rt) {
        runtime = rt;
    }

    public static void shutdown() {
        runtime = null;
    }

    private static PaperRuntime rt() {
        PaperRuntime r = runtime;
        if (r == null) {
            throw new IllegalStateException("sMaintenance is not enabled");
        }
        return r;
    }

    public static boolean isEnabled() {
        return rt().manager.isEnabled();
    }

    public static boolean enable(String by, long durationMillis, String reason) {
        return rt().tryEnable(by, durationMillis, reason);
    }

    public static boolean disable(String by) {
        return rt().tryDisable(by);
    }

    public static boolean isWhitelisted(UUID id) {
        return rt().whitelist.contains(id);
    }

    public static long timeLeftMillis() {
        return rt().manager.timeLeft();
    }
}
