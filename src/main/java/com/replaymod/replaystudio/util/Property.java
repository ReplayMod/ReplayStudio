/*
 * Copyright (c) 2022
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

package com.replaymod.replaystudio.util;

import com.replaymod.replaystudio.protocol.Packet;

import java.io.IOException;

public class Property {
    private final String name;
    private final String value;
    private final String signature;

    public Property(String name, String value, String signature) {
        this.name = name;
        this.value = value;
        this.signature = signature;
    }

    public static Property read(Packet.Reader in) throws IOException {
        String name = in.readString();
        String value = in.readString();
        String signature = null;
        if (in.readBoolean()) {
            signature = in.readString();
        }
        return new Property(name, value, signature);
    }

    public void write(Packet.Writer out) throws IOException {
        out.writeString(name);
        out.writeString(value);
        if (signature != null) {
            out.writeBoolean(true);
            out.writeString(signature);
        } else {
            out.writeBoolean(false);
        }
    }
}
