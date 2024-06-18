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

package com.replaymod.replaystudio.protocol.data;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;

import java.io.IOException;

public class VersionedIdentifier {
    public final String namespace;
    public final String id;
    public final String version;

    public VersionedIdentifier(String namespace, String id, String version) {
        this.namespace = namespace;
        this.id = id;
        this.version = version;
    }

    public void write(NetOutput out) throws IOException {
        out.writeString(namespace);
        out.writeString(id);
        out.writeString(version);
    }

    public static VersionedIdentifier read(NetInput in) throws IOException {
        return new VersionedIdentifier(in.readString(), in.readString(), in.readString());
    }
}
