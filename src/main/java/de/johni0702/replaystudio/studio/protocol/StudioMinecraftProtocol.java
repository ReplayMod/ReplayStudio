package de.johni0702.replaystudio.studio.protocol;

import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.io.WrappedPacket;
import de.johni0702.replaystudio.util.Reflection;
import lombok.SneakyThrows;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.ProtocolMode;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.packet.PacketProtocol;

import java.lang.reflect.Constructor;
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
            @SneakyThrows
            public Object put(Object key, Object value) {
                Constructor constructor = (Constructor) value;
                constructor = getPacketClass(studio, constructor.getDeclaringClass()).getDeclaredConstructor();
                constructor.setAccessible(true);
                return super.put(key, constructor);
            }
        });

        Reflection.setField(PacketProtocol.class, "outgoing", this, new HashMap() {
            @Override
            @SuppressWarnings("unchecked")
            public Object put(Object key, Object value) {
                return super.put(getPacketClass(studio, (Class) key), value);
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
