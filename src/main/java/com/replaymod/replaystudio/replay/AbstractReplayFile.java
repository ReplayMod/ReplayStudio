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
import com.google.common.io.Closeables;
import com.google.gson.*;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.data.ModInfo;
import com.replaymod.replaystudio.data.ReplayAssetEntry;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.serialize.TimelineSerialization;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public abstract class AbstractReplayFile implements ReplayFile {

    private static final String ENTRY_META_DATA = "metaData.json";
    protected static final String ENTRY_RECORDING = "recording.tmcpr";
    private static final String ENTRY_RESOURCE_PACK = "resourcepack/%s.zip";
    private static final String ENTRY_RESOURCE_PACK_INDEX = "resourcepack/index.json";
    private static final String ENTRY_THUMB_OLD = "thumb";
    private static final String ENTRY_THUMB = "thumb.jpg";
    private static final String ENTRY_VISIBILITY_OLD = "visibility";
    private static final String ENTRY_VISIBILITY = "visibility.json";
    private static final String ENTRY_MARKERS = "markers.json";
    private static final String ENTRY_ASSET = "asset/%s_%s.%s";
    private static final Pattern PATTERN_ASSETS = Pattern.compile("asset/.*");
    private static final String ENTRY_MODS = "mods.json";

    private static final byte[] THUMB_MAGIC_NUMBERS = {0, 1, 1, 2, 3, 5, 8};

    protected final Studio studio;

    public AbstractReplayFile(Studio studio) throws IOException {
        this.studio = studio;
    }

    @Override
    public ReplayMetaData getMetaData() throws IOException {
        Optional<InputStream> in = get(ENTRY_META_DATA);
        if (!in.isPresent()) {
            return null;
        }
        try (Reader is = new InputStreamReader(in.get())) {
            return new Gson().fromJson(is, ReplayMetaData.class);
        }
    }

    @Override
    public void writeMetaData(PacketTypeRegistry registry, ReplayMetaData metaData) throws IOException {
        metaData.setFileFormat("MCPR");
        if (registry != null) {
            metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
            metaData.setProtocolVersion(registry.getVersion().getOriginalVersion());
        }
        if (metaData.getGenerator() == null) {
            metaData.setGenerator("ReplayStudio v" + studio.getVersion());
        }

        try (OutputStream out = write(ENTRY_META_DATA)) {
            String json = new Gson().toJson(metaData);
            out.write(json.getBytes());
        }
    }

    @Override
    public ReplayInputStream getPacketData(PacketTypeRegistry registry) throws IOException {
        Optional<InputStream> in = get(ENTRY_RECORDING);
        if (!in.isPresent()) {
            return null;
        }
        ReplayMetaData metaData = getMetaData();
        return new ReplayInputStream(registry, in.get(), metaData.getFileFormatVersion(), metaData.getRawProtocolVersionOr0());
    }

    @Override
    public ReplayOutputStream writePacketData() throws IOException {
        return new ReplayOutputStream(write(ENTRY_RECORDING));
    }

    @Override
    public Map<Integer, String> getResourcePackIndex() throws IOException {
        Optional<InputStream> in = get(ENTRY_RESOURCE_PACK_INDEX);
        if (!in.isPresent()) {
            return null;
        }
        Map<Integer, String> index = new HashMap<>();
        try (Reader is = new InputStreamReader(in.get())) {
            JsonObject array = new Gson().fromJson(is, JsonObject.class);
            for (Map.Entry<String, JsonElement> e : array.entrySet()) {
                try {
                    index.put(Integer.parseInt(e.getKey()), e.getValue().getAsString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return index;
    }

    @Override
    public void writeResourcePackIndex(Map<Integer, String> index) throws IOException {
        try (OutputStream out = write(ENTRY_RESOURCE_PACK_INDEX)) {
            String json = new Gson().toJson(index);
            out.write(json.getBytes());
        }
    }

    @Override
    public Optional<InputStream> getResourcePack(String hash) throws IOException {
        return get(String.format(ENTRY_RESOURCE_PACK, hash));
    }

    @Override
    public OutputStream writeResourcePack(String hash) throws IOException {
        return write(String.format(ENTRY_RESOURCE_PACK, hash));
    }

    @Override
    public Map<String, Timeline> getTimelines(PathingRegistry pathingRegistry) throws IOException {
        return new TimelineSerialization(pathingRegistry, this).load();
    }

    @Override
    public void writeTimelines(PathingRegistry pathingRegistry, Map<String, Timeline> timelines) throws IOException {
        new TimelineSerialization(pathingRegistry, this).save(timelines);
    }

    @Override
    public Optional<InputStream> getThumbBytes() throws IOException {
        Optional<InputStream> maybeThumb = get(ENTRY_THUMB);
        if (maybeThumb.isPresent()) {
            return Optional.of(maybeThumb.get());
        }

        maybeThumb = get(ENTRY_THUMB_OLD);
        if (maybeThumb.isPresent()) {
            PushbackInputStream in = new PushbackInputStream(maybeThumb.get(), THUMB_MAGIC_NUMBERS.length);
            byte[] buf = new byte[THUMB_MAGIC_NUMBERS.length];
            new DataInputStream(in).readFully(buf);
            if (!Arrays.equals(buf, THUMB_MAGIC_NUMBERS)) {
                in.unread(buf);
            }
            return Optional.of(in);
        }

        return Optional.absent();
    }

    @Override
    public void writeThumbBytes(byte[] image) throws IOException {
        try (OutputStream out = write(ENTRY_THUMB)) {
            out.write(image);
        }
    }

    @Override
    public Optional<Set<UUID>> getInvisiblePlayers() throws IOException {
        Optional<InputStream> in = get(ENTRY_VISIBILITY);
        if (!in.isPresent()) {
            in = get(ENTRY_VISIBILITY_OLD);
            if (!in.isPresent()) {
                return Optional.absent();
            }
        }
        Set<UUID> uuids = new HashSet<>();
        try (Reader is = new InputStreamReader(in.get())) {
            JsonObject json = new Gson().fromJson(is, JsonObject.class);
            for (JsonElement e : json.getAsJsonArray("hidden")) {
                uuids.add(UUID.fromString(e.getAsString()));
            }
        }
        return Optional.of(uuids);
    }

    @Override
    public void writeInvisiblePlayers(Set<UUID> uuids) throws IOException {
        try (OutputStream out = write(ENTRY_VISIBILITY)) {
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            root.add("hidden", array);
            for (UUID uuid : uuids) {
                array.add(new JsonPrimitive(uuid.toString()));
            }
            String json = new Gson().toJson(root);
            out.write(json.getBytes());
        }
    }

    @Override
    public Optional<Set<Marker>> getMarkers() throws IOException {
        Optional<InputStream> in = get(ENTRY_MARKERS);
        if (in.isPresent()) {
            try (Reader is = new InputStreamReader(in.get())) {
                JsonArray json = new Gson().fromJson(is, JsonArray.class);
                Set<Marker> markers = new HashSet<>();
                for (JsonElement element : json) {
                    JsonObject obj = element.getAsJsonObject();
                    JsonObject value = obj.getAsJsonObject("value");
                    JsonObject position = value.getAsJsonObject("position");
                    Marker marker = new Marker();
                    marker.setTime(obj.get("realTimestamp").getAsInt());
                    marker.setX(position.get("x").getAsDouble());
                    marker.setY(position.get("y").getAsDouble());
                    marker.setZ(position.get("z").getAsDouble());
                    marker.setYaw(position.get("yaw").getAsFloat());
                    marker.setPitch(position.get("pitch").getAsFloat());
                    marker.setRoll(position.get("roll").getAsFloat());
                    if (value.has("name")) {
                        marker.setName(value.get("name").getAsString());
                    }
                    markers.add(marker);
                }
                return Optional.of(markers);
            }
        }
        return Optional.absent();
    }

    @Override
    public void writeMarkers(Set<Marker> markers) throws IOException {
        try (OutputStream out = write(ENTRY_MARKERS)) {
            JsonArray root = new JsonArray();
            for (Marker marker : markers) {
                JsonObject entry = new JsonObject();
                JsonObject value = new JsonObject();
                JsonObject position = new JsonObject();

                entry.add("realTimestamp", new JsonPrimitive(marker.getTime()));
                value.add("name", marker.getName() == null ? null : new JsonPrimitive(marker.getName()));
                position.add("x", new JsonPrimitive(marker.getX()));
                position.add("y", new JsonPrimitive(marker.getY()));
                position.add("z", new JsonPrimitive(marker.getZ()));
                position.add("yaw", new JsonPrimitive(marker.getYaw()));
                position.add("pitch", new JsonPrimitive(marker.getPitch()));
                position.add("roll", new JsonPrimitive(marker.getRoll()));

                value.add("position", position);
                entry.add("value", value);
                root.add(entry);
            }
            out.write(new Gson().toJson(root).getBytes());
        }
    }

    @Override
    public Collection<ReplayAssetEntry> getAssets() throws IOException {
        Map<String, InputStream> entries = getAll(PATTERN_ASSETS);
        entries.values().forEach(Closeables::closeQuietly);
        List<ReplayAssetEntry> list = new ArrayList<>();
        for (String key : entries.keySet()) {
            int delim = key.indexOf('_');
            UUID uuid = UUID.fromString(key.substring(0, delim));
            String name = key.substring(delim + 1, key.lastIndexOf('.'));
            String extension = key.substring(key.lastIndexOf('.'));
            list.add(new ReplayAssetEntry(uuid, extension, name));
        }
        return list;
    }

    @Override
    public Optional<InputStream> getAsset(UUID uuid) throws IOException {
        Map<String, InputStream> entries = getAll(Pattern.compile("asset/" + Pattern.quote(uuid.toString()) + "_.*"));
        if (entries.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(entries.values().iterator().next());
    }

    @Override
    public OutputStream writeAsset(ReplayAssetEntry asset) throws IOException {
        return write(String.format(ENTRY_ASSET, asset.getUuid().toString(), asset.getName(), asset.getFileExtension()));
    }

    @Override
    public void removeAsset(UUID uuid) throws IOException {
        Collection<ReplayAssetEntry> assets = getAssets();
        for (ReplayAssetEntry asset : assets) {
            if (asset.getUuid().equals(uuid)) {
                remove(String.format(ENTRY_ASSET, asset.getUuid().toString(), asset.getName(), asset.getFileExtension()));
            }
        }
    }

    @Override
    public Collection<ModInfo> getModInfo() throws IOException {
        Optional<InputStream> in = get(ENTRY_MODS);
        if (in.isPresent()) {
            try (Reader is = new InputStreamReader(in.get())) {
                JsonArray json = new Gson().fromJson(is, JsonObject.class).getAsJsonArray("requiredMods");
                List<ModInfo> modInfoList = new ArrayList<>();
                for (JsonElement element : json) {
                    JsonObject obj = element.getAsJsonObject();
                    modInfoList.add(new ModInfo(
                            obj.get("modID").getAsString(),
                            obj.get("modName").getAsString(),
                            obj.get("modVersion").getAsString()
                    ));
                }
                return modInfoList;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void writeModInfo(Collection<ModInfo> modInfo) throws IOException {
        try (OutputStream out = write(ENTRY_MODS)) {
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();
            for (ModInfo mod : modInfo) {
                JsonObject entry = new JsonObject();
                entry.addProperty("modID", mod.getId());
                entry.addProperty("modName", mod.getName());
                entry.addProperty("modVersion", mod.getVersion());
                array.add(entry);
            }
            root.add("requiredMods", array);
            out.write(new Gson().toJson(root).getBytes());
        }
    }
}
