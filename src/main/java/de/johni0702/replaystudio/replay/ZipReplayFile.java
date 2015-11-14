package de.johni0702.replaystudio.replay;

import com.google.common.base.Optional;
import com.google.common.io.Closeables;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.data.Marker;
import de.johni0702.replaystudio.data.ReplayAssetEntry;
import de.johni0702.replaystudio.io.ReplayInputStream;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.path.KeyframePosition;
import de.johni0702.replaystudio.path.KeyframeTime;
import de.johni0702.replaystudio.path.Path;
import de.johni0702.replaystudio.util.DPosition;
import de.johni0702.replaystudio.util.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.mc.protocol.data.game.Rotation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipReplayFile implements ReplayFile {

    private static final String ENTRY_META_DATA = "metaData.json";
    private static final String ENTRY_RECORDING = "recording.tmcpr";
    private static final String ENTRY_RESOURCE_PACK = "resourcepack/%s.zip";
    private static final String ENTRY_RESOURCE_PACK_INDEX = "resourcepack/index.json";
    private static final String ENTRY_THUMB = "thumb";
    private static final String ENTRY_PATHS_OLD = "paths";
    private static final String ENTRY_PATHS = "path.json";
    private static final String ENTRY_VISIBILITY_OLD = "visibility";
    private static final String ENTRY_VISIBILITY = "visibility.json";
    private static final String ENTRY_MARKERS = "markers.json";
    private static final String ENTRY_ASSET = "asset/%s_%s.%s";
    private static final Pattern PATTERN_ASSETS = Pattern.compile("asset/.*");

    private static final byte[] THUMB_MAGIC_NUMBERS = {0, 1, 1, 2, 3, 5, 8};

    private final Studio studio;
    private final File file;
    private final Map<String, OutputStream> outputStreams = new HashMap<>();
    private final Map<String, File> changedEntries = new HashMap<>();
    private final Set<String> removedEntries = new HashSet<>();

    private ZipFile zipFile;

    public ZipReplayFile(Studio studio, File file) throws IOException {
        this(studio, file, file);
    }

    public ZipReplayFile(Studio studio, File input, File output) throws IOException {
        this.studio = studio;
        this.file = output;
        if (input.exists()) {
            this.zipFile = new ZipFile(input);
        }
    }

    @Override
    public Optional<InputStream> get(String entry) throws IOException {
        if (changedEntries.containsKey(entry)) {
            return Optional.of(new BufferedInputStream(new FileInputStream(changedEntries.get(entry))));
        }
        if (zipFile == null || removedEntries.contains(entry)) {
            return Optional.absent();
        }
        ZipEntry zipEntry = zipFile.getEntry(entry);
        if (zipEntry == null) {
            return Optional.absent();
        }
        return Optional.of(new BufferedInputStream(zipFile.getInputStream(zipEntry)));
    }

    @Override
    public Map<String, InputStream> getAll(Pattern pattern) throws IOException {
        Map<String, InputStream> streams = new HashMap<>();

        for (Map.Entry<String, File> entry : changedEntries.entrySet()) {
            String name = entry.getKey();
            if (pattern.matcher(name).matches()) {
                streams.put(name, new BufferedInputStream(new FileInputStream(changedEntries.get(name))));
            }
        }

        if (zipFile != null) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (pattern.matcher(name).matches()) {
                    if (!streams.containsKey(name) && !removedEntries.contains(name)) {
                        streams.put(name, new BufferedInputStream(zipFile.getInputStream(entry)));
                    }
                }
            }
        }

        return streams;
    }

    @Override
    public OutputStream write(String entry) throws IOException {
        File file = changedEntries.get(entry);
        if (file == null) {
            file = Files.createTempFile("replaystudio", "replayfile").toFile();
            changedEntries.put(entry, file);
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        Closeables.closeQuietly(outputStreams.put(entry, out));
        removedEntries.remove(entry);
        return out;
    }

    @Override
    public void remove(String entry) throws IOException {
        File file = changedEntries.remove(entry);
        if (file != null && file.exists()) {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
        removedEntries.add(entry);
    }

    @Override
    public void save() throws IOException {
        if (zipFile != null && changedEntries.isEmpty() && removedEntries.isEmpty()) {
            return; // No changes, no need to save
        }
        File outputFile = Files.createTempFile("replaystudio", "replayfile").toFile();
        saveTo(outputFile);
        close();
        if (file.exists()) {
            Files.delete(file.toPath());
        }
        Files.move(outputFile.toPath(), file.toPath());
        zipFile = new ZipFile(file);
    }

    @Override
    public void saveTo(File target) throws IOException {
        for (OutputStream out : outputStreams.values()) {
            Closeables.closeQuietly(out);
        }
        outputStreams.clear();

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)))) {
            if (zipFile != null) {
                for (ZipEntry entry : Collections.list(zipFile.entries())) {
                    if (!changedEntries.containsKey(entry.getName()) && !removedEntries.contains(entry.getName())) {
                        out.putNextEntry(entry);
                        Utils.copy(zipFile.getInputStream(entry), out);
                    }
                }
            }
            for (Map.Entry<String, File> e : changedEntries.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                Utils.copy(new BufferedInputStream(new FileInputStream(e.getValue())), out);
            }
        }
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
    public void writeMetaData(ReplayMetaData metaData) throws IOException {
        metaData.setFileFormat("MCPR");
        metaData.setFileFormatVersion(1);
        if (metaData.getGenerator() == null) {
            metaData.setGenerator("ReplayStudio v" + studio.getVersion());
        }

        try (OutputStream out = write(ENTRY_META_DATA)) {
            String json = new Gson().toJson(metaData);
            out.write(json.getBytes());
        }
    }

    @Override
    public ReplayInputStream getPacketData() throws IOException {
        Optional<InputStream> in = get(ENTRY_RECORDING);
        if (!in.isPresent()) {
            return null;
        }
        return new ReplayInputStream(studio, in.get());
    }

    @Override
    public ReplayOutputStream writePacketData() throws IOException {
        return new ReplayOutputStream(studio, write(ENTRY_RECORDING));
    }

    @Override
    public Replay toReplay() throws IOException {
        return studio.createReplay(this);
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

    private Gson gsonPaths() {
        return new GsonBuilder().registerTypeAdapter(KeyframeTime.class, new TypeAdapter<KeyframeTime>() {
            @Override
            public void write(JsonWriter jsonWriter, KeyframeTime keyframe) throws IOException {
                jsonWriter.beginObject();
                jsonWriter.name("timestamp").value(keyframe.getReplayTime());
                jsonWriter.name("realTimestamp").value(keyframe.getTime());
            }

            @Override
            public KeyframeTime read(JsonReader jsonReader) throws IOException {
                long time = 0;
                long replayTime = 0;
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if ("timestamp".equals(name)) {
                        replayTime = jsonReader.nextLong();
                    } else if ("realTimestamp".equals(name)) {
                        time = jsonReader.nextLong();
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
                return new KeyframeTime(time, replayTime);
            }
        }).registerTypeAdapter(KeyframePosition.class, new TypeAdapter<KeyframePosition>() {
            @Override
            public void write(JsonWriter jsonWriter, KeyframePosition keyframe) throws IOException {
                jsonWriter.beginObject();
                jsonWriter.name("position");
                {
                    jsonWriter.beginObject();
                    jsonWriter.name("x").value(keyframe.getPosition().getX());
                    jsonWriter.name("y").value(keyframe.getPosition().getY());
                    jsonWriter.name("z").value(keyframe.getPosition().getZ());
                    jsonWriter.name("pitch").value(keyframe.getRotation().getPitch());
                    jsonWriter.name("yaw").value(keyframe.getRotation().getYaw());
                    jsonWriter.name("roll").value(keyframe.getRotation().getRoll());
                }
                jsonWriter.name("realTimestamp");
                jsonWriter.value(keyframe.getTime());
            }

            @Override
            public KeyframePosition read(JsonReader jsonReader) throws IOException {
                long time = 0;
                Pair<DPosition, Rotation> posRot = Pair.of(DPosition.NULL, new Rotation(0, 0, 0));
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if ("position".equals(name)) {
                        posRot = readPosRot(jsonReader);
                    } else if ("realTimestamp".equals(name)) {
                        time = jsonReader.nextLong();
                    } else {
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
                return new KeyframePosition(time, posRot.getLeft(), posRot.getRight());
            }

            private Pair<DPosition, Rotation> readPosRot(JsonReader jsonReader) throws IOException {
                double x = 0, y = 0, z = 0;
                float pitch = 0, yaw = 0, roll = 0;
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    switch (name) {
                        case "x": x = jsonReader.nextDouble(); break;
                        case "y": y = jsonReader.nextDouble(); break;
                        case "z": z = jsonReader.nextDouble(); break;
                        case "pitch": pitch = (float) jsonReader.nextDouble(); break;
                        case "yaw": yaw = (float) jsonReader.nextDouble(); break;
                        case "roll": roll = (float) jsonReader.nextDouble(); break;
                        default: jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();
                return Pair.of(new DPosition(x, y, z), new Rotation(pitch, yaw, roll));
            }
        }).create();
    }

    @Override
    public Optional<Path[]> getPaths() throws IOException {
        Optional<InputStream> in = get(ENTRY_PATHS);
        if (!in.isPresent()) {
            in = get(ENTRY_PATHS_OLD);
            if (!in.isPresent()) {
                return null;
            }
        }
        return Optional.of(gsonPaths().fromJson(new InputStreamReader(in.get()), Path[].class));
    }

    @Override
    public void writePaths(Path[] paths) throws IOException {
        try (OutputStream out = write(ENTRY_PATHS)) {
            String json = gsonPaths().toJson(paths);
            out.write(json.getBytes());
        }
    }

    @Override
    public Optional<BufferedImage> getThumb() throws IOException {
        Optional<InputStream> in = get(ENTRY_THUMB);
        if (in.isPresent()) {
            int i = 7;
            while (i > 0) {
                i -= in.get().skip(i);
            }
            return Optional.of(ImageIO.read(in.get()));
        }
        return Optional.absent();
    }

    @Override
    public void writeThumb(BufferedImage image) throws IOException {
        try (OutputStream out = write(ENTRY_THUMB)) {
            out.write(THUMB_MAGIC_NUMBERS);
            ImageIO.write(image, "jpg", out);
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
        for (InputStream in : entries.values()) {
            Closeables.closeQuietly(in);
        }
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
    public void close() throws IOException {
        if (zipFile != null) {
            zipFile.close();
        }
        for (OutputStream out : outputStreams.values()) {
            Closeables.closeQuietly(out);
        }
        outputStreams.clear();

        for (File file : changedEntries.values()) {
            Files.delete(file.toPath());
        }
        changedEntries.clear();
        removedEntries.clear();
    }
}
