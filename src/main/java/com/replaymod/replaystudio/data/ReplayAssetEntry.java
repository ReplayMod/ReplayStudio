/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
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
