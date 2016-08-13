/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.mock;


import com.google.common.base.Function;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.event.session.SessionEvent;
import org.spacehq.packetlib.event.session.SessionListener;
import org.spacehq.packetlib.packet.Packet;

import java.util.List;
import java.util.Map;

public class SessionMock<P extends MinecraftProtocol> implements Session {
    private final P packetProtocol;

    private int compressionThreshold;

    public SessionMock(Function<SessionMock, P> protocol) {
        packetProtocol = protocol.apply(this);
    }

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
    public boolean isConnected() {
        return false;
    }

    @Override
    public void send(Packet packet) {

    }

    @Override
    public void disconnect(String s) {

    }

    @Override
    public void disconnect(String s, boolean b) {

    }

    @Override
    public void disconnect(String s, Throwable throwable) {

    }

    @Override
    public void disconnect(String s, Throwable throwable, boolean b) {

    }

    public P getPacketProtocol() {
        return this.packetProtocol;
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }

    public void setCompressionThreshold(int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
    }

    @Override
    public int getConnectTimeout() {
        return 0;
    }

    @Override
    public void setConnectTimeout(int i) {

    }
}
