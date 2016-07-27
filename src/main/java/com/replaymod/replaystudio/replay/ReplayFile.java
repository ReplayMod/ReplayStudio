package com.replaymod.replaystudio.replay;

import com.google.common.base.Optional;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.path.Path;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.data.ReplayAssetEntry;

import java.awt.image.BufferedImage;
import java.io.*;
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
     * Removes the entry from this replay file.
     * Changes will not be written unless {@link #save()} is called.
     * @param entry The entry
     * @throws IOException
     */
    void remove(String entry) throws IOException;

    /**
     * Saves the changes to this replay file.
     * If this operation fails, the original replay file will be unchanged,
     * @throws IOException If an I/O error occurs
     */
    void save() throws IOException;

    /**
     * Saves this replay file and all changes to the specified file.
     * @param target The target file location
     * @throws IOException If an I/O error occurs
     */
    void saveTo(File target) throws IOException;

    ReplayMetaData getMetaData() throws IOException;
    void writeMetaData(ReplayMetaData metaData) throws IOException;

    ReplayInputStream getPacketData() throws IOException;
    ReplayOutputStream writePacketData() throws IOException;

    Replay toReplay() throws IOException;

    Map<Integer, String> getResourcePackIndex() throws IOException;
    void writeResourcePackIndex(Map<Integer, String> index) throws IOException;

    Optional<InputStream> getResourcePack(String hash) throws IOException;
    OutputStream writeResourcePack(String hash) throws IOException;

    Optional<Path[]> getPaths() throws IOException;
    void writePaths(Path[] paths) throws IOException;

    Optional<BufferedImage> getThumb() throws IOException;
    void writeThumb(BufferedImage image) throws IOException;

    Optional<Set<UUID>> getInvisiblePlayers() throws IOException;
    void writeInvisiblePlayers(Set<UUID> uuids) throws IOException;

    Optional<Set<Marker>> getMarkers() throws IOException;
    void writeMarkers(Set<Marker> markers) throws IOException;

    Collection<ReplayAssetEntry> getAssets() throws IOException;
    Optional<InputStream> getAsset(UUID uuid) throws IOException;
    OutputStream writeAsset(ReplayAssetEntry asset) throws IOException;
    void removeAsset(UUID uuid) throws IOException;

}
