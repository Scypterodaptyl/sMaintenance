package com.scypter.smaintenance.core.luckperms;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BypassGroups {
    private final List<String> groups = new CopyOnWriteArrayList<String>();
    private final Runnable onChange;

    public BypassGroups(List<String> initial, Runnable onChange) {
        for (String g : initial) {
            add0(g);
        }
        this.onChange = onChange;
    }

    private boolean add0(String g) {
        String n = g.toLowerCase(Locale.ROOT);
        if (groups.contains(n)) {
            return false;
        }
        groups.add(n);
        return true;
    }

    public boolean add(String g) {
        boolean changed = add0(g);
        if (changed) onChange.run();
        return changed;
    }

    public boolean remove(String g) {
        boolean changed = groups.remove(g.toLowerCase(Locale.ROOT));
        if (changed) onChange.run();
        return changed;
    }

    public boolean contains(String g) {
        return groups.contains(g.toLowerCase(Locale.ROOT));
    }

    public List<String> list() {
        return new ArrayList<String>(groups);
    }
}
