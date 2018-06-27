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

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Closeables;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.util.Utils;

import org.apache.commons.lang3.ObjectUtils.Null;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Files.*;
import static java.nio.file.Files.*;
import static java.nio.file.Files.move;

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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesisfirehose.model.DeliveryStreamDescription;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;

public class StreamReplayFile extends AbstractReplayFile {

    private static final String ENTRY_META_DATA = "metaData.json";
    private static final String ENTRY_RECORDING = "recording.tmcpr";
    private static final String ENTRY_RESOURCE_PACK = "resourcepack/%s.zip";
    private static final String ENTRY_RESOURCE_PACK_INDEX = "resourcepack/index.json";
    private static final String ENTRY_THUMB = "thumb";
    private static final String ENTRY_VISIBILITY_OLD = "visibility";
    private static final String ENTRY_VISIBILITY = "visibility.json";
    private static final String ENTRY_MARKERS = "markers.json";
    private static final String ENTRY_ASSET = "asset/%s_%s.%s";
    private static final Pattern PATTERN_ASSETS = Pattern.compile("asset/.*");
    private static final String ENTRY_MODS = "mods.json";

    private static final byte[] THUMB_MAGIC_NUMBERS = {0, 1, 1, 2, 3, 5, 8};

    private static final int FIREHOSE_BUFFER_LIMIT = 1000;

    private final ByteBuffer streamBufffer;
    private final AmazonKinesisFirehose firehoseClient;
    private final String streamName;
    PutRecordRequest putRecordRequest;

    private final Map<String, OutputStream> outputStreams = new HashMap<>();
    private final Map<String, File> changedEntries = new HashMap<>();
    private final Set<String> removedEntries = new HashSet<>();

    private ZipFile zipFile;

    //TODO add a gzip compression step before streaming to firehose
    public StreamReplayFile(Studio studio, AmazonKinesisFirehose firehoseClient, String streamName) throws IOException {
        super(studio);

        this.firehoseClient = firehoseClient;
        this.streamName = streamName;

        //Allocate buffer for stream
        this.streamBufffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);

        // Create a default record request
        this.putRecordRequest = new PutRecordRequest();
        this.putRecordRequest.setDeliveryStreamName(streamName);

        //Check that our firehose stream is open and active
        DescribeDeliveryStreamRequest describeDeliveryStreamRequest = new DescribeDeliveryStreamRequest();
        describeDeliveryStreamRequest.withDeliveryStreamName(streamName);
        DescribeDeliveryStreamResult describeDeliveryStreamResponse =
        firehoseClient.describeDeliveryStream(describeDeliveryStreamRequest);
        DeliveryStreamDescription  deliveryStreamDescription = describeDeliveryStreamResponse.getDeliveryStreamDescription();
        String deliveryStreamStatus = deliveryStreamDescription.getDeliveryStreamStatus();
        if (!deliveryStreamStatus.equals("ACTIVE")) {
            throw(new IOException("Delivery stream not active!"));
        }   

        

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
        return null;
    }

    @Override
    public void save() throws IOException {
        // Make sure that we have all the needed infromation
        writeMetaData(getMetaData());
    }

    @Override
    public void saveTo(File target) throws IOException {
       throw(new IOException("Save to file not supported for replay type StreamReplayFile"));
    }

    @Override
    public void close() throws IOException {
        // TODO Send MC server return firehose key command

    }


    /* 
    *  Write methods
    *  
    *  The following methods are adapted to write data to aws kinesis firehose
    *  streams. Streming data is structured as follows:
    *  
    *  byte     :   Entry Type
    *  int      :   Size of data
    *  byte[]   :   Data
    *
    */

    private void sendToStream(String entry, byte[] data) {
        // TODO add buffering to imporve stream performance

        // Wrap Data
        ByteBuffer buff = ByteBuffer.wrap(data);

        // Create Record
        Record record = new Record().withData(buff);
        putRecordRequest.setRecord(record);

        // Put record into the DeliveryStream
        firehoseClient.putRecord(this.putRecordRequest);
    }

    @Override
    public void writeMetaData(ReplayMetaData metaData) throws IOException {
        metaData.setFileFormat("MCPR");
        metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
        if (metaData.getGenerator() == null) {
            metaData.setGenerator("ReplayStudio v" + studio.getVersion());
        }

        String json = new Gson().toJson(metaData);
        sendToStream(ENTRY_META_DATA, json.getBytes());
    }

    @Override
    public void writeResourcePackIndex(Map<Integer, String> index) throws IOException {
        String json = new Gson().toJson(index);
        sendToStream(ENTRY_RESOURCE_PACK_INDEX, json.getBytes());
    }

    @Override
    public void writeThumb(BufferedImage image) throws IOException {
        // Not supported
    }

    @Override
    public void writeInvisiblePlayers(Set<UUID> uuids) throws IOException {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        root.add("hidden", array);
        for (UUID uuid : uuids) {
            array.add(new JsonPrimitive(uuid.toString()));
        }
        String json = new Gson().toJson(root);
        sendToStream(ENTRY_VISIBILITY, json.getBytes());
    }

    @Override
    public void writeModInfo(Collection<ModInfo> modInfo) throws IOException {
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
        String json = new Gson().toJson(root);
        sendToStream(ENTRY_MODS, json.getBytes());
        
    }

    @Override
    public void writeMarkers(Set<Marker> markers) throws IOException {
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
        String json = new Gson().toJson(root);
        sendToStream(ENTRY_MARKERS, json.getBytes());
    }



    /* 
    *  Modified methods
    *  
    *  The following methods are not supporeted by the streaming replay file
    *  so they have been overidden to prevent confusion
    *
    */

    @Override
    public void remove(String entry) throws IOException {
        throw(new IOException());
    }

    @Override
    public ReplayInputStream getPacketData() throws IOException {
        return getPacketData(studio);
    }

    @Override
    public ReplayInputStream getPacketData(Studio studio) throws IOException {
        return null;
    }

    @Override
    public ReplayOutputStream writePacketData() throws IOException {
        return null;
    }

    @Override
    public Replay toReplay() throws IOException {
        return null;
    }

    @Override
    public Optional<InputStream> get(String entry) throws IOException {
        return Optional.absent();
    }

    @Override
    public OutputStream writeResourcePack(String hash) throws IOException {
        return write(String.format(ENTRY_RESOURCE_PACK, hash));
    }

    @Override
    public OutputStream writeAsset(ReplayAssetEntry asset) throws IOException {
        return null;
        //return write(String.format(ENTRY_ASSET, asset.getUuid().toString(), asset.getName(), asset.getFileExtension()));
    }

    @Override
    public void removeAsset(UUID uuid) throws IOException {
        // Function not supported by streaming replay files
        throw(new IOException());
    }

}

