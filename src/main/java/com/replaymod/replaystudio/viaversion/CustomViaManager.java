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

import com.replaymod.replaystudio.us.myles.ViaVersion.ViaManager;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Via;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.data.UserConnection;
import com.replaymod.replaystudio.us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class CustomViaManager extends ViaManager {
    public static synchronized void initialize() {
        if (Via.getPlatform() != null) return;
        Via.init(new CustomViaManager());
    }

    private CustomViaManager() {
        super(new CustomViaPlatform(), new CustomViaInjector(), null, null);
    }

    @Override
    public Map<UUID, UserConnection> getPortedPlayers() {
        UserConnection user = CustomViaAPI.INSTANCE.get().user();
        UUID uuid = user.get(ProtocolInfo.class).getUuid();
        return Collections.singletonMap(uuid, user);
    }

    @Override
    public UserConnection getConnection(UUID playerUUID) {
        UserConnection user = CustomViaAPI.INSTANCE.get().user();
        if (playerUUID.equals(user.get(ProtocolInfo.class).getUuid())) {
            return user;
        }
        throw new UnsupportedOperationException();
    }
}