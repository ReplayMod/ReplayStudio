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

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.platform.ViaInjector;

public class CustomViaInjector implements ViaInjector {
    private final int serverProtocolVersion = new ReplayStudio().getCurrentFileFormatVersion();

    @Override
    public void inject() throws Exception {}

    @Override
    public void uninject() throws Exception {}

    @Override
    public int getServerProtocolVersion() throws Exception {
        return serverProtocolVersion;
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
