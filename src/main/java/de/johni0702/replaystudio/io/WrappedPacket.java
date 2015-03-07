package de.johni0702.replaystudio.io;

import com.google.common.base.Supplier;
import lombok.SneakyThrows;
import org.spacehq.mc.auth.util.IOUtils;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class WrappedPacket implements Packet, Supplier<Class<? extends Packet>> {

    private static final Map<Class<? extends Packet>, Class<? extends Packet>> classes = new HashMap<>();

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static Class<? extends Packet> getClassFor(Class<? extends Packet> org) {
        Class<? extends Packet> cls = classes.get(org);
        if (cls == null) {
            cls = (Class) new ClassLoader() {
                @Override
                @SneakyThrows
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (name.equals(WrappedPacket.class.getName())) {
                        if (findLoadedClass(name) == null) {
                            String path = name.replace('.', '/').concat(".class");
                            InputStream in = WrappedPacket.class.getClassLoader().getResourceAsStream(path);
                            if (in == null) {
                                throw new ClassNotFoundException(name);
                            }
                            byte[] array = IOUtils.toByteArray(in);
                            defineClass(name, array, 0, array.length);
                        }
                    }
                    return super.loadClass(name, resolve);
                }
            }.loadClass(WrappedPacket.class.getName());
            Field field = cls.getDeclaredField("wrapped");
            field.setAccessible(true);
            field.set(null, org);
            classes.put(org, cls);
        }
        return cls;
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends Packet> getWrapped(Packet wrapper) {
        return wrapper instanceof Supplier ? ((Supplier<Class<? extends Packet>>) wrapper).get() : wrapper.getClass();
    }

    public static boolean instanceOf(Packet packet, Class<? extends Packet> of) {
        return of.isAssignableFrom(getWrapped(packet));
    }

    @SuppressWarnings("unused")
    private static Class<? extends Packet> wrapped;

    private byte[] buf;

    private WrappedPacket() {}

    @Override
    public void read(NetInput netInput) throws IOException {
        buf = new byte[netInput.available()];
        netInput.readBytes(buf);
    }

    @Override
    public void write(NetOutput netOutput) throws IOException {
        netOutput.writeBytes(buf);
    }

    @Override
    public boolean isPriority() {
        return false;
    }

    @Override
    public String toString() {
        return "Wrapper for " + wrapped.getName();
    }

    @Override
    public Class<? extends Packet> get() {
        return wrapped;
    }
}
