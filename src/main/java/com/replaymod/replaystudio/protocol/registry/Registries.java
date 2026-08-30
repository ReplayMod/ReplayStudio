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

import com.github.steveice10.opennbt.tag.builtin.*;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.protocol.data.VersionedIdentifier;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

public class Registries {
    public CompoundTag registriesTag; // pre 1.20.5
    public Map<String, List<Pair<String, Tag>>> registriesMap; // 1.20.5+
    public List<VersionedIdentifier> enabledPacks; // 1.20.5+

    public Registries() {
    }

    public Registries(CompoundTag tag) {
        this.registriesTag = tag;
    }

    public Registries copy() {
        Registries copy = new Registries();
        copy.registriesTag = this.registriesTag != null ? this.registriesTag.clone() : null;
        copy.registriesMap = this.registriesMap != null ? new HashMap<>(this.registriesMap) : null;
        copy.enabledPacks = this.enabledPacks != null ? new ArrayList<>(this.enabledPacks) : null;
        return copy;
    }

    public Entry getEntry(String registryName, String entryName) {
        if (registriesTag != null) {
            CompoundTag registry = registriesTag.get(registryName);
            if (registry == null) {
                registry = registriesTag.get("minecraft:" + registryName);
            }
            if (registry == null) return null;
            ListTag entries = registry.get("value");
            if (entries == null) return null;
            for (Tag entry : entries) {
                StringTag name = ((CompoundTag) entry).get("name");
                if (name != null && (name.getValue().equals(entryName) || name.getValue().equals("minecraft:" + entryName))) {
                    NumberTag id = ((CompoundTag) entry).get("id");
                    Tag value = ((CompoundTag) entry).get("element");
                    return new Entry(id == null ? 0 : id.asInt(), name.getValue(), value);
                }
            }
            return null;
        } else if (registriesMap != null) {
            List<Pair<String, Tag>> registry = registriesMap.get(registryName);
            if (registry == null) return null;
            int id = 0;
            for (Pair<String, Tag> entry : registry) {
                if (entry.getKey().equals(entryName)) {
                    return new Entry(id, entry.getKey(), entry.getValue());
                }
                id++;
            }
            return null;
        } else {
            return null;
        }
    }

    public Entry getEntry(String registryName, int entryId) {
        if (registriesTag != null) {
            CompoundTag registry = registriesTag.get(registryName);
            if (registry == null) {
                registry = registriesTag.get("minecraft:" + registryName);
            }
            if (registry == null) return null;
            ListTag entries = registry.get("value");
            if (entries == null) return null;
            for (Tag entry : entries) {
                NumberTag id = ((CompoundTag) entry).get("id");
                if (id != null && id.asInt() == entryId) {
                    StringTag name = ((CompoundTag) entry).get("name");
                    Tag value = ((CompoundTag) entry).get("element");
                    return new Entry(id.asInt(), name != null ? name.getValue() : "", value);
                }
            }
            return null;
        } else if (registriesMap != null) {
            List<Pair<String, Tag>> registry = registriesMap.get(registryName);
            if (registry == null) return null;
            if (entryId < 0 || entryId >= registry.size()) return null;
            Pair<String, Tag> entry = registry.get(entryId);
            return new Entry(entryId, entry.getKey(), entry.getValue());
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Registries that = (Registries) o;
        return Objects.equals(registriesTag, that.registriesTag)
                && Objects.equals(registriesMap, that.registriesMap)
                && Objects.equals(enabledPacks, that.enabledPacks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registriesTag, registriesMap, enabledPacks);
    }

    public void writeInternal(PacketTypeRegistry registry, NetOutput out) throws IOException {
        if (registry.atLeast(ProtocolVersion.v1_20_5)) {
            Map<String, List<Pair<String, Tag>>> registriesMap = this.registriesMap;
            if (registriesMap == null) {
                registriesMap = Collections.emptyMap();
            }
            List<VersionedIdentifier> enabledPacks = this.enabledPacks;
            if (enabledPacks == null) {
                enabledPacks = Collections.emptyList();
            }
            Packet.Writer.writeList(registry, out, new ArrayList<>(registriesMap.entrySet()), registryEntry -> {
                out.writeString(registryEntry.getKey());
                Packet.Writer.writeList(registry, out, registryEntry.getValue(), entry -> {
                    out.writeString(entry.getKey());
                    Tag value = entry.getValue();
                    if (value != null) {
                        out.writeBoolean(true);
                        Packet.Writer.writeNBT(registry, out, value);
                    } else {
                        out.writeBoolean(false);
                    }
                });
            });
            Packet.Writer.writeList(registry, out, enabledPacks, entry -> entry.write(out));
        } else {
            Packet.Writer.writeNBT(registry, out, registriesTag);
        }
    }

    public static Registries readInternal(PacketTypeRegistry registry, NetInput in) throws IOException {
        if (registry.atLeast(ProtocolVersion.v1_20_5)) {
            Registries registries = new Registries();
            registries.registriesMap = new HashMap<>();
            Packet.Reader.readList(registry, in, () -> {
                String registryName = in.readString();
                List<Pair<String, Tag>> registryEntries = Packet.Reader.readList(registry, in, () -> {
                    String key = in.readString();
                    Tag value = in.readBoolean() ? Packet.Reader.readNBT(registry, in) : null;
                    return Pair.of(key, value);
                });
                registries.registriesMap.put(registryName, registryEntries);
                return null;
            });
            registries.enabledPacks = Packet.Reader.readList(registry, in, () -> VersionedIdentifier.read(in));
            return registries;
        } else {
            return new Registries(Packet.Reader.readNBT(registry, in));
        }
    }

    public static class Entry {
        public int id;
        public String name;
        public Tag value;

        public Entry(int id, String name, Tag value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }

        public CompoundTag asCompoundOrEmpty() {
            if (value instanceof CompoundTag) {
                return (CompoundTag) value;
            } else {
                return new CompoundTag();
            }
        }
    }
}
