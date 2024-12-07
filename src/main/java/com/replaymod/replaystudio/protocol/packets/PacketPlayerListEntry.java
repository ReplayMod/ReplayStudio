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
import com.replaymod.replaystudio.protocol.data.StringOrNbtText;
import com.replaymod.replaystudio.util.Property;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PacketPlayerListEntry {
    public enum Action {
        ADD,
        CHAT_KEY, // 1.19.3+
        GAMEMODE, // 1.8+
        LISTED, // 1.19.3+
        LATENCY, // 1.8+
        DISPLAY_NAME, // 1.8+
        LIST_ORDER, // 1.21.2+
        SHOW_HAT, // 1.21.4+
        REMOVE,
        ;

        private static final List<Action> VALUES_1_21_4 = Arrays.asList(ADD, CHAT_KEY, GAMEMODE, LISTED, LATENCY, DISPLAY_NAME, LIST_ORDER, SHOW_HAT);
        private static final List<Action> VALUES_1_21_2 = Arrays.asList(ADD, CHAT_KEY, GAMEMODE, LISTED, LATENCY, DISPLAY_NAME, LIST_ORDER);
        private static final List<Action> VALUES_1_19_3 = Arrays.asList(ADD, CHAT_KEY, GAMEMODE, LISTED, LATENCY, DISPLAY_NAME);
        private static final List<Action> VALUES_1_8 = Arrays.asList(ADD, GAMEMODE, LATENCY, DISPLAY_NAME, REMOVE);
        private static final List<Action> VALUES_1_7 = Arrays.asList(ADD, REMOVE);

        public static Set<Action> init(PacketTypeRegistry registry) {
            if (registry.atLeast(ProtocolVersion.v1_19_3)) {
                return EnumSet.copyOf(values(registry));
            } else {
                return EnumSet.of(ADD);
            }
        }

        public static List<Action> values(PacketTypeRegistry registry) {
            if (registry.atLeast(ProtocolVersion.v1_21_4)) {
                return VALUES_1_21_4;
            } else if (registry.atLeast(ProtocolVersion.v1_21_2)) {
                return VALUES_1_21_2;
            } else if (registry.atLeast(ProtocolVersion.v1_19_3)) {
                return VALUES_1_19_3;
            } else if (registry.atLeast(ProtocolVersion.v1_8)) {
                return VALUES_1_8;
            } else {
                return VALUES_1_7;
            }
        }

        // 1.19.3+
        public static EnumSet<Action> readSet(Packet.Reader in, List<Action> values) throws IOException {
            BitSet bitSet = BitSet.valueOf(in.readBytes((values.size() + 7) / 8));
            EnumSet<Action> set = EnumSet.noneOf(Action.class);
            for (int i = 0; i < values.size(); i++) {
                if (bitSet.get(i)) {
                    set.add(values.get(i));
                }
            }
            return set;
        }

        // 1.19.3+
        public static void writeSet(Packet.Writer out, Set<Action> actions, List<Action> values) throws IOException {
            BitSet bitSet = new BitSet();
            for (int i = 0; i < values.size(); i++) {
                if (actions.contains(values.get(i))) {
                    bitSet.set(i);
                }
            }
            out.writeBytes(Arrays.copyOf(bitSet.toByteArray(), (values.size() + 7) / 8));
        }
    }

    private UUID uuid; // any action (1.8+)
    private String name; // only in ADD (or 1.7.10 REMOVE)
    private List<Property> properties; // only in ADD (1.8+)
    private StringOrNbtText displayName; // ADD (pre 1.19.3) or DISPLAY_NAME, nullable (1.8+)
    private int gamemode; // ADD (pre 1.19.3) or GAMEMODE (1.8+)
    private boolean listed; // LISTED (1.19.3+)
    private int latency; // ADD (pre 1.19.3) or latency
    private int listOrder; // LIST_ORDER (1.21.2+)
    private boolean showHat; // SHOW_HAT (1.21.4+)
    private SigData sigData; // ADD (1.19+; pre 1.19.3) or CHAT_KEY (1.19.3+)

    public static PacketPlayerListEntry updateChatKey(PacketPlayerListEntry entry, SigData sigData) {
        entry = new PacketPlayerListEntry(entry);
        entry.sigData = sigData;
        return entry;
    }

    public static PacketPlayerListEntry updateGamemode(PacketPlayerListEntry entry, int gamemode) {
        entry = new PacketPlayerListEntry(entry);
        entry.gamemode = gamemode;
        return entry;
    }

    public static PacketPlayerListEntry updateListed(PacketPlayerListEntry entry, boolean listed) {
        entry = new PacketPlayerListEntry(entry);
        entry.listed = listed;
        return entry;
    }

    public static PacketPlayerListEntry updateLatency(PacketPlayerListEntry entry, int latency) {
        entry = new PacketPlayerListEntry(entry);
        entry.latency = latency;
        return entry;
    }

    public static PacketPlayerListEntry updateDisplayName(PacketPlayerListEntry entry, StringOrNbtText displayName) {
        entry = new PacketPlayerListEntry(entry);
        entry.displayName = displayName;
        return entry;
    }

    public static PacketPlayerListEntry updateListOrder(PacketPlayerListEntry entry, int listOrder) {
        entry = new PacketPlayerListEntry(entry);
        entry.listOrder = listOrder;
        return entry;
    }

    public static PacketPlayerListEntry updateShowHat(PacketPlayerListEntry entry, boolean showHat) {
        entry = new PacketPlayerListEntry(entry);
        entry.showHat = showHat;
        return entry;
    }

    public static Set<Action> getActions(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_19_3)) {
                if (packet.getType() == PacketType.PlayerListEntryRemove) {
                    return Collections.singleton(Action.REMOVE);
                }
                return Action.readSet(in, Action.values(packet.getRegistry()));
            } else if (packet.atLeast(ProtocolVersion.v1_8)) {
                return Collections.singleton(Action.VALUES_1_8.get(in.readVarInt()));
            } else {
                in.readString(); // name
                if (in.readBoolean()) {
                    return Collections.singleton(Action.ADD);
                } else {
                    return Collections.singleton(Action.REMOVE);
                }
            }
        }
    }

    public static List<PacketPlayerListEntry> read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                Set<Action> actions;
                if (packet.getType() == PacketType.PlayerListEntryRemove) {
                    actions = Collections.singleton(Action.REMOVE);
                } else if (packet.atLeast(ProtocolVersion.v1_19_3)) {
                    actions = Action.readSet(in, Action.values(packet.getRegistry()));
                } else {
                    actions = Collections.singleton(Action.VALUES_1_8.get(in.readVarInt()));
                }
                int count = in.readVarInt();
                List<PacketPlayerListEntry> result = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    PacketPlayerListEntry entry = new PacketPlayerListEntry();
                    entry.uuid = in.readUUID();
                    for (Action action : actions) {
                        switch (action) {
                            case ADD:
                                entry.name = in.readString();
                                entry.properties = in.readList(() -> Property.read(in));
                                if (packet.olderThan(ProtocolVersion.v1_19_3)) {
                                    entry.gamemode = in.readVarInt();
                                    entry.latency = in.readVarInt();
                                    if (in.readBoolean()) {
                                        entry.displayName = in.readText();
                                    }
                                    if (packet.atLeast(ProtocolVersion.v1_19)) {
                                        if (in.readBoolean()) {
                                            entry.sigData = SigData.read(packet, in);
                                        }
                                    }
                                }
                                break;
                            case CHAT_KEY:
                                if (in.readBoolean()) {
                                    entry.sigData = SigData.read(packet, in);
                                }
                                break;
                            case GAMEMODE:
                                entry.gamemode = in.readVarInt();
                                break;
                            case LISTED:
                                entry.listed = in.readBoolean();
                                break;
                            case LATENCY:
                                entry.latency = in.readVarInt();
                                break;
                            case DISPLAY_NAME:
                                if (in.readBoolean()) {
                                    entry.displayName = in.readText();
                                }
                                break;
                            case LIST_ORDER:
                                entry.listOrder = in.readVarInt();
                                break;
                            case SHOW_HAT:
                                entry.showHat = in.readBoolean();
                                break;
                        }
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

    public static Packet write(PacketTypeRegistry registry, Set<Action> actions, PacketPlayerListEntry entry) throws IOException {
        return write(registry, actions, Collections.singletonList(entry)).get(0);
    }

    public static List<Packet> write(PacketTypeRegistry registry, Set<Action> actions, List<PacketPlayerListEntry> entries) throws IOException {
        if (registry.atLeast(ProtocolVersion.v1_8)) {
            return Collections.singletonList(write_1_8(registry, actions, entries));
        } else {
            List<Packet> packets = new ArrayList<>(entries.size());
            for (PacketPlayerListEntry it : entries) {
                packets.add(write_1_7(registry, actions.iterator().next(), it));
            }
            return packets;
        }
    }

    private static Packet write_1_8(PacketTypeRegistry registry, Set<Action> actions, List<PacketPlayerListEntry> entries) throws IOException {
        if (registry.atLeast(ProtocolVersion.v1_19_3) && actions.contains(Action.REMOVE)) {
            Packet packet = new Packet(registry, PacketType.PlayerListEntry);
            try (Packet.Writer out = packet.overwrite()) {
                out.writeList(entries, entry -> out.writeUUID(entry.uuid));
            }
            return packet;
        }
        Packet packet = new Packet(registry, PacketType.PlayerListEntry);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(ProtocolVersion.v1_19_3)) {
                Action.writeSet(out, actions, Action.values(registry));
            } else {
                out.writeVarInt(Action.VALUES_1_8.indexOf(actions.iterator().next()));
            }
            out.writeVarInt(entries.size());
            for (PacketPlayerListEntry entry : entries) {
                out.writeUUID(entry.uuid);
                for (Action action : actions) {
                    switch (action) {
                        case ADD:
                            out.writeString(entry.name);
                            out.writeList(entry.properties, it -> it.write(out));
                            if (packet.olderThan(ProtocolVersion.v1_19_3)) {
                                out.writeVarInt(entry.gamemode);
                                out.writeVarInt(entry.latency);
                                if (entry.displayName != null) {
                                    out.writeBoolean(true);
                                    out.writeText(entry.displayName);
                                } else {
                                    out.writeBoolean(false);
                                }
                                if (packet.atLeast(ProtocolVersion.v1_19)) {
                                    if (entry.sigData != null) {
                                        out.writeBoolean(true);
                                        entry.sigData.write(packet, out);
                                    } else {
                                        out.writeBoolean(false);
                                    }
                                }
                            }
                            break;
                        case CHAT_KEY:
                            if (entry.sigData != null) {
                                out.writeBoolean(true);
                                entry.sigData.write(packet, out);
                            } else {
                                out.writeBoolean(false);
                            }
                            break;
                        case GAMEMODE:
                            out.writeVarInt(entry.gamemode);
                            break;
                        case LISTED:
                            out.writeBoolean(entry.listed);
                            break;
                        case LATENCY:
                            out.writeVarInt(entry.latency);
                            break;
                        case DISPLAY_NAME:
                            if (entry.displayName != null) {
                                out.writeBoolean(true);
                                out.writeText(entry.displayName);
                            } else {
                                out.writeBoolean(false);
                            }
                            break;
                        case LIST_ORDER:
                            out.writeVarInt(entry.listOrder);
                            break;
                        case SHOW_HAT:
                            out.writeBoolean(entry.showHat);
                            break;
                    }
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
        this.listed = from.listed;
        this.latency = from.latency;
        this.listOrder = from.listOrder;
        this.showHat = from.showHat;
        this.sigData = from.sigData;
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

    public StringOrNbtText getDisplayName() {
        return displayName;
    }

    public int getGamemode() {
        return gamemode;
    }

    public boolean isListed() {
        return listed;
    }

    public int getLatency() {
        return latency;
    }

    public int getListOrder() {
        return listOrder;
    }

    public boolean getShowHat() {
        return showHat;
    }

    public SigData getSigData() {
        return sigData;
    }

    /**
     * Returns the key identifying the player which this packet relates to.
     * In 1.8+ that is the UUID, in 1.7 it's the name.
     * @see PacketSpawnPlayer#getPlayerListEntryId(Packet)
     */
    public String getId() {
        return uuid != null ? uuid.toString() : name;
    }

    public static class SigData {
        private final UUID sessionUuid; // 1.19.3+
        private final long expireTimestamp;
        private final byte[] publicKey;
        private final byte[] signature;

        public SigData(UUID sessionUuid, long expireTimestamp, byte[] publicKey, byte[] signature) {
            this.sessionUuid = sessionUuid;
            this.expireTimestamp = expireTimestamp;
            this.publicKey = publicKey;
            this.signature = signature;
        }

        public static SigData read(Packet packet, Packet.Reader in) throws IOException {
            UUID sessionUuid;
            if (packet.atLeast(ProtocolVersion.v1_19_3)) {
                sessionUuid = in.readUUID();
            } else {
                sessionUuid = null;
            }
            long expireTimestamp = in.readLong();
            byte[] publicKey = in.readBytes(in.readVarInt());
            byte[] signature = in.readBytes(in.readVarInt());
            return new SigData(sessionUuid, expireTimestamp, publicKey, signature);
        }

        public void write(Packet packet, Packet.Writer out) throws IOException {
            if (packet.atLeast(ProtocolVersion.v1_19_3)) {
                out.writeUUID(sessionUuid);
            }
            out.writeLong(expireTimestamp);
            out.writeVarInt(publicKey.length);
            out.writeBytes(publicKey);
            out.writeVarInt(signature.length);
            out.writeBytes(signature);
        }
    }
}
