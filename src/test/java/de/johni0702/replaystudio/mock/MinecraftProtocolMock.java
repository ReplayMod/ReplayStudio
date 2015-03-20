package de.johni0702.replaystudio.mock;

import com.google.common.base.Function;
import de.johni0702.replaystudio.util.Reflection;
import lombok.SneakyThrows;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.ProtocolMode;
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
        super(ProtocolMode.LOGIN);

        this.incoming = incoming;
        this.outgoing = outgoing;

        init(session, client, ProtocolMode.GAME);
    }

    public void init(Session session, boolean client, ProtocolMode mode) {
        Reflection.setField(PacketProtocol.class, "incoming", this, new HashMap() {
            @Override
            @SuppressWarnings("unchecked")
            @SneakyThrows
            public Object put(Object key, Object value) {
                if (value instanceof Constructor) {
                    Constructor constructor = (Constructor) value;
                    Class<? extends Packet> cls = constructor.getDeclaringClass();
                    cls = incoming.apply(cls);
                    if (cls != null) {
                        constructor = cls.getDeclaredConstructor();
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

        setMode(mode, client, session);
    }

    @Override
    public void setMode(ProtocolMode mode, boolean client, Session session) {
        super.setMode(mode, client, session);
    }
}
