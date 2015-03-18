package de.johni0702.replaystudio.io;

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

/**
 * Wrapper class for packet which don't need parsing.
 * As ProtocolLib performs packet lookup by class this class shouldn't be used directly.
 * Instead the static methods should be used to generate the class for a specified packet automatically.
 */
public class WrappedPacket implements IWrappedPacket {

    /**
     * Class cache.
     */
    private static final Map<Class<? extends Packet>, Class<? extends IWrappedPacket>> classes = new HashMap<>();

    /**
     * Returns the wrapper class for the specified packet class.
     * This generates a new class if necessary using the same bytecode but a different class loader.
     * Note that this will fail if the binary file for this class is not accessible using
     * {@link ClassLoader#getResourceAsStream(String)}.
     * @param org The class whose wrapper to get
     * @return The wrapper class
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static synchronized Class<? extends IWrappedPacket> getClassFor(Class<? extends Packet> org) {
        Class<? extends IWrappedPacket> cls = classes.get(org);
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

    /**
     * Returns the class for which the specified packet is a wrapper for.
     * If the specified packet is not a wrapped packet then returns its own class.
     * @param wrapper The packet
     * @return The real class
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Packet> getWrapped(Packet wrapper) {
        return wrapper instanceof IWrappedPacket ? ((IWrappedPacket) wrapper).getWrapped() : wrapper.getClass();
    }

    /**
     * Checks whether the specified packet wrapper wraps packets of the specified type.
     * If the specified packet is not a wrapper then this acts just as an {@code instanceof} check.<br>
     * <br>
     * This can be used in order to e.g. filter packets by type without having to always have a special
     * case for wrapped packets.
     * @param packet The packet or packet wrapper
     * @param of The class
     * @return {@code true} if the packet is an instance (or a wrapper) of the specified class
     */
    public static boolean instanceOf(Packet packet, Class<? extends Packet> of) {
        return of.isAssignableFrom(getWrapped(packet));
    }

    /**
     * The class wrapped by this instance of the WrappedPacket class.
     * Set via reflection.
     */
    @SuppressWarnings("unused")
    private static Class<? extends Packet> wrapped;

    /**
     * Byte buffer used for reading and writing.
     */
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
    public byte[] getBytes() {
        return buf;
    }

    @Override
    public Class<? extends Packet> getWrapped() {
        return wrapped;
    }
}
