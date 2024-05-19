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

import java.util.Objects;

public class Registries {
    public CompoundTag registriesTag;

    public Registries() {
    }

    public Registries(CompoundTag tag) {
        this.registriesTag = tag;
    }

    public Registries copy() {
        Registries copy = new Registries();
        copy.registriesTag = this.registriesTag != null ? this.registriesTag.clone() : null;
        return copy;
    }

    public Entry getEntry(String registryName, String entryName) {
        if (registriesTag != null) {
            CompoundTag registry = registriesTag.get(registryName);
            if (registry == null) return null;
            ListTag entries = registry.get("value");
            if (entries == null) return null;
            for (Tag entry : entries) {
                StringTag name = ((CompoundTag) entry).get("name");
                if (name != null && name.getValue().equals(entryName)) {
                    NumberTag id = ((CompoundTag) entry).get("id");
                    Tag value = ((CompoundTag) entry).get("element");
                    return new Entry(id == null ? 0 : id.asInt(), name.getValue(), value);
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Registries that = (Registries) o;
        return Objects.equals(registriesTag, that.registriesTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registriesTag);
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
