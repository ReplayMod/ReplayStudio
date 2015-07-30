package de.johni0702.replaystudio.mock;

import com.google.common.base.Function;
import de.johni0702.replaystudio.util.Reflection;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.SubProtocol;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.packet.PacketProtocol;

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
