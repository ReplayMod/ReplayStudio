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

import com.replaymod.replaystudio.us.myles.ViaVersion.ViaManager;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Via;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.data.UserConnection;
import com.replaymod.replaystudio.us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

class CustomViaManager extends ViaManager {
    static synchronized void initialize() {
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