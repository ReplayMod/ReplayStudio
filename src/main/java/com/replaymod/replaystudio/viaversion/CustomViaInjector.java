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

import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.lib.viaversion.libs.gson.JsonObject;
import com.replaymod.replaystudio.lib.viaversion.api.platform.ViaInjector;

public class CustomViaInjector implements ViaInjector {
    @Override
    public void inject() throws Exception {}

    @Override
    public void uninject() throws Exception {}

    @Override
    public ProtocolVersion getServerProtocolVersion() throws Exception {
        return ProtocolVersion.unknown;
    }

    @Override
    public String getEncoderName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDecoderName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonObject getDump() {
        throw new UnsupportedOperationException();
    }
}
