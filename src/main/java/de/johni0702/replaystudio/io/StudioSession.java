package de.johni0702.replaystudio.io;

import lombok.Getter;
import lombok.Setter;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.TimeoutHandler;
import org.spacehq.packetlib.event.session.SessionEvent;
import org.spacehq.packetlib.event.session.SessionListener;
import org.spacehq.packetlib.packet.Packet;

import java.util.List;
import java.util.Map;

public class StudioSession implements Session {

    @Getter
    private StudioMinecraftProtocol packetProtocol;

    public StudioSession(boolean client) {
        packetProtocol = new StudioMinecraftProtocol(this, client);
    }

    @Getter
    @Setter
    private int compressionThreshold;

    @Override
    public void connect() {

    }

    @Override
    public void connect(boolean b) {

    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public Map<String, Object> getFlags() {
        return null;
    }

    @Override
    public boolean hasFlag(String s) {
        return false;
    }

    @Override
    public <T> T getFlag(String s) {
        return null;
    }

    @Override
    public void setFlag(String s, Object o) {

    }

    @Override
    public List<SessionListener> getListeners() {
        return null;
    }

    @Override
    public void addListener(SessionListener sessionListener) {

    }

    @Override
    public void removeListener(SessionListener sessionListener) {

    }

    @Override
    public void callEvent(SessionEvent sessionEvent) {

    }

    @Override
    public int getReadTimeout() {
        return 0;
    }

    @Override
    public void setReadTimeout(int i) {

    }

    @Override
    public int getWriteTimeout() {
        return 0;
    }

    @Override
    public void setWriteTimeout(int i) {

    }

    @Override
    public TimeoutHandler getTimeoutHandler() {
        return null;
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler timeoutHandler) {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void send(Packet packet) {

    }

    @Override
    public void disconnect(String s) {

    }

}

