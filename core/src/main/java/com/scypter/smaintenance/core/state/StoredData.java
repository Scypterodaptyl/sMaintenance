package com.scypter.smaintenance.core.state;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StoredData {
    public MaintenanceState state = new MaintenanceState();
    public Map<String, String> whitelist = new LinkedHashMap<String, String>();
    public java.util.List<com.scypter.smaintenance.core.history.HistoryEntry> history = new java.util.ArrayList<com.scypter.smaintenance.core.history.HistoryEntry>();
    public java.util.List<String> bypassGroups = new java.util.ArrayList<String>();
}
