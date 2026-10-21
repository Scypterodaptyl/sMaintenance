package com.scypter.smaintenance.core.history;

public final class HistoryEntry {
    public long at;
    public String scope;
    public String by;
    public String reason;
    public long durationMs;

    public HistoryEntry() {
    }

    public HistoryEntry(long at, String scope, String by, String reason, long durationMs) {
        this.at = at;
        this.scope = scope;
        this.by = by;
        this.reason = reason;
        this.durationMs = durationMs;
    }
}
