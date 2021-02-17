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
package com.replaymod.replaystudio.filter;

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.stream.PacketStream;

import java.util.function.Predicate;

public class RemoveFilter implements StreamFilter {

    private Predicate<PacketData> filter = packetData -> true;

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        if (config.has("type")) {
            String name = config.get("type").getAsString();
            PacketType type = PacketType.valueOf(name);
            filter = (d) -> d.getPacket().getType() == type;
        }
    }

    @Override
    public void onStart(PacketStream stream) {

    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        return !filter.test(data);
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {

    }
}
