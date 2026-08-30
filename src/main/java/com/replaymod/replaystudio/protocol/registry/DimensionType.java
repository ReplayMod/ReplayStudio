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

package com.replaymod.replaystudio.protocol.registry;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.NumberTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;

import java.util.Objects;

public class DimensionType {
    private final CompoundTag tag;
    private final String name;
    private final int id;
    private final int minY;
    private final int height;

    // pre 1.16.2
    public DimensionType(String name) {
        this(new CompoundTag(), name, 0);
    }

    // 1.16.2+ pre 1.19
    public DimensionType(CompoundTag tag) {
        this(tag, "", 0);
    }

    // 1.19+ (and internally all versions)
    public DimensionType(CompoundTag tag, String name, int id) {
        this.tag = tag;
        this.name = name;
        this.id = id;

        Tag minY = tag.get("min_y");
        this.minY = minY instanceof NumberTag ? ((NumberTag) minY).asInt() : 0;
        Tag height = tag.get("height");
        this.height = height instanceof NumberTag ? ((NumberTag) height).asInt() : 256;
    }

    // pre 1.16.2 and 1.19+
    public String getName() {
        return this.name;
    }

    // 1.16.2+
    public CompoundTag getTag() {
        return this.tag;
    }

    // 1.20.5+
    public int getId() {
        return this.id;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMinSectionY() {
        return this.minY >> 4;
    }

    public int getHeight() {
        return this.height;
    }

    public int getSections() {
        return this.height >> 4;
    }

    public int sectionYToIndex(int sectionY) {
        return sectionY - this.getMinSectionY();
    }

    public int indexToSectionY(int index) {
        return index + this.getMinSectionY();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimensionType that = (DimensionType) o;
        return tag.equals(that.tag) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, name);
    }

    public static DimensionType fromRegistry(Registries registries, String name) {
        if (registries == null) {
            return new DimensionType(name);
        }
        Registries.Entry entry = registries.getEntry("dimension_type", name);
        if (entry == null) {
            return new DimensionType(name);
        }
        return DimensionType.fromRegistry(entry);
    }

    public static DimensionType fromRegistry(Registries registries, int id) {
        Registries.Entry entry = registries.getEntry("dimension_type", id);
        if (entry == null) {
            return new DimensionType(new CompoundTag(), "", id);
        }
        return DimensionType.fromRegistry(entry);
    }

    public static DimensionType fromRegistry(Registries.Entry entry) {
        return new DimensionType(entry.asCompoundOrEmpty(), entry.name, entry.id);
    }
}
