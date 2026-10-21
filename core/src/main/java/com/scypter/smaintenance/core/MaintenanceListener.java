package com.scypter.smaintenance.core;

import com.scypter.smaintenance.core.state.MaintenanceState;

public interface MaintenanceListener {
    void onEnabled(MaintenanceState state);

    void onDisabled(MaintenanceState state, boolean automatic, String by);

    void onScheduled(MaintenanceState state);

    void onScheduleCancelled();

    void onWarning(long remainingMillis);

    void onChanged();
}
