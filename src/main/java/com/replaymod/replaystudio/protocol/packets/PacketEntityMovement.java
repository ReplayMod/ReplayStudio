/*
 * This file is part of ReplayStudio, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 johni0702 <https://github.com/johni0702>
 * Copyright (c) ReplayStudio contributors (see git)
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
package com.replaymod.replaystudio.protocol.packets;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Pair;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.Triple;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.util.DPosition;

import java.io.IOException;

public class PacketEntityMovement {
    public static Triple<DPosition, Pair<Float, Float>, Boolean> getMovement(Packet packet) throws IOException {
        PacketType type = packet.getType();
        boolean hasPos = type == PacketType.EntityPosition || type == PacketType.EntityPositionRotation;
        boolean hasRot = type == PacketType.EntityRotation || type == PacketType.EntityPositionRotation;
        try (Packet.Reader in = packet.reader()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                in.readVarInt(); // entity id
            } else {
                in.readInt(); // entity id
            }
            DPosition pos = null;
            if (hasPos) {
                if (packet.atLeast(ProtocolVersion.v1_9)) {
                    pos = new DPosition(
                            in.readShort() / 4096.0,
                            in.readShort() / 4096.0,
                            in.readShort() / 4096.0
                    );
                } else {
                    pos = new DPosition(
                            in.readByte() / 32.0,
                            in.readByte() / 32.0,
                            in.readByte() / 32.0
                    );
                }
            }
            Pair<Float, Float> yawPitch = null;
            if (hasRot) {
                yawPitch = new Pair<>(
                        in.readByte() / 256f * 360,
                        in.readByte() / 256f * 360
                );
            }
            boolean onGround = true;
            if (packet.atLeast(ProtocolVersion.v1_8) && (hasPos || hasRot)) {
                onGround = in.readBoolean();
            }
            return new Triple<>(pos, yawPitch, onGround);
        }
    }

    public static Packet write(PacketTypeRegistry registry, int entityId, DPosition deltaPos, Pair<Float, Float> yawPitch, boolean onGround) throws IOException {
        boolean hasPos = deltaPos != null;
        boolean hasRot = yawPitch != null;
        PacketType type;
        if (hasPos) {
            if (hasRot) {
                type = PacketType.EntityPositionRotation;
            } else {
                type = PacketType.EntityPosition;
            }
        } else {
            if (hasRot) {
                type = PacketType.EntityRotation;
            } else {
                type = PacketType.EntityMovement;
            }
        }
        Packet packet = new Packet(registry, type);
        try (Packet.Writer out = packet.overwrite()) {
            if (packet.atLeast(ProtocolVersion.v1_8)) {
                out.writeVarInt(entityId);
            } else {
                out.writeInt(entityId);
            }
            if (hasPos) {
                if (packet.atLeast(ProtocolVersion.v1_9)) {
                    out.writeShort((int) (deltaPos.getX() * 4096));
                    out.writeShort((int) (deltaPos.getY() * 4096));
                    out.writeShort((int) (deltaPos.getZ() * 4096));
                } else {
                    out.writeByte((int) (deltaPos.getX() * 32));
                    out.writeByte((int) (deltaPos.getY() * 32));
                    out.writeByte((int) (deltaPos.getZ() * 32));
                }
            }
            if (hasRot) {
                out.writeByte((int) (yawPitch.getKey() / 360 * 256));
                out.writeByte((int) (yawPitch.getValue() / 360 * 256));
            }
            if (packet.atLeast(ProtocolVersion.v1_8) && (hasPos || hasRot)) {
                out.writeBoolean(onGround);
            }
        }
        return packet;
    }
}
