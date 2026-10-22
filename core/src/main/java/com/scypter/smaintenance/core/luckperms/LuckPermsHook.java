package com.scypter.smaintenance.core.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class LuckPermsHook {
    private final LuckPerms lp;
    private final BypassGroups groups;
    private ContextCalculator<Object> calculator;

    public LuckPermsHook(BypassGroups groups) {
        this.lp = LuckPermsProvider.get();
        this.groups = groups;
    }

    public static boolean present() {
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider");
            LuckPermsProvider.get();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public void preload(UUID id) {
        try {
            lp.getUserManager().loadUser(id).join();
        } catch (Exception ignored) {
        }
    }

    public boolean hasPermission(UUID id, String node) {
        User user = lp.getUserManager().getUser(id);
        if (user == null) {
            return false;
        }
        return user.getCachedData().getPermissionData(user.getQueryOptions()).checkPermission(node).asBoolean();
    }

    public boolean inBypassGroup(UUID id) {
        User user = lp.getUserManager().getUser(id);
        if (user == null) {
            return false;
        }
        for (Group g : user.getInheritedGroups(user.getQueryOptions())) {
            if (groups.contains(g.getName().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public CompletableFuture<UUID> lookup(String name) {
        return lp.getUserManager().lookupUniqueId(name);
    }

    public String prefix(UUID id) {
        return meta(id, true);
    }

    public String suffix(UUID id) {
        return meta(id, false);
    }

    private String meta(UUID id, boolean prefix) {
        User user = lp.getUserManager().getUser(id);
        if (user == null) {
            return "";
        }
        CachedMetaData data = user.getCachedData().getMetaData();
        String v = prefix ? data.getPrefix() : data.getSuffix();
        return v == null ? "" : v;
    }

    public void registerContexts(final BooleanSupplier enabled, final Supplier<Collection<String>> scopes) {
        calculator = new ContextCalculator<Object>() {
            public void calculate(Object target, ContextConsumer consumer) {
                consumer.accept("maintenance", String.valueOf(enabled.getAsBoolean()));
                Collection<String> s = scopes.get();
                if (s != null) {
                    for (String scope : s) {
                        consumer.accept("maintenance-server", scope);
                    }
                }
            }

            public ContextSet estimatePotentialContexts() {
                return net.luckperms.api.context.ImmutableContextSet.builder()
                        .add("maintenance", "true").add("maintenance", "false").build();
            }
        };
        lp.getContextManager().registerCalculator(calculator);
    }

    public void unregister() {
        if (calculator != null) {
            lp.getContextManager().unregisterCalculator(calculator);
        }
    }

    public void signalContextUpdate(Object subject) {
        lp.getContextManager().signalContextUpdate(subject);
    }
}
