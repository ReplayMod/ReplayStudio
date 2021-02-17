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

import com.google.common.base.Optional;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.data.ModInfo;
import com.replaymod.replaystudio.data.ReplayAssetEntry;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public interface ReplayFile extends Closeable {

    /**
     * Returns an input stream for the specified entry in this replay file.
     * @param entry The entry
     * @return Optional input stream
     * @throws IOException If an I/O error occurs
     */
    Optional<InputStream> get(String entry) throws IOException;

    /**
     * Returns an input stream for the specified entry in the cache of this replay file.
     * @param entry The entry
     * @return Optional input stream
     * @throws IOException If an I/O error occurs
     */
    Optional<InputStream> getCache(String entry) throws IOException;

    /**
     * Returns input streams for each entry matching in this replay file.
     * @param pattern The pattern used for matching entries
     * @return Map with entry names as keys and their input streams as values
     * @throws IOException If an I/O error occurs
     */
    Map<String, InputStream> getAll(Pattern pattern) throws IOException;

    /**
     * Write to the specified entry in this replay file.
     * If an output stream for this entry already exists, it is closed.
     * The changes will not be written unless {@link #save()} is called.
     * @param entry The entry
     * @return An output stream to write to
     * @throws IOException If an I/O error occurs
     */
    OutputStream write(String entry) throws IOException;

    /**
     * Write to the specified cache entry of this replay file.
     * Since the cache in not part of the original replay file, there's no need to call {@link #save()} to persist it.
     * There's also no guarantee of persistence, the cache may be cleared (but shouldn't be most of the time) when the
     * cache entry is closed. The cache will always be cleared when the replay data is being re-written.
     * Writing to the cache while an OutputStream to the replay data is open results in undefined behavior.
     * @param entry The entry
     * @return An output stream to write to
     * @throws IOException If an I/O error occurs
     */
    OutputStream writeCache(String entry) throws IOException;

    /**
     * Removes the entry from this replay file.
     * Changes will not be written unless {@link #save()} is called.
     * @param entry The entry
     * @throws IOException
     */
    void remove(String entry) throws IOException;

    /**
     * Removes the cache entry of this replay file.
     * @param entry The entry
     * @throws IOException
     */
    void removeCache(String entry) throws IOException;

    /**
     * Saves the changes to this replay file.
     * If this operation fails, the original replay file will be unchanged,
     * @throws IOException If an I/O error occurs
     */
    void save() throws IOException;

    /**
     * Saves this replay file and all changes to the specified file.
     * Does not include any cache entries.
     * @param target The target file location
     * @throws IOException If an I/O error occurs
     */
    void saveTo(File target) throws IOException;

    ReplayMetaData getMetaData() throws IOException;
    void writeMetaData(PacketTypeRegistry registry, ReplayMetaData metaData) throws IOException;

    ReplayInputStream getPacketData(PacketTypeRegistry registry) throws IOException;

    ReplayOutputStream writePacketData() throws IOException;

    Map<Integer, String> getResourcePackIndex() throws IOException;
    void writeResourcePackIndex(Map<Integer, String> index) throws IOException;

    Optional<InputStream> getResourcePack(String hash) throws IOException;
    OutputStream writeResourcePack(String hash) throws IOException;

    Map<String, Timeline> getTimelines(PathingRegistry pathingRegistry) throws IOException;
    void writeTimelines(PathingRegistry pathingRegistry, Map<String, Timeline> timelines) throws IOException;

    default Optional<BufferedImage> getThumb() throws IOException {
        Optional<InputStream> maybeThumb = getThumbBytes();
        if (maybeThumb.isPresent()) {
            return Optional.of(ImageIO.read(maybeThumb.get()));
        }
        return Optional.absent();
    }
    default void writeThumb(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        writeThumbBytes(out.toByteArray());
    }

    Optional<InputStream> getThumbBytes() throws IOException;
    void writeThumbBytes(byte[] image) throws IOException;

    Optional<Set<UUID>> getInvisiblePlayers() throws IOException;
    void writeInvisiblePlayers(Set<UUID> uuids) throws IOException;

    Optional<Set<Marker>> getMarkers() throws IOException;
    void writeMarkers(Set<Marker> markers) throws IOException;

    Collection<ReplayAssetEntry> getAssets() throws IOException;
    Optional<InputStream> getAsset(UUID uuid) throws IOException;
    OutputStream writeAsset(ReplayAssetEntry asset) throws IOException;
    void removeAsset(UUID uuid) throws IOException;

    Collection<ModInfo> getModInfo() throws IOException;
    void writeModInfo(Collection<ModInfo> modInfo) throws IOException;
}
