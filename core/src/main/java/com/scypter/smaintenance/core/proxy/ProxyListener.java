package com.scypter.smaintenance.core.proxy;

import com.scypter.smaintenance.core.state.MaintenanceState;

public interface ProxyListener {
    void onEnabled(String scope, MaintenanceState state);

    void onDisabled(String scope, MaintenanceState state, boolean automatic, String by);

    void onScheduled(String scope, MaintenanceState state);

    void onScheduleCancelled(String scope);

    void onWarning(String scope, long remainingMillis);

    void onChanged();
}
