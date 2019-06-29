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
package com.replaymod.replaystudio.studio.protocol;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.io.WrappedPacket;

//#if MC>=10800
import com.github.steveice10.mc.protocol.data.SubProtocol;
//#else
//$$ import com.github.steveice10.mc.protocol.ProtocolMode;
//#endif

import java.util.HashMap;
import java.util.Map;

public class StudioMinecraftProtocol extends MinecraftProtocol {

    private final Studio studio;

    /**
     * @deprecated Use {@link #StudioMinecraftProtocol(Studio, Session, boolean, boolean)} instead.
     */
    @Deprecated
    public StudioMinecraftProtocol(Studio studio, Session session, boolean client) {
        this(studio, session, client, false);
    }

    public StudioMinecraftProtocol(Studio studio, Session session, boolean client, boolean includeLoginPhase) {
        //#if MC>=10800
        super(SubProtocol.LOGIN);
        //#else
        //$$ super(ProtocolMode.LOGIN);
        //#endif

        this.studio = studio;

        //#if MC>=10800
        setSubProtocol(includeLoginPhase ? SubProtocol.LOGIN : SubProtocol.GAME, client, session);
        //#else
        //$$ setMode(includeLoginPhase ? ProtocolMode.LOGIN : ProtocolMode.GAME, client, session);
        //#endif
    }

    @Override
    protected Map<Integer, Class<? extends Packet>> createIncomingMap() {
        return new HashMap<Integer, Class<? extends Packet>>() {
            @Override
            public Class<? extends Packet> get(Object key) {
                return getPacketClass(studio, super.get(key));
            }
        };
    }

    @Override
    protected Map<Class<? extends Packet>, Integer> createOutgoingMap() {
        return new HashMap<Class<? extends Packet>, Integer>() {
            @Override
            public boolean containsKey(Object key) {
                return get(key) != null;
            }

            @Override
            public Integer get(Object key) {
                if (!(key instanceof Class)) {
                    return super.get(key);
                }
                Class<?> cls = (Class<?>) key;
                if (!Packet.class.isAssignableFrom(cls)) {
                    return super.get(key);
                }
                return super.get(WrappedPacket.getWrappedClassFor(cls.asSubclass(Packet.class)));
            }
        };
    }

    //#if MC>=10800
    @Override
    public void setSubProtocol(SubProtocol mode, boolean client, Session session) {
        super.setSubProtocol(mode, client, session);
    }
    //#else
    //$$ @Override
    //$$ public void setMode(ProtocolMode mode, boolean client, Session session) {
    //$$     super.setMode(mode, client, session);
    //$$ }
    //#endif

    private Class<? extends Packet> getPacketClass(Studio studio, Class<? extends Packet> cls) {
        if (studio.isWrappingEnabled() && !studio.willBeParsed(cls)) {
            return WrappedPacket.getClassFor(cls);
        } else {
            return cls;
        }
    }

}
