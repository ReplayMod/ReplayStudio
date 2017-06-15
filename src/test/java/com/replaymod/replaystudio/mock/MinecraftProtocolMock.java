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
package com.replaymod.replaystudio.mock;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.SubProtocol;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import com.google.common.base.Function;
import com.replaymod.replaystudio.util.Reflection;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class MinecraftProtocolMock extends MinecraftProtocol {

    private Function<Class<? extends Packet>, Class<? extends Packet>> incoming;
    private Function<Class<? extends Packet>, Class<? extends Packet>> outgoing;

    public MinecraftProtocolMock(Session session, boolean client,
                                 Function<Class<? extends Packet>, Class<? extends Packet>> incoming,
                                 Function<Class<? extends Packet>, Class<? extends Packet>> outgoing) {
        super(SubProtocol.LOGIN);

        this.incoming = incoming;
        this.outgoing = outgoing;

        init(session, client, SubProtocol.GAME);
    }

    public void init(Session session, boolean client, SubProtocol mode) {
        Reflection.setField(PacketProtocol.class, "incoming", this, new HashMap() {
            @Override
            @SuppressWarnings("unchecked")
            public Object put(Object key, Object value) {
                if (value instanceof Constructor) {
                    Constructor constructor = (Constructor) value;
                    Class<? extends Packet> cls = constructor.getDeclaringClass();
                    cls = incoming.apply(cls);
                    if (cls != null) {
                        try {
                            constructor = cls.getDeclaredConstructor();
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                        constructor.setAccessible(true);
                    }
                    value = constructor;
                }
                return super.put(key, value);
            }
        });

        Reflection.setField(PacketProtocol.class, "outgoing", this, new HashMap() {
            @Override
            @SuppressWarnings("unchecked")
            public Object put(Object key, Object value) {
                if (key instanceof Class) {
                    Class<? extends Packet> cls = (Class) key;
                    cls = outgoing.apply(cls);
                    if (cls != null) {
                        key = cls;
                    }
                }
                return super.put(key, value);
            }
        });

        setSubProtocol(mode, client, session);
    }

    @Override
    public void setSubProtocol(SubProtocol mode, boolean client, Session session) {
        super.setSubProtocol(mode, client, session);
    }
}
