/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 johni0702 <https://github.com/johni0702>
 * Copyright (c) ReplayStudio contributors (see git)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.replaymod.replaystudio.protocol;

import com.google.common.collect.Lists;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.Protocol;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.us.myles.ViaVersion.packets.State;
import com.replaymod.replaystudio.us.myles.ViaVersion.protocols.protocol1_14to1_13_2.Protocol1_14To1_13_2;
import com.replaymod.replaystudio.us.myles.ViaVersion.protocols.protocol1_9to1_8.Protocol1_9To1_8;
import com.replaymod.replaystudio.viaversion.CustomViaManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacketTypeRegistry {
    private static Map<ProtocolVersion, EnumMap<State, PacketTypeRegistry>> forVersionAndState = new HashMap<>();
    private static Field outgoing;
    private static Field oldId;
    private static Field newId;

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

            List<Pair<Integer, Protocol>> protocolPath = getProtocolPath(version.getId(), packetType.getInitialVersion().getId());
            if (protocolPath == null) {
                continue; // no path from packet version to current version (current version is not supported)
            }

            int id = packetType.getInitialId();
            for (Pair<Integer, Protocol> pair : Lists.reverse(protocolPath)) {
                Protocol protocol = pair.getValue();
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

    private static List<Pair<Integer, Protocol>> getProtocolPath(int clientVersion, int serverVersion) {
        // ViaVersion doesn't officially support 1.7.6 but luckily there weren't any (client-bound) packet id changes
        if (serverVersion == ProtocolVersion.v1_7_6.getId()) {
            return getProtocolPath(clientVersion, ProtocolVersion.v1_8.getId());
        }
        if (clientVersion == ProtocolVersion.v1_7_6.getId()) {
            return getProtocolPath(ProtocolVersion.v1_8.getId(), serverVersion);
        }
        // The trivial case
        if (clientVersion == serverVersion) {
            return Collections.emptyList();
        }
        // otherwise delegate to ViaVersion
        return ProtocolRegistry.getProtocolPath(clientVersion, serverVersion);
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
        return version.getId() >= protocolVersion.getId();
    }

    public boolean atMost(ProtocolVersion protocolVersion) {
        return version.getId() <= protocolVersion.getId();
    }

    @SuppressWarnings("unchecked")
    private static List<Pair<Integer, Integer>> getIdMappings(Protocol protocol, State state) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        try {
            if (outgoing == null) {
                outgoing = Protocol.class.getDeclaredField("outgoing");
                outgoing.setAccessible(true);
            }
            for (Map.Entry<Pair<State, Integer>, Object> entry : ((Map<Pair<State, Integer>, Object>) outgoing.get(protocol)).entrySet()) {
                if (entry.getKey().getKey() != state) {
                    continue;
                }
                Object mapping = entry.getValue();
                if (oldId == null || newId == null) {
                    Class<?> mappingClass = mapping.getClass();
                    oldId = mappingClass.getDeclaredField("oldID");
                    newId = mappingClass.getDeclaredField("newID");
                    oldId.setAccessible(true);
                    newId.setAccessible(true);
                }
                result.add(new Pair<>((Integer) oldId.get(mapping), (Integer) newId.get(mapping)));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
