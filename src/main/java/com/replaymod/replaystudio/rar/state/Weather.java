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

package com.replaymod.replaystudio.rar.state;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.packets.PacketChunkData;
import com.replaymod.replaystudio.protocol.packets.PacketNotifyClient;
import com.replaymod.replaystudio.rar.PacketSink;
import com.replaymod.replaystudio.rar.RandomAccessState;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;
import com.replaymod.replaystudio.rar.containers.PacketStateTree;

import java.io.IOException;

public class Weather extends TransientThing implements RandomAccessState {
    private final PacketStateTree rainStrengths;

    public Weather(PacketTypeRegistry registry, NetInput in) throws IOException {
        super(registry, in);
        this.rainStrengths = new PacketStateTree(registry, in.readVarInt());
    }

    @Override
    public void load(PacketSink sink, ReadableCache cache) throws IOException {
        super.load(sink, cache);
        rainStrengths.load(sink, cache);
    }

    @Override
    public void unload(PacketSink sink, ReadableCache cache) throws IOException {
        super.unload(sink, cache);
        rainStrengths.unload(sink, cache);
    }

    @Override
    public void play(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        rainStrengths.play(sink, currentTimeStamp, targetTime);
    }

    @Override
    public void rewind(PacketSink sink, int currentTimeStamp, int targetTime) throws IOException {
        rainStrengths.rewind(sink, currentTimeStamp, targetTime);
    }

    public static class Builder extends TransientThing.Builder {
        private final PacketStateTree.Builder rainStrengths = new PacketStateTree.Builder();

        public Builder(PacketTypeRegistry registry) throws IOException {
            addSpawnPacket(PacketNotifyClient.write(registry, PacketNotifyClient.Action.START_RAIN, 0));
            addDespawnPacket(PacketNotifyClient.write(registry, PacketNotifyClient.Action.STOP_RAIN, 0));
        }

        public void updateRainStrength(int time, Packet packet) {
            rainStrengths.put(time, packet);
        }

        @Override
        public void build(NetOutput out, WriteableCache cache) throws IOException {
            super.build(out, cache);

            out.writeVarInt(rainStrengths.build(cache));
        }
    }
}
