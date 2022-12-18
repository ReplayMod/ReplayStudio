/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.replay;

import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Meta data for replay files.
 */
public class ReplayMetaData {
    public static final int CURRENT_FILE_FORMAT_VERSION = 14;

    /**
     * Mapping from replay file version to protocol version for versions prior to 10.
     * For 10+ see https://github.com/ReplayMod/ReplayStudio/issues/9 (i.e. {@link #protocol}).
     */
    public static final Map<Integer, Integer> PROTOCOL_FOR_FILE_FORMAT = Collections.unmodifiableMap(new HashMap<Integer, Integer>() {{
        put(0, 47);
        put(1, 47);
        put(2, 110);
        put(3, 210);
        put(4, 315);
        put(5, 316);
        put(6, 335);
        put(7, 338);
        put(8, 5);
        put(9, 340);
    }});

    /**
     * Whether this is a singleplayer recording.
     */
    private boolean singleplayer;

    /**
     * The server address or the singleplayer world name.
     */
    private String serverName;

    /**
     * The server name (as configured by the user in the "Add Server" menu) or the singleplayer world name.
     * May be absent for older replays or when "Direct Connection" was used.
     */
    private String customServerName;

    /**
     * Duration of the replay in milliseconds.
     */
    private int duration;

    /**
     * Unix timestamp of when the recording was started in milliseconds.
     */
    private long date;

    /**
     * Minecraft version. (E.g. 1.8)
     */
    private String mcversion;

    /**
     * File format. Defaults to 'MCPR'
     */
    private String fileFormat;

    /**
     * Version of the file format.
     */
    private int fileFormatVersion;

    /**
     * Minecraft protocol version. Mandatory for `fileFormatVersion >= 13`.
     */
    private Integer protocol;

    /**
     * The program which generated the file.
     * Will always be written as "ReplayStudio vXY".
     */
    private String generator;

    /**
     * The entity id of the player manually added to this replay which represents the recording player.
     * Must be a valid entity id (e.g. must not be -1). May not be set.
     */
    private int selfId = -1;

    /**
     * Array of UUIDs of all players which can be seen in this replay.
     */
    private String[] players = new String[0];

    public ReplayMetaData() {
    }

    public ReplayMetaData(ReplayMetaData other) {
        singleplayer = other.singleplayer;
        serverName = other.serverName;
        customServerName = other.customServerName;
        duration = other.duration;
        date = other.date;
        mcversion = other.mcversion;
        fileFormat = other.fileFormat;
        fileFormatVersion = other.fileFormatVersion;
        generator = other.generator;
        selfId = other.selfId;
        players = Arrays.copyOf(other.players, other.players.length);
    }

    public boolean isSingleplayer() {
        return this.singleplayer;
    }

    public String getServerName() {
        return this.serverName;
    }

    public String getCustomServerName() {
        return customServerName;
    }

    public int getDuration() {
        return this.duration;
    }

    public long getDate() {
        return this.date;
    }

    public String getMcVersion() {
        return mcversion;
    }

    public String getFileFormat() {
        return this.fileFormat;
    }

    public int getFileFormatVersion() {
        return this.fileFormatVersion;
    }

    public Integer getRawProtocolVersion() {
        return protocol;
    }

    public int getRawProtocolVersionOr0() {
        return protocol != null ? protocol : 0;
    }

    public ProtocolVersion getProtocolVersion() {
        return getProtocolVersion(this.fileFormatVersion, this.getRawProtocolVersionOr0());
    }

    public static ProtocolVersion getProtocolVersion(int fileFormatVersion, int fileProtocol) {
        // See https://github.com/ReplayMod/ReplayStudio/issues/9#issuecomment-464451582
        // and https://github.com/ReplayMod/ReplayStudio/issues/9#issuecomment-464456558
        Integer protocol = fileProtocol != 0 ? fileProtocol : null;
        if (protocol == null) {
            protocol = PROTOCOL_FOR_FILE_FORMAT.get(fileFormatVersion);
            if (protocol == null) {
                throw new IllegalStateException("Replay files with version 10+ must provide the `protocol` key.");
            }
        }
        return ProtocolVersion.getProtocol(protocol);
    }

    public String getGenerator() {
        return this.generator;
    }

    public int getSelfId() {
        return this.selfId;
    }

    public String[] getPlayers() {
        return this.players;
    }

    public void setSingleplayer(boolean singleplayer) {
        this.singleplayer = singleplayer;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setCustomServerName(String customServerName) {
        this.customServerName = customServerName;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setMcVersion(String mcVersion) {
        this.mcversion = mcVersion;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public void setFileFormatVersion(int fileFormatVersion) {
        this.fileFormatVersion = fileFormatVersion;
    }

    public void setProtocolVersion(int protocol) {
        this.protocol = protocol;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public void setSelfId(int selfId) {
        this.selfId = selfId;
    }

    public void setPlayers(String[] players) {
        this.players = players;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ReplayMetaData)) return false;
        final ReplayMetaData other = (ReplayMetaData) o;
        if (!other.canEqual(this)) return false;
        if (this.singleplayer != other.singleplayer) return false;
        if (!Objects.equals(this.serverName, other.serverName)) return false;
        if (!Objects.equals(this.customServerName, other.customServerName)) return false;
        if (this.duration != other.duration) return false;
        if (this.date != other.date) return false;
        if (!Objects.equals(this.mcversion, other.mcversion)) return false;
        if (!Objects.equals(this.fileFormat, other.fileFormat)) return false;
        if (this.fileFormatVersion != other.fileFormatVersion) return false;
        if (this.protocol != other.protocol) return false;
        if (!Objects.equals(this.generator, other.generator)) return false;
        if (this.selfId != other.selfId) return false;
        return Arrays.deepEquals(this.players, other.players);
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + (this.singleplayer ? 79 : 97);
        result = result * 59 + (serverName == null ? 0 : serverName.hashCode());
        result = result * 59 + (customServerName == null ? 0 : customServerName.hashCode());
        result = result * 59 + this.duration;
        result = result * 59 + (int) (date >>> 32 ^ date);
        result = result * 59 + (mcversion == null ? 0 : mcversion.hashCode());
        result = result * 59 + (fileFormat == null ? 0 : fileFormat.hashCode());
        result = result * 59 + this.fileFormatVersion;
        result = result * 59 + this.protocol;
        result = result * 59 + (generator == null ? 0 : generator.hashCode());
        result = result * 59 + this.selfId;
        result = result * 59 + Arrays.deepHashCode(this.players);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ReplayMetaData;
    }

    @Override
    public String toString() {
        return "ReplayMetaData{" +
                "singleplayer=" + singleplayer +
                ", serverName='" + serverName + '\'' +
                ", customServerName='" + customServerName + '\'' +
                ", duration=" + duration +
                ", date=" + date +
                ", mcversion='" + mcversion + '\'' +
                ", fileFormat='" + fileFormat + '\'' +
                ", fileFormatVersion=" + fileFormatVersion +
                ", protocol=" + protocol +
                ", generator='" + generator + '\'' +
                ", selfId=" + selfId +
                ", players=" + Arrays.toString(players) +
                '}';
    }
}
