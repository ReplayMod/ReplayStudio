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
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.packets.PacketJoinGame;
import com.replaymod.replaystudio.protocol.packets.PacketRespawn;
import com.replaymod.replaystudio.protocol.registry.DimensionType;
import com.replaymod.replaystudio.protocol.registry.Registries;
import com.replaymod.replaystudio.protocol.registry.RegistriesBuilder;
import com.replaymod.replaystudio.stream.PacketStream;

import java.io.IOException;

public class DimensionTracker implements StreamFilter {

    private final RegistriesBuilder registriesBuilder = new RegistriesBuilder();
    public Registries registries = new Registries();
    public String dimension;
    public DimensionType dimensionType;

    @Override
    public void onStart(PacketStream stream) {
    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) throws IOException {
        Packet packet = data.getPacket();
        PacketType type = packet.getType();

        switch (type) {
            case ConfigCustomPayload:
            case ConfigSelectKnownPacks:
            case ConfigRegistries:
            case ConfigFinish: {
                registries = registriesBuilder.update(packet, registries);
                break;
            }
            case Respawn: {
                PacketRespawn packetRespawn = PacketRespawn.read(packet, registries);
                dimension = packetRespawn.dimension;
                dimensionType = packetRespawn.dimensionType;
                break;
            }
            case JoinGame: {
                PacketJoinGame packetJoinGame = PacketJoinGame.read(packet, registries);
                registries = packetJoinGame.registries;
                dimension = packetJoinGame.dimension;
                dimensionType = packetJoinGame.dimensionType;
                break;
            }
        }

        return true;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) throws IOException {
    }

    @Override
    public String getName() {
        return "track_dimension";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
    }
}
