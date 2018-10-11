package com.sangha.weather.config;

public class Device {
    private String type = null;
    private String label = null;
    private String ip = null;
    private int port = -1;
    private String path = null;
    private String sha256 = null;

    public String getType() { return type; }
    public void setDevicetype(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
}
