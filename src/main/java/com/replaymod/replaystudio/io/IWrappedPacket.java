package com.replaymod.replaystudio.io;

import org.spacehq.packetlib.packet.Packet;

/**
 * Interface for wrapped packets.
 */
public interface IWrappedPacket extends Packet {

    /**
     * Returns the bytes which were read in.
     * @return byte array or {@code null} if {@link #read(org.spacehq.packetlib.io.NetInput)} hasn't been called yet
     */
    byte[] getBytes();

    /**
     * Returns the class which this wrapper is a replacement for.
     * @return The original class
     */
    Class<? extends Packet> getWrapped();

}
