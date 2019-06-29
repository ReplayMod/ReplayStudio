/*
 * This is a 1:1 copy of the class from PacketLib except with the option to supply a custom map for `incoming` and `outgoing`.
 * We used to do this via reflection but modifying final fields was finally made impossible with JDK12 (at least the way we did it).
 * https://github.com/Steveice10/PacketLib/blob/1d5a0ad81bfd99325a1db9991ced09ab773f55c8/src/main/java/com/github/steveice10/packetlib/packet/PacketProtocol.java
 *
 * Original copyright notice:
 *
 * Copyright (C) 2013-2018 Steveice10
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.steveice10.packetlib.packet;

import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.Server;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.crypt.PacketEncryption;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * A protocol for packet sending and receiving.
 * All implementations must have a no-params constructor for server protocol creation.
 */
public abstract class PacketProtocol {
    private final Map<Integer, Class<? extends Packet>> incoming = createIncomingMap();
    private final Map<Class<? extends Packet>, Integer> outgoing = createOutgoingMap();

    protected Map<Integer, Class<? extends Packet>> createIncomingMap() {
        return new HashMap<>();
    }

    protected Map<Class<? extends Packet>, Integer> createOutgoingMap() {
        return new HashMap<>();
    }

    /**
     * Gets the prefix used when locating SRV records for this protocol.
     *
     * @return The protocol's SRV record prefix.
     */
    public abstract String getSRVRecordPrefix();

    /**
     * Gets the packet header of this protocol.
     *
     * @return The protocol's packet header.
     */
    public abstract PacketHeader getPacketHeader();

    /**
     * Gets this protocol's active packet encryption.
     *
     * @return The protocol's packet encryption, or null if packets should not be encrypted.
     */
    public abstract PacketEncryption getEncryption();

    /**
     * Called when a client session is created with this protocol.
     *
     * @param client  The client that the session belongs to.
     * @param session The created session.
     */
    public abstract void newClientSession(Client client, Session session);

    /**
     * Called when a server session is created with this protocol.
     *
     * @param server  The server that the session belongs to.
     * @param session The created session.
     */
    public abstract void newServerSession(Server server, Session session);

    /**
     * Clears all currently registered packets.
     */
    public final void clearPackets() {
        this.incoming.clear();
        this.outgoing.clear();
    }

    /**
     * Registers a packet to this protocol as both incoming and outgoing.
     *
     * @param id     Id to register the packet to.
     * @param packet Packet to register.
     * @throws IllegalArgumentException If the packet fails a test creation when being registered as incoming.
     */
    public final void register(int id, Class<? extends Packet> packet) {
        this.registerIncoming(id, packet);
        this.registerOutgoing(id, packet);
    }

    /**
     * Registers an incoming packet to this protocol.
     *
     * @param id     Id to register the packet to.
     * @param packet Packet to register.
     * @throws IllegalArgumentException If the packet fails a test creation.
     */
    public final void registerIncoming(int id, Class<? extends Packet> packet) {
        this.incoming.put(id, packet);
        try {
            this.createIncomingPacket(id);
        } catch(IllegalStateException e) {
            this.incoming.remove(id);
            throw new IllegalArgumentException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Registers an outgoing packet to this protocol.
     *
     * @param id     Id to register the packet to.
     * @param packet Packet to register.
     */
    public final void registerOutgoing(int id, Class<? extends Packet> packet) {
        this.outgoing.put(packet, id);
    }

    /**
     * Creates a new instance of an incoming packet with the given id.
     *
     * @param id Id of the packet to create.
     * @return The created packet.
     * @throws IllegalArgumentException If the packet ID is invalid.
     * @throws IllegalStateException    If the packet does not have a no-params constructor or cannot be instantiated.
     */
    public final Packet createIncomingPacket(int id) {
        if(id < 0 || !this.incoming.containsKey(id) || this.incoming.get(id) == null) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }

        Class<? extends Packet> packet = this.incoming.get(id);
        try {
            Constructor<? extends Packet> constructor = packet.getDeclaredConstructor();
            if(!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }

            return constructor.newInstance();
        } catch(NoSuchMethodError e) {
            throw new IllegalStateException("Packet \"" + id + ", " + packet.getName() + "\" does not have a no-params constructor for instantiation.");
        } catch(Exception e) {
            throw new IllegalStateException("Failed to instantiate packet \"" + id + ", " + packet.getName() + "\".", e);
        }
    }

    /**
     * Gets the registered id of an outgoing packet class.
     *
     * @param packet Class of the packet to get the id for.
     * @return The packet's registered id.
     * @throws IllegalArgumentException If the packet is not registered.
     */
    public final int getOutgoingId(Class<? extends Packet> packet) {
        if(!this.outgoing.containsKey(packet) || this.outgoing.get(packet) == null) {
            throw new IllegalArgumentException("Unregistered outgoing packet class: " + packet.getName());
        }

        return this.outgoing.get(packet);
    }
}
