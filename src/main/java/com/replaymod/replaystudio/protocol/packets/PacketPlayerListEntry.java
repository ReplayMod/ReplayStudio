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

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.util.Property;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PacketPlayerListEntry {
    public enum Action {
        ADD,
        GAMEMODE, // 1.8+
        LATENCY, // 1.8+
        DISPLAY_NAME, // 1.8+
        REMOVE,
        ;
    }

    private UUID uuid; // any action (1.8+)
    private String name; // only in ADD (or 1.7.10 REMOVE)
    private List<Property> properties; // only in ADD (1.8+)
    private String displayName; // ADD or DISPLAY_NAME, nullable (1.8+)
    private int gamemode; // ADD or GAMEMODE (1.8+)
    private int latency; // ADD or latency

    public static PacketPlayerListEntry updateGamemode(PacketPlayerListEntry entry, int gamemode) {
        entry = new PacketPlayerListEntry(entry);
        entry.gamemode = gamemode;
        return entry;
    }

    public static PacketPlayerListEntry updateLatency(PacketPlayerListEntry entry, int latency) {
        entry = new PacketPlayerListEntry(entry);
        entry.latency = latency;
        return entry;
    }

    public static PacketPlayerListEntry updateDisplayName(PacketPlayerListEntry entry, String displayName) {
        entry = new PacketPlayerListEntry(entry);
        entry.displayName = displayName;
        return entry;
    }

    public static Action getAction(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                return Action.values()[in.readVarInt()];
            } else {
                in.readString(); // name
                if (in.readBoolean()) {
                    return Action.ADD;
                } else {
                    return Action.REMOVE;
                }
            }
        }
    }

    public static List<PacketPlayerListEntry> read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                Action action = Action.values()[in.readVarInt()];
                int count = in.readVarInt();
                List<PacketPlayerListEntry> result = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    PacketPlayerListEntry entry = new PacketPlayerListEntry();
                    entry.uuid = in.readUUID();
                    switch (action) {
                        case ADD:
                            entry.name = in.readString();
                            entry.properties = in.readList(() -> Property.read(in));
                            entry.gamemode = in.readVarInt();
                            entry.latency = in.readVarInt();
                            if (in.readBoolean()) {
                                entry.displayName = in.readString();
                            }
                            break;
                        case GAMEMODE:
                            entry.gamemode = in.readVarInt();
                            break;
                        case LATENCY:
                            entry.latency = in.readVarInt();
                            break;
                        case DISPLAY_NAME:
                            if (in.readBoolean()) {
                                entry.displayName = in.readString();
                            }
                            break;
                    }
                    result.add(entry);
                }
                return result;
            } else {
                PacketPlayerListEntry entry = new PacketPlayerListEntry();
                entry.name = in.readString();
                in.readBoolean(); // action
                entry.latency = in.readShort();
                return Collections.singletonList(entry);
            }
        }
    }

    public static Packet write(PacketTypeRegistry registry, Action action, PacketPlayerListEntry entry) throws IOException {
        return write(registry, action, Collections.singletonList(entry)).get(0);
    }

    public static List<Packet> write(PacketTypeRegistry registry, Action action, List<PacketPlayerListEntry> entries) throws IOException {
        if (registry.atLeast(ProtocolVersion.v1_8)) {
            return Collections.singletonList(write_1_8(registry, action, entries));
        } else {
            List<Packet> packets = new ArrayList<>(entries.size());
            for (PacketPlayerListEntry it : entries) {
                packets.add(write_1_7(registry, action, it));
            }
            return packets;
        }
    }

    private static Packet write_1_8(PacketTypeRegistry registry, Action action, List<PacketPlayerListEntry> entries) throws IOException {
        Packet packet = new Packet(registry, PacketType.PlayerListEntry);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeVarInt(action.ordinal());
            out.writeVarInt(entries.size());
            for (PacketPlayerListEntry entry : entries) {
                out.writeUUID(entry.uuid);
                switch (action) {
                    case ADD:
                        out.writeString(entry.name);
                        out.writeVarInt(entry.properties.size());
                        out.writeList(entry.properties, it -> it.write(out));
                        out.writeVarInt(entry.gamemode);
                        out.writeVarInt(entry.latency);
                        if (entry.displayName != null) {
                            out.writeBoolean(true);
                            out.writeString(entry.displayName);
                        } else {
                            out.writeBoolean(false);
                        }
                        break;
                    case GAMEMODE:
                        out.writeVarInt(entry.gamemode);
                        break;
                    case LATENCY:
                        out.writeVarInt(entry.latency);
                        break;
                    case DISPLAY_NAME:
                        if (entry.displayName != null) {
                            out.writeBoolean(true);
                            out.writeString(entry.displayName);
                        } else {
                            out.writeBoolean(false);
                        }
                        break;
                }
            }
        }
        return packet;
    }

    private static Packet write_1_7(PacketTypeRegistry registry, Action action, PacketPlayerListEntry entry) throws IOException {
        Packet packet = new Packet(registry, PacketType.PlayerListEntry);
        try (Packet.Writer out = packet.overwrite()) {
            out.writeString(entry.name);
            if (action == Action.ADD) {
                out.writeBoolean(true);
            } else if (action == Action.REMOVE) {
                out.writeBoolean(false);
            } else {
                throw new IllegalStateException("1.7 only supports ADD or REMOVE");
            }
            out.writeShort(entry.latency);
        }
        return packet;
    }

    private PacketPlayerListEntry() {}
    private PacketPlayerListEntry(PacketPlayerListEntry from) {
        this.uuid = from.uuid;
        this.name = from.name;
        this.properties = from.properties;
        this.displayName = from.displayName;
        this.gamemode = from.gamemode;
        this.latency = from.latency;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getGamemode() {
        return gamemode;
    }

    public int getLatency() {
        return latency;
    }

    /**
     * Returns the key identifying the player which this packet relates to.
     * In 1.8+ that is the UUID, in 1.7 it's the name.
     * @see PacketSpawnPlayer#getPlayerListEntryId(Packet)
     */
    public String getId() {
        return uuid != null ? uuid.toString() : name;
    }
}
