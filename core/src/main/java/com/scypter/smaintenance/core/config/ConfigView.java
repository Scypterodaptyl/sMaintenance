package com.scypter.smaintenance.core.config;

public interface ConfigView {
    String getString(String key, String def);

    int getInt(String key, int def);

    boolean getBoolean(String key, boolean def);
}
