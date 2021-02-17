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
package com.replaymod.replaystudio.data;

import java.util.UUID;

public class ReplayAssetEntry {
    private final UUID uuid;
    private final String fileExtension;
    private String name;

    public ReplayAssetEntry(UUID uuid, String fileExtension) {
        this.uuid = uuid;
        this.fileExtension = fileExtension;
    }

    public ReplayAssetEntry(UUID uuid, String fileExtension, String name) {
        this.uuid = uuid;
        this.fileExtension = fileExtension;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReplayAssetEntry that = (ReplayAssetEntry) o;

        return uuid.equals(that.uuid);

    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "ReplayAssetEntry{" +
                "uuid=" + uuid +
                ", fileExtension='" + fileExtension + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
