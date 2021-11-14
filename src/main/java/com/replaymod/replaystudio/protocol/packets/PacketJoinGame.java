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
package com.replaymod.replaystudio.protocol.packets;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.registry.DimensionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketJoinGame {
    public int entityId;
    public boolean hardcore;
    public byte gameMode;
    public byte prevGameMode; // 1.16+
    public List<String> dimensions; // 1.16+
    public CompoundTag registry; // 1.16+
    public DimensionType dimensionType;
    public String dimension;
    public long seed; // 1.15+
    public int difficulty; // pre 1.14
    public int maxPlayers;
    public int viewDistance; // 1.14+
    public int simulationDistance; // 1.18+
    public boolean reducedDebugInfo; // 1.8+
    public boolean respawnScreen; // 1.15+
    public boolean debugWorld; // 1.16+
    public boolean flatWorld; // 1.16+

    public PacketJoinGame() {
    }

    public PacketJoinGame(PacketJoinGame other) {
        this.entityId = other.entityId;
        this.hardcore = other.hardcore;
        this.gameMode = other.gameMode;
        this.prevGameMode = other.prevGameMode;
        this.dimensions = other.dimensions;
        this.registry = other.registry;
        this.dimensionType = other.dimensionType;
        this.dimension = other.dimension;
        this.seed = other.seed;
        this.difficulty = other.difficulty;
        this.maxPlayers = other.maxPlayers;
        this.viewDistance = other.viewDistance;
        this.simulationDistance = other.simulationDistance;
        this.reducedDebugInfo = other.reducedDebugInfo;
        this.respawnScreen = other.respawnScreen;
        this.debugWorld = other.debugWorld;
        this.flatWorld = other.flatWorld;
    }

    public static PacketJoinGame read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            PacketJoinGame joinGame = new PacketJoinGame();
            joinGame.read(packet, in);
            return joinGame;
        }
    }

    public void read(Packet packet, Packet.Reader in) throws IOException {
        this.entityId = in.readInt();
        if (packet.atLeast(ProtocolVersion.v1_16_2)) {
            this.hardcore = in.readBoolean();
            this.gameMode = in.readByte();
        } else {
            int flags = in.readByte();
            this.hardcore = (flags & 0x8) != 0;
            this.gameMode = (byte) (flags & ~0x8);
        }
        if (packet.atLeast(ProtocolVersion.v1_16)) {
            this.prevGameMode = in.readByte();
            int count = in.readVarInt();
            this.dimensions = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                this.dimensions.add(in.readString());
            }
            this.registry = in.readNBT();
            if (packet.atLeast(ProtocolVersion.v1_16_2)) {
                this.dimensionType = new DimensionType(in.readNBT());
            } else {
                this.dimensionType = new DimensionType(in.readString());
            }
        }

        if (packet.atLeast(ProtocolVersion.v1_16)) {
            this.dimension = in.readString();
        } else if (packet.atLeast(ProtocolVersion.v1_9_1)) {
            this.dimension = String.valueOf(in.readInt());
        } else {
            this.dimension = String.valueOf(in.readByte());
        }

        if (packet.atLeast(ProtocolVersion.v1_15)) {
            this.seed = in.readLong();
        }
        if (packet.olderThan(ProtocolVersion.v1_14)) {
            this.difficulty = in.readByte();
        }
        if (packet.atLeast(ProtocolVersion.v1_16_2)) {
            this.maxPlayers = in.readVarInt();
        } else {
            this.maxPlayers = in.readByte();
        }
        if (packet.olderThan(ProtocolVersion.v1_16)) {
            this.dimensionType = new DimensionType(in.readString());
        }
        if (packet.atLeast(ProtocolVersion.v1_14)) {
            this.viewDistance = in.readVarInt();
        }
        if (packet.atLeast(ProtocolVersion.v1_18)) {
            this.simulationDistance = in.readVarInt();
        }
        if (packet.atLeast(ProtocolVersion.v1_8)) {
            this.reducedDebugInfo = in.readBoolean();
        }
        if (packet.atLeast(ProtocolVersion.v1_15)) {
            this.respawnScreen = in.readBoolean();
        }
        if (packet.atLeast(ProtocolVersion.v1_16)) {
            this.debugWorld = in.readBoolean();
            this.flatWorld = in.readBoolean();
        }
    }

    public Packet write(PacketTypeRegistry registry) throws IOException {
        Packet packet = new Packet(registry, PacketType.JoinGame);
        try (Packet.Writer out = packet.overwrite()) {
            write(packet, out);
        }
        return packet;
    }

    public void write(Packet packet, Packet.Writer out) throws IOException {
        out.writeInt(this.entityId);
        if (packet.atLeast(ProtocolVersion.v1_16_2)) {
            out.writeBoolean(this.hardcore);
            out.writeByte(this.gameMode);
        } else {
            out.writeByte((this.hardcore ? 0x8 : 0) | this.gameMode);
        }
        if (packet.atLeast(ProtocolVersion.v1_16)) {
            out.writeByte(this.prevGameMode);
            out.writeVarInt(this.dimensions.size());
            for (String dimension : this.dimensions) {
                out.writeString(dimension);
            }
            out.writeNBT(this.registry);
            if (packet.atLeast(ProtocolVersion.v1_16_2)) {
                out.writeNBT(this.dimensionType.getTag());
            } else {
                out.writeString(this.dimensionType.getName());
            }
        }

        if (packet.atLeast(ProtocolVersion.v1_16)) {
            out.writeString(this.dimension);
        } else if (packet.atLeast(ProtocolVersion.v1_9_1)) {
            out.writeInt(Integer.parseInt(this.dimension));
        } else {
            out.writeByte(Integer.parseInt(this.dimension));
        }

        if (packet.atLeast(ProtocolVersion.v1_15)) {
            out.writeLong(this.seed);
        }
        if (packet.olderThan(ProtocolVersion.v1_14)) {
            out.writeByte(this.difficulty);
        }
        if (packet.atLeast(ProtocolVersion.v1_16_2)) {
            out.writeVarInt(this.maxPlayers);
        } else {
            out.writeByte(this.maxPlayers);
        }
        if (packet.olderThan(ProtocolVersion.v1_16)) {
            out.writeString(this.dimensionType.getName());
        }
        if (packet.atLeast(ProtocolVersion.v1_14)) {
            out.writeVarInt(this.viewDistance);
        }
        if (packet.atLeast(ProtocolVersion.v1_18)) {
            out.writeVarInt(this.simulationDistance);
        }
        if (packet.atLeast(ProtocolVersion.v1_8)) {
            out.writeBoolean(this.reducedDebugInfo);
        }
        if (packet.atLeast(ProtocolVersion.v1_15)) {
            out.writeBoolean(this.respawnScreen);
        }
        if (packet.atLeast(ProtocolVersion.v1_16)) {
            out.writeBoolean(this.debugWorld);
            out.writeBoolean(this.flatWorld);
        }
    }
}
