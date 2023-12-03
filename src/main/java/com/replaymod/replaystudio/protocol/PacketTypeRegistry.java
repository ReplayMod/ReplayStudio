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

import com.replaymod.replaystudio.lib.guava.collect.Lists;
import com.replaymod.replaystudio.lib.viaversion.api.Via;
import com.replaymod.replaystudio.lib.viaversion.api.connection.UserConnection;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.AbstractProtocol;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.ProtocolPathEntry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.ClientboundPacketType;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.PacketWrapper;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.Protocol;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.mapping.PacketMapping;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.mapping.PacketMappings;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.provider.PacketTypeMap;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_14to1_13_2.Protocol1_14To1_13_2;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_17_1to1_17.Protocol1_17_1To1_17;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_17to1_16_4.Protocol1_17To1_16_4;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_19to1_18_2.Protocol1_19To1_18_2;
import com.replaymod.replaystudio.lib.viaversion.protocols.protocol1_20_3to1_20_2.Protocol1_20_3To1_20_2;
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
                            // Packet no longer exists in this version.

                            // Special case: Minecraft replaces the DestroyEntities packet in 1.17 with a singe-entity
                            //               variant, only to revert that change in 1.17.1. So let's keep that around
                            //               if we are not stopping at 1.17.
                            if (protocol instanceof Protocol1_17To1_16_4 && packetType == PacketType.DestroyEntities && version != ProtocolVersion.v1_17) {
                                // ViaVersion maps the newly introduced DestroyEntity back to the good old
                                // DestroyEntities in 1.17.1, but not the other way around (cause it has to emit many
                                // packets for one), so if we just manually map to DestroyEntity here, it'll map back
                                // in 1.17.1 for us.
                                id = PacketType.DestroyEntity.getInitialId();
                                wasReplaced = false;
                                break;
                            }

                            continue packets;
                        }
                        id = newId;
                        wasReplaced = false;
                        break;
                    }
                    if (newId == id) {
                        wasReplaced = true;
                    }
                }

                // Special case: Multiple packets get merged into Spawn Object in 1.19, we want to drop those and
                //               preserve the original type.
                if (protocol instanceof Protocol1_19To1_18_2) {
                    switch (packetType) {
                        case SpawnPainting:
                        case SpawnMob:
                            continue packets;
                        case SpawnObject:
                            wasReplaced = false;
                            id = 0;
                            break;
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

                // Special case: ViaVersion remaps the DestroyEntity packet into a DestroyEntities. thought they're
                //               logically distinct packets for us.
                if (protocol instanceof Protocol1_17_1To1_17 && packetType == PacketType.DestroyEntity) {
                    wasReplaced = true;
                }

                // FIXME: ViaVersion doesn't yet use proper mapping for config-phase packets
                //        https://github.com/ViaVersion/ViaVersion/blob/45d08e9066802e5ae478b9efe97df73443005f77/common/src/main/java/com/viaversion/viaversion/protocols/protocol1_20_3to1_20_2/Protocol1_20_3To1_20_2.java#L327
                if (protocol instanceof Protocol1_20_3To1_20_2 && state == State.CONFIGURATION && id >= 7) {
                    id++;
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

    public boolean olderThan(ProtocolVersion protocolVersion) {
        return version.getVersion() < protocolVersion.getVersion();
    }

    public PacketTypeRegistry withState(State state) {
        return PacketTypeRegistry.get(version, state);
    }

    public PacketTypeRegistry withLoginSuccess() {
        return withState(atLeast(ProtocolVersion.v1_20_2) ? State.CONFIGURATION : State.PLAY);
    }

    private static List<Pair<Integer, Integer>> getIdMappings(Protocol<?, ?, ?, ?> protocol, State state) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        try {
            if (clientbound == null) {
                clientbound = AbstractProtocol.class.getDeclaredField("clientboundMappings");
                clientbound.setAccessible(true);
            }
            PacketMappings mappings = (PacketMappings) clientbound.get(protocol);

            PacketTypeMap<? extends ClientboundPacketType> packetTypeMap =
                    protocol.getPacketTypesProvider().unmappedClientboundPacketTypes().get(state);
            if (packetTypeMap == null) {
                return result;
            }

            PacketWrapper dummyPacketWrapper = PacketWrapper.create(null, (UserConnection) null);
            for (ClientboundPacketType unmappedPacketType : packetTypeMap.types()) {
                PacketMapping packetMapping = mappings.mappedPacket(state, unmappedPacketType.getId());
                if (packetMapping == null) {
                    continue;
                }

                dummyPacketWrapper.setPacketType(null);
                packetMapping.applyType(dummyPacketWrapper);

                int oldId = unmappedPacketType.getId();
                int newId = dummyPacketWrapper.getId();
                result.add(Pair.of(oldId, newId));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
