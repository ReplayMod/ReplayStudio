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
package com.replaymod.replaystudio.viaversion;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Via;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.ViaAPI;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.boss.BossBar;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.boss.BossColor;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.boss.BossStyle;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.data.UserConnection;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import com.replaymod.replaystudio.us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.util.SortedSet;
import java.util.UUID;

class CustomViaAPI implements ViaAPI<Void> {
    static final ThreadLocal<CustomViaAPI> INSTANCE = new ThreadLocal<>();

    private final int sourceVersion;
    private final UserConnection userConnection;

    CustomViaAPI(int sourceVersion, UserConnection userConnection) {
        this.sourceVersion = sourceVersion;
        this.userConnection = userConnection;
    }

    UserConnection user() {
        return userConnection;
    }

    @Override
    public int getPlayerVersion(Void aVoid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPlayerVersion(UUID uuid) {
        if (uuid.equals(userConnection.get(ProtocolInfo.class).getUuid())) {
            return sourceVersion;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInjected(UUID uuid) {
        return sourceVersion >= 107;
    }

    @Override
    public String getVersion() {
        return Via.getPlatform().getPluginVersion();
    }

    @Override
    public void sendRawPacket(Void aVoid, ByteBuf byteBuf) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRawPacket(UUID uuid, ByteBuf byteBuf) throws IllegalArgumentException {
        if (uuid.equals(userConnection.get(ProtocolInfo.class).getUuid())) {
            userConnection.sendRawPacket(byteBuf);
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public BossBar createBossBar(String title, BossColor color, BossStyle style) {
        return createBossBar(title, 1, color, style);
    }

    @Override
    public BossBar createBossBar(String title, float health, BossColor color, BossStyle style) {
        return new CustomBossBar(title, health, color, style);
    }

    @Override
    public SortedSet<Integer> getSupportedVersions() {
        return ProtocolRegistry.getSupportedVersions();
    }
}
