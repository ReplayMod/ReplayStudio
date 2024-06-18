/*
 * Copyright (c) 2024
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

package com.replaymod.replaystudio.protocol.registry;

import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.data.VersionedIdentifier;
import com.replaymod.replaystudio.protocol.packets.PacketConfigRegistries;
import com.replaymod.replaystudio.protocol.packets.PacketConfigSelectKnownPacks;
import com.replaymod.replaystudio.protocol.packets.PacketCustomPayload;
import com.replaymod.replaystudio.protocol.packets.PacketEnabledPacksData;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RegistriesBuilder {
    private List<VersionedIdentifier> enabledPacks; // 1.20.5+
    private Map<String, Map<String, Tag>> enabledPacksData; // 1.20.5+
    private Registries registries;

    public void readRegistriesPacket(Packet packet) throws IOException {
        if (registries == null) registries = new Registries();
        PacketConfigRegistries.read(packet, registries);
    }

    public void readKnownPacksPacket(Packet packet) throws IOException {
        enabledPacks = PacketConfigSelectKnownPacks.read(packet);
    }

    public void readEnabledPacksDataPacket(Packet packet) throws IOException {
        enabledPacksData = PacketEnabledPacksData.read(packet);
    }

    public Registries finish(Registries oldRegistries) {
        Registries result;
        if (registries != null) {
            result = registries;
            result.enabledPacks = enabledPacks;
            if (enabledPacksData != null && registries.registriesMap != null) {
                mergeEnabledPacksIntoRegistry();
            }
        } else {
            result = oldRegistries;
        }

        enabledPacks = null;
        registries = null;

        return result;
    }

    private void mergeEnabledPacksIntoRegistry() {
        for (Map.Entry<String, Map<String, Tag>> sourceRegistry : enabledPacksData.entrySet()) {
            Map<String, Tag> sourceEntries = sourceRegistry.getValue();
            List<Pair<String, Tag>> targetEntries = registries.registriesMap.get(sourceRegistry.getKey());
            if (targetEntries == null) {
                continue;
            }
            for (int i = 0; i < targetEntries.size(); i++) {
                Pair<String, Tag> entry = targetEntries.get(i);
                if (entry.getValue() != null) {
                    continue;
                }
                String id = entry.getKey();
                Tag tag = sourceEntries.get(id);
                if (tag == null) {
                    continue;
                }
                targetEntries.set(i, Pair.of(id, tag));
            }
        }
    }

    public Registries update(Packet packet, Registries registries) throws IOException {
        switch (packet.getType()) {
            case ConfigSelectKnownPacks:
                readKnownPacksPacket(packet);
                return registries;
            case ConfigRegistries:
                readRegistriesPacket(packet);
                return registries;
            case ConfigFinish:
                return finish(registries);
            case ConfigCustomPayload:
                switch (PacketCustomPayload.getId(packet)) {
                    case PacketEnabledPacksData.ID:
                        readEnabledPacksDataPacket(packet);
                        return registries;
                }
            default:
                return registries;
        }
    }

    public void copyFrom(RegistriesBuilder other) {
        this.enabledPacks = other.enabledPacks != null ? other.enabledPacks : null;
        this.registries = other.registries != null ? other.registries.copy() : null;
        this.enabledPacksData = other.enabledPacksData != null ? other.enabledPacksData : null;
    }
}
