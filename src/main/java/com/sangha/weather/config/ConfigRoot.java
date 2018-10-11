package com.sangha.weather.config;

import java.util.Map;

public class ConfigRoot {
    private String siteName;
    private String defaultPath;
    private String[] devices;

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSiteName() {
        return this.siteName;
    }

    public void setDefaultPath(String defaultPath) {
        this.defaultPath = defaultPath;
    }

    public String getDefaultPath() {
        return this.defaultPath;
    }

    public void setDevices(String[] devices) {
        this.devices = devices;
    }

    public String[] getDevices() {
        return this.devices;
    }
}
