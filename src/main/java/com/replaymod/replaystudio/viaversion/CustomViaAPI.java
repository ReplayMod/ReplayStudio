/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.viaversion;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.replaymod.replaystudio.lib.viaversion.ViaAPIBase;
import com.replaymod.replaystudio.lib.viaversion.api.Via;
import com.replaymod.replaystudio.lib.viaversion.api.connection.UserConnection;

import java.util.SortedSet;
import java.util.UUID;

class CustomViaAPI extends ViaAPIBase<Void> {
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
        if (uuid.equals(userConnection.getProtocolInfo().getUuid())) {
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
        if (uuid.equals(userConnection.getProtocolInfo().getUuid())) {
            userConnection.sendRawPacket(byteBuf);
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<Integer> getSupportedVersions() {
        return Via.getManager().getProtocolManager().getSupportedVersions();
    }
}
