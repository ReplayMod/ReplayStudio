/*
 * Copyright (c) 2023
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

package com.replaymod.replaystudio.protocol.packets;

import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.replaymod.replaystudio.protocol.Packet;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a ReplayMod-specific custom packet which contains registry data which vanilla MC as of 1.20.5 no longer sends
 * across the wire because server and client have already agreed (via the "known packs" packets) that they both already
 * know it.
 * ReplayStudio doesn't though, so during recording ReplayMod will inject this custom packet containing that data
 * at the end of the configuration phase.
 */
public class PacketEnabledPacksData {
    public static final String ID = "replaymod:enabled_packs_data";

    public static Map<String, Map<String, Tag>> read(Packet packet) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            in.readString(); // custom payload id
            return in.readList(() -> {
                String registryName = in.readString();
                Map<String, Tag> registryEntries = in.readList(() -> {
                    String key = in.readString();
                    Tag value = in.readNBT();
                    return Pair.of(key, value);
                }).stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
                return Pair.of(registryName, registryEntries);
            }).stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
        }
    }
}
