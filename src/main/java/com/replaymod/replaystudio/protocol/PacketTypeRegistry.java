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
package com.replaymod.replaystudio.protocol;

import com.google.common.collect.Lists;
import com.replaymod.replaystudio.lib.viaversion.api.Via;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.AbstractProtocol;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolPathEntry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.Protocol;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_14to1_13_2.Protocol1_14To1_13_2;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_9to1_8.Protocol1_9To1_8;
import com.replaymod.replaystudio.viaversion.CustomViaManager;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacketTypeRegistry {
    private static Map<ProtocolVersion, EnumMap<State, PacketTypeRegistry>> forVersionAndState = new HashMap<>();
    private static Field clientbound;

    static {
        CustomViaManager.initialize();
        for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
            if (ProtocolVersion.getIndex(version) < ProtocolVersion.getIndex(ProtocolVersion.v1_7_1)) {
                continue;
            }
            EnumMap<State, PacketTypeRegistry> forState = new EnumMap<>(State.class);
            for (State state : State.values()) {
                forState.put(state, new PacketTypeRegistry(version, state));
            }
            forVersionAndState.put(version, forState);
        }
    }

    public static PacketTypeRegistry get(ProtocolVersion version, State state) {
        EnumMap<State, PacketTypeRegistry> forState = forVersionAndState.get(version);
        return forState != null ? forState.get(state) : new PacketTypeRegistry(version, state);
    }

    private final ProtocolVersion version;
    private final State state;
    private final PacketType unknown;
    private final Map<Integer, PacketType> typeForId = new HashMap<>();
    private final Map<PacketType, Integer> idForType = new HashMap<>();

    private PacketTypeRegistry(ProtocolVersion version, State state) {
        this.version = version;
        this.state = state;

        PacketType unknown = null;
        int versionIndex = ProtocolVersion.getIndex(version);
        packets: for (PacketType packetType : PacketType.values()) {
            if (packetType.getState() != state) {
                continue; // incorrect protocol state (e.g. LOGIN vs PLAY)
            }

            if (packetType.isUnknown()) {
                unknown = packetType;
                continue; // "unknown" type exists for all versions
            }

            if (ProtocolVersion.getIndex(packetType.getInitialVersion()) > versionIndex) {
                continue; // packet didn't yet exist in this version
            }

            List<ProtocolPathEntry> protocolPath = getProtocolPath(version.getVersion(), packetType.getInitialVersion().getVersion());
            if (protocolPath == null) {
                continue; // no path from packet version to current version (current version is not supported)
            }

            int id = packetType.getInitialId();
            for (ProtocolPathEntry entry : Lists.reverse(protocolPath)) {
                Protocol<?, ?, ?, ?> protocol = entry.getProtocol();
                boolean wasReplaced = false;
                for (Pair<Integer, Integer> idMapping : getIdMappings(protocol, state)) {
                    int oldId = idMapping.getKey();
                    int newId = idMapping.getValue();
                    if (oldId == id) {
                        if (newId == -1) {
                            continue packets; // packet no longer exists in this version
                        }
                        id = newId;
                        wasReplaced = false;
                        break;
                    }
                    if (newId == id) {
                        wasReplaced = true;
                    }
                }

                // Special case: ViaVersion remaps the Spawn Global Entity packet into a Spawn Entity, though they're
                //               logically distinct packets for us.
                if (protocol instanceof Protocol1_16To1_15_2 && packetType == PacketType.SpawnGlobalEntity) {
                    wasReplaced = true;
                }

                // Special case: ViaVersion remaps the Use Bed packet into a Entity Metadata, though they're logically
                //               distinct packets for us.
                if (protocol instanceof Protocol1_14To1_13_2 && packetType == PacketType.PlayerUseBed) {
                    wasReplaced = true;
                }

                // Special case: ViaVersion cancels the Update Entity NBT packets unconditionally, instead of setting
                //               their newId to -1.
                if (protocol instanceof Protocol1_9To1_8 && packetType == PacketType.EntityNBTUpdate) {
                    wasReplaced = true;
                }

                if (wasReplaced) {
                    continue packets; // packet no longer exists in this version
                }
            }

            typeForId.put(id, packetType);
            idForType.put(packetType, id);
        }
        this.unknown = unknown;
    }

    private static List<ProtocolPathEntry> getProtocolPath(int clientVersion, int serverVersion) {
        // ViaVersion doesn't officially support 1.7.6 but luckily there weren't any (client-bound) packet id changes
        if (serverVersion == ProtocolVersion.v1_7_6.getVersion()) {
            return getProtocolPath(clientVersion, ProtocolVersion.v1_8.getVersion());
        }
        if (clientVersion == ProtocolVersion.v1_7_6.getVersion()) {
            return getProtocolPath(ProtocolVersion.v1_8.getVersion(), serverVersion);
        }
        // The trivial case
        if (clientVersion == serverVersion) {
            return Collections.emptyList();
        }
        // otherwise delegate to ViaVersion
        return Via.getManager().getProtocolManager().getProtocolPath(clientVersion, serverVersion);
    }

    public ProtocolVersion getVersion() {
        return version;
    }

    public State getState() {
        return state;
    }

    public Integer getId(PacketType type) {
        return idForType.get(type);
    }

    public PacketType getType(int id) {
        return typeForId.getOrDefault(id, unknown);
    }

    public boolean atLeast(ProtocolVersion protocolVersion) {
        return version.getVersion() >= protocolVersion.getVersion();
    }

    public boolean atMost(ProtocolVersion protocolVersion) {
        return version.getVersion() <= protocolVersion.getVersion();
    }

    @SuppressWarnings("unchecked")
    private static List<Pair<Integer, Integer>> getIdMappings(Protocol<?, ?, ?, ?> protocol, State state) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        try {
            if (clientbound == null) {
                clientbound = AbstractProtocol.class.getDeclaredField("clientbound");
                clientbound.setAccessible(true);
            }
            for (Map.Entry<AbstractProtocol.Packet, AbstractProtocol.ProtocolPacket> entry : ((Map<AbstractProtocol.Packet, AbstractProtocol.ProtocolPacket>) clientbound.get(protocol)).entrySet()) {
                if (entry.getKey().getState() != state) {
                    continue;
                }
                AbstractProtocol.ProtocolPacket mapping = entry.getValue();
                result.add(Pair.of(mapping.getOldID(), mapping.getNewID()));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
