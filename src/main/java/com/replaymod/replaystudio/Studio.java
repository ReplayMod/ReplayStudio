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
package com.replaymod.replaystudio;

import com.replaymod.replaystudio.filter.StreamFilter;

public interface Studio {

    /**
     * Returns the name of this implementation.
     * @return The name
     */
    String getName();

    /**
     * Returns the numerical version of this implementation.
     * @return Version number
     */
    int getVersion();

    /**
     * Loads a new instance of the specified stream filter.
     * @param name Name of the stream filter
     * @return New instance of the stream filter
     */
    StreamFilter loadStreamFilter(String name);

    /**
     * Return whether the specified replay and protocol file version can be read (and if necessary be converted to the
     * current version) by this Studio implementation.
     * @param fileVersion The file version
     * @param protocolVersion The MC protocol version
     * @param currentVersion The desired MC protocol version
     * @return {@code true} if the specified version is supported, {@code false} otherwise
     */
    boolean isCompatible(int fileVersion, int protocolVersion, int currentVersion);

    /**
     * Returns the file format version of replay files written with this Studio implementation.
     * @return The current file format version
     */
    int getCurrentFileFormatVersion();

}
