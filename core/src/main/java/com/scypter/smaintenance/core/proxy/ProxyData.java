package com.scypter.smaintenance.core.proxy;

import com.scypter.smaintenance.core.state.MaintenanceState;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProxyData {
    public Map<String, MaintenanceState> states = new LinkedHashMap<String, MaintenanceState>();
    public Map<String, Map<String, String>> whitelists = new LinkedHashMap<String, Map<String, String>>();
    public java.util.List<com.scypter.smaintenance.core.history.HistoryEntry> history = new java.util.ArrayList<com.scypter.smaintenance.core.history.HistoryEntry>();
    public java.util.List<String> bypassGroups = new java.util.ArrayList<String>();
}
