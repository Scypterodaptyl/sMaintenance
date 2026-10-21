package com.scypter.smaintenance.core.state;

public final class MaintenanceState {
    private boolean enabled;
    private String reason;
    private String enabledBy;
    private long endAt;
    private long scheduledStart;
    private long scheduledDuration;
    private String scheduledReason;
    private String scheduledBy;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { reason = v; }
    public String getEnabledBy() { return enabledBy; }
    public void setEnabledBy(String v) { enabledBy = v; }
    public long getEndAt() { return endAt; }
    public void setEndAt(long v) { endAt = v; }
    public long getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(long v) { scheduledStart = v; }
    public long getScheduledDuration() { return scheduledDuration; }
    public void setScheduledDuration(long v) { scheduledDuration = v; }
    public String getScheduledReason() { return scheduledReason; }
    public void setScheduledReason(String v) { scheduledReason = v; }
    public String getScheduledBy() { return scheduledBy; }
    public void setScheduledBy(String v) { scheduledBy = v; }

    public MaintenanceState copy() {
        MaintenanceState c = new MaintenanceState();
        c.enabled = enabled;
        c.reason = reason;
        c.enabledBy = enabledBy;
        c.endAt = endAt;
        c.scheduledStart = scheduledStart;
        c.scheduledDuration = scheduledDuration;
        c.scheduledReason = scheduledReason;
        c.scheduledBy = scheduledBy;
        return c;
    }
}
