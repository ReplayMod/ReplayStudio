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
package com.replaymod.replaystudio.protocol;

import com.github.steveice10.netty.buffer.Unpooled;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.packets.PacketTeam;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class PacketTeamTest {
    // Wire fixtures: name "team", CREATE, then the parameters in Minecraft's codec order.
    // Text components use unnamed NBT string tags: "Team", "[P]", and "[S]".
    private static final String TEAM = "04 7465616d 00";
    private static final String DISPLAY = "08 0004 5465616d";
    private static final String PREFIX = "08 0003 5b505d";
    private static final String SUFFIX = "08 0003 5b535d";
    private static final String PLAYERS = "02 05 416c696365 03 426f62";

    @Test
    public void team26_2WithColor() throws IOException {
        verify(ProtocolVersion.v26_2, TEAM + DISPLAY + PREFIX + SUFFIX + "00 00 01 0e 03");
    }

    @Test
    public void team26_2WithoutColor() throws IOException {
        verify(ProtocolVersion.v26_2, TEAM + DISPLAY + PREFIX + SUFFIX + "02 01 00 03");
    }

    @Test
    public void team26_1() throws IOException {
        verify(ProtocolVersion.v26_1, TEAM + DISPLAY + "03 00 00 0e" + PREFIX + SUFFIX);
    }

    private void verify(ProtocolVersion version, String header) throws IOException {
        PacketTypeRegistry registry = PacketTypeRegistry.get(version, State.PLAY);
        Packet template = packet(registry, header + PLAYERS);
        try {
            assertEquals("team", PacketTeam.getName(template));
            assertEquals(PacketTeam.Action.CREATE, PacketTeam.getAction(template));
            assertEquals(Arrays.asList("Alice", "Bob"), PacketTeam.getPlayers(template));
            Packet rewritten = PacketTeam.createTeam(template, Collections.singletonList("Carol"));
            Packet expected = packet(registry, header + "01 05 4361726f6c");
            try {
                assertEquals(Collections.singletonList("Carol"), PacketTeam.getPlayers(rewritten));
                assertEquals(expected.getBuf(), rewritten.getBuf());
                assertEquals(Arrays.asList("Alice", "Bob"), PacketTeam.getPlayers(template));
            } finally {
                rewritten.release();
                expected.release();
            }
            Packet empty = PacketTeam.createTeam(template, Collections.emptyList());
            try {
                assertEquals(Collections.emptyList(), PacketTeam.getPlayers(empty));
            } finally {
                empty.release();
            }
        } finally {
            template.release();
        }
    }

    private Packet packet(PacketTypeRegistry registry, String hex) {
        hex = hex.replace(" ", "");
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return new Packet(registry, PacketType.Team, Unpooled.wrappedBuffer(bytes));
    }
}
