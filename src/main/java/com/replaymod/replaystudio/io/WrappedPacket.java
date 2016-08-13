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
package com.replaymod.replaystudio.io;

import com.google.common.io.ByteStreams;
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
    @SuppressWarnings("unchecked")
    public static synchronized Class<? extends IWrappedPacket> getClassFor(Class<? extends Packet> org) {
        Class<? extends IWrappedPacket> cls = classes.get(org);
        if (cls == null) {
            ClassLoader loader = new WrappingClassLoader(org);
            try {
                cls = (Class) loader.loadClass(WrappedPacket.class.getName());
                Field field = cls.getDeclaredField("wrapped");
                field.setAccessible(true);
                field.set(null, org);
                classes.put(org, cls);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return cls;
    }

    /**
     * Returns the packet class for the specified wrapper class.
     * @param wrapper The wrapper whose packet class to get
     * @return The packet class or the specified class if it is no wrapper class
     */
    @SuppressWarnings("unchecked")
    public static synchronized Class<? extends Packet> getWrappedClassFor(Class<? extends IWrappedPacket> wrapper) {
        ClassLoader loader = wrapper.getClassLoader();
        return loader instanceof WrappingClassLoader ? ((WrappingClassLoader) loader).wrapped : wrapper;
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

    private static class WrappingClassLoader extends ClassLoader {
        private final Class<? extends Packet> wrapped;

        public WrappingClassLoader(Class<? extends Packet> wrapped) {
            super(WrappingClassLoader.class.getClassLoader());
            this.wrapped = wrapped;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(WrappedPacket.class.getName())) {
                if (findLoadedClass(name) == null) {
                    String path = name.replace('.', '/').concat(".class");
                    InputStream in = WrappedPacket.class.getClassLoader().getResourceAsStream(path);
                    if (in == null) {
                        throw new ClassNotFoundException(name);
                    }
                    byte[] array;
                    try {
                        array = ByteStreams.toByteArray(in);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    defineClass(name, array, 0, array.length);
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}
