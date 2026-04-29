package com.echoim.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "echoim.call")
public class CallProperties {

    private int ringTimeoutSeconds = 30;
    private List<IceServerProperties> iceServers = new ArrayList<>(List.of(defaultStun()));

    public int getRingTimeoutSeconds() {
        return ringTimeoutSeconds;
    }

    public void setRingTimeoutSeconds(int ringTimeoutSeconds) {
        this.ringTimeoutSeconds = ringTimeoutSeconds;
    }

    public List<IceServerProperties> getIceServers() {
        return iceServers;
    }

    public void setIceServers(List<IceServerProperties> iceServers) {
        this.iceServers = iceServers == null || iceServers.isEmpty() ? List.of(defaultStun()) : iceServers;
    }

    public static class IceServerProperties {
        private List<String> urls = new ArrayList<>();
        private String username;
        private String credential;

        public List<String> getUrls() {
            return urls;
        }

        public void setUrls(List<String> urls) {
            this.urls = urls == null ? List.of() : urls;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCredential() {
            return credential;
        }

        public void setCredential(String credential) {
            this.credential = credential;
        }
    }

    private static IceServerProperties defaultStun() {
        IceServerProperties server = new IceServerProperties();
        server.setUrls(List.of("stun:stun.l.google.com:19302"));
        return server;
    }
}
