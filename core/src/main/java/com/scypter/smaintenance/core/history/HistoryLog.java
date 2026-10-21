package com.scypter.smaintenance.core.history;

import java.util.ArrayList;
import java.util.List;

public final class HistoryLog {
    private final List<HistoryEntry> backing;
    private final int max;

    public HistoryLog(List<HistoryEntry> backing, int max) {
        this.backing = backing;
        this.max = max;
    }

    public synchronized void add(HistoryEntry e) {
        backing.add(e);
        while (backing.size() > max) {
            backing.remove(0);
        }
    }

    public synchronized List<HistoryEntry> last(int n) {
        List<HistoryEntry> out = new ArrayList<HistoryEntry>();
        for (int i = backing.size() - 1; i >= 0 && out.size() < n; i--) {
            out.add(backing.get(i));
        }
        return out;
    }

    public synchronized List<HistoryEntry> snapshot() {
        return new ArrayList<HistoryEntry>(backing);
    }
}
