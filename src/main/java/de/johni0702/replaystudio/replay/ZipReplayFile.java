package de.johni0702.replaystudio.replay;

import com.google.common.base.Optional;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.johni0702.replaystudio.Studio;
import de.johni0702.replaystudio.io.ReplayInputStream;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import de.johni0702.replaystudio.path.KeyframePosition;
import de.johni0702.replaystudio.path.KeyframeTime;
import de.johni0702.replaystudio.path.Path;
import de.johni0702.replaystudio.util.DPosition;
import de.johni0702.replaystudio.util.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.spacehq.mc.auth.util.IOUtils;
import org.spacehq.mc.protocol.data.game.Rotation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
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

    private static final byte[] THUMB_MAGIC_NUMBERS = {0, 1, 1, 2, 3, 5, 8};

    private final Studio studio;
    private final File file;
    private final Map<String, OutputStream> outputStreams = new HashMap<>();
    private final Map<String, File> changedEntries = new HashMap<>();

    private ZipFile zipFile;

    public ZipReplayFile(Studio studio, File file) throws IOException {
        this.studio = studio;
        this.file = file;
        this.zipFile = new ZipFile(file);
    }

    @Override
    public Optional<InputStream> get(String entry) throws IOException {
        if (changedEntries.containsKey(entry)) {
            return Optional.of(new FileInputStream(changedEntries.get(entry)));
        }
        ZipEntry zipEntry = zipFile.getEntry(entry);
        if (zipEntry == null) {
            return Optional.absent();
        }
        return Optional.of(zipFile.getInputStream(zipEntry));
    }

    @Override
    public OutputStream write(String entry) throws IOException {
        File file = changedEntries.get(entry);
        if (file == null) {
            file = Files.createTempFile("replaystudio", "replayfile").toFile();
            changedEntries.put(entry, file);
        }
        OutputStream out = new FileOutputStream(file);
        IOUtils.closeQuietly(outputStreams.put(entry, out));
        return out;
    }

    @Override
    public void save() throws IOException {
        File outputFile = Files.createTempFile("replaystudio", "replayfile").toFile();
        saveTo(outputFile);
        Files.move(outputFile.toPath(), file.toPath());
        close();
        zipFile = new ZipFile(file);
    }

    @Override
    public void saveTo(File target) throws IOException {
        for (OutputStream out : outputStreams.values()) {
            IOUtils.closeQuietly(out);
        }
        outputStreams.clear();

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(target))) {
            for (ZipEntry entry : Collections.list(zipFile.entries())) {
                if (!changedEntries.containsKey(entry.getName())) {
                    out.putNextEntry(entry);
                    Utils.copy(zipFile.getInputStream(entry), out);
                }
            }
            for (Map.Entry<String, File> e : changedEntries.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                Utils.copy(new FileInputStream(e.getValue()), out);
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
    public void close() throws IOException {
        zipFile.close();
        for (OutputStream out : outputStreams.values()) {
            IOUtils.closeQuietly(out);
        }
        outputStreams.clear();

        for (File file : changedEntries.values()) {
            Files.delete(file.toPath());
        }
        changedEntries.clear();
    }
}
