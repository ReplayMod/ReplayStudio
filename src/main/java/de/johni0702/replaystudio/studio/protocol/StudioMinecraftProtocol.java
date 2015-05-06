package de.johni0702.replaystudio.studio.protocol;

import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.io.WrappedPacket;
import de.johni0702.replaystudio.util.Reflection;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.ProtocolMode;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.packet.PacketProtocol;

import java.util.HashMap;

public class StudioMinecraftProtocol extends MinecraftProtocol {

    public StudioMinecraftProtocol() {
        super(ProtocolMode.LOGIN);
    }

    public StudioMinecraftProtocol(Studio studio, Session session, boolean client) {
        super(ProtocolMode.LOGIN);

        init(studio, session, client, ProtocolMode.GAME);
    }

    public void init(Studio studio, Session session, boolean client, ProtocolMode mode) {
        Reflection.setField(PacketProtocol.class, "incoming", this, new HashMap() {
            @Override
            @SuppressWarnings("unchecked")
            public Object get(Object key) {
                Class<? extends Packet> value = (Class<? extends Packet>) super.get(key);
                return getPacketClass(studio, value);
            }
        });

        Reflection.setField(PacketProtocol.class, "outgoing", this, new HashMap() {
            @Override
            public boolean containsKey(Object key) {
                return get(key) != null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object get(Object key) {
                if (!(key instanceof Class)) {
                    return super.get(key);
                }
                return super.get(WrappedPacket.getWrappedClassFor((Class) key));
            }
        });

        setMode(mode, client, session);
    }

    @Override
    public void setMode(ProtocolMode mode, boolean client, Session session) {
        super.setMode(mode, client, session);
    }

    private Class<?> getPacketClass(Studio studio, Class<? extends Packet> cls) {
        if (studio.isWrappingEnabled() && !studio.willBeParsed(cls)) {
            return WrappedPacket.getClassFor(cls);
        } else {
            return cls;
        }
    }

}
