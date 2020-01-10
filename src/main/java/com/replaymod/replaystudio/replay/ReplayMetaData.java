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
package com.replaymod.replaystudio.replay;

import java.util.Arrays;
import java.util.Objects;

/**
 * Meta data for replay files.
 */
public class ReplayMetaData {
    //#if MC>=11300
    public static final int CURRENT_FILE_FORMAT_VERSION = 14;
    //#else
    //#if MC>=11202
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 9;
    //#else
    //#if MC>=11201
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 7;
    //#else
    //#if MC>=11200
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 6;
    //#else
    //#if MC>=11102
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 5;
    //#else
    //#if MC>=11100
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 4;
    //#else
    //#if MC>=11002
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 3;
    //#else
    //#if MC>=10904
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 2;
    //#else
    //#if MC>=10800
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 1;
    //#else
    //$$ public static final int CURRENT_FILE_FORMAT_VERSION = 8;
    //#endif
    //#endif
    //#endif
    //#endif
    //#endif
    //#endif
    //#endif
    //#endif
    //#endif

    /**
     * Whether this is a singleplayer recording.
     */
    private boolean singleplayer;

    /**
     * The server address or the singleplayer world name.
     */
    private String serverName;

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
    private int protocol;

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

    public int getProtocolVersion() {
        return protocol;
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
