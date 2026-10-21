package com.scypter.smaintenance.core.luckperms;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ExtrasFactory {
    private ExtrasFactory() {
    }

    public static Extras create(BypassGroups groups, BooleanSupplier enabled, Supplier<Collection<String>> scopes) {
        try {
            if (LuckPermsHook.present()) {
                return new LuckPermsExtras(groups, enabled, scopes);
            }
        } catch (Throwable ignored) {
        }
        return new NoExtras();
    }
}
