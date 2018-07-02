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

import org.apache.logging.log4j.Logger;

public class StreamReplayFile extends AbstractReplayFile {

    private static final int ENTRY_META_DATA           = 0;
    private static final int ENTRY_RECORDING           = 1;
    private static final int ENTRY_RESOURCE_PACK       = 2;
    private static final int ENTRY_RESOURCE_PACK_INDEX = 3;
    private static final int ENTRY_THUMB               = 4;
    private static final int ENTRY_VISIBILITY_OLD      = 5;
    private static final int ENTRY_VISIBILITY          = 6;
    private static final int ENTRY_MARKERS             = 7;
    private static final int ENTRY_ASSET               = 8;
    private static final int ENTRY_MODS                = 9;
    private static final int ENTRY_END_OF_FILE         = 10;

    private static final byte[] THUMB_MAGIC_NUMBERS = {0, 1, 1, 2, 3, 5, 8};

    private static final int FIREHOSE_BUFFER_LIMIT = 1000;

    private ByteBuffer streamBuffer;
    private final AmazonKinesisFirehose firehoseClient;
    private final String streamName;
    PutRecordRequest putRecordRequest;

    private final Map<String, OutputStream> outputStreams = new HashMap<>();
    private final Map<String, File> changedEntries = new HashMap<>();
    private final Set<String> removedEntries = new HashSet<>();

    private ZipFile zipFile;

    private int bytesWritten = 0;

    private final Logger logger;

    //TODO add a gzip compression step before streaming to firehose
    public StreamReplayFile(Studio studio, AmazonKinesisFirehose firehoseClient, String streamName, Logger logger) throws IOException {
        super(studio);

        this.logger = logger;

        this.firehoseClient = firehoseClient;
        this.streamName = streamName;

        //Allocate buffer for stream
        this.streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);

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
        logger.error("Tried to call getAll - cmd unsupported");
        throw(new IOException("getAll is not supported for replay type StreamReplayFile"));
    }

    @Override
    public OutputStream write(String entry) throws IOException {
        throw(new IOException("Write is supported for replay type StreamReplayFile"));
        // // Record packet in the buffer and flush if near capacity
        // if (bytes.length + streamBuffer.position() < streamBuffer.capacity())
        // {
        //     streamBuffer.put(bytes);
        // } else {
        //     // Put records on stream
        //     PutRecordRequest putRecordRequest = new PutRecordRequest();
        //     putRecordRequest.setDeliveryStreamName(this.firehoseStreamName);

        //     Record record = new Record().withData(streamBuffer);
        //     putRecordRequest.setRecord(record);

        //     // Put record into the DeliveryStream
        //     // TODO measure performace of put_record 
        //     firehoseClient.putRecord(putRecordRequest);

        //     // Clear the dependent data buffer
        //     streamBuffer.clear();

        //     logger.info("Wrote record to firehose stream");
        // }
    }

    @Override
    public void save() throws IOException {
        logger.info("Ignoring Save");
        logger.info("Wrote " + Integer.toString(bytesWritten) + " bytes in total");
        // Make sure that we have all the needed infromation
        //writeMetaData(getMetaData());
        //flushToStream();
    }

    @Override
    public void saveTo(File target) throws IOException {
       throw(new IOException("Save to file not supported for replay type StreamReplayFile"));
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing stream");
        // TODO Send MC server return firehose key command

        byte[] EOF = "This is the end.".getBytes();

        sendToStream(ENTRY_END_OF_FILE, 0, EOF.length, EOF);
        flushToStream();
    }


    /* 
    *  Write methods
    *  
    *  The following methods are adapted to write data to aws kinesis firehose
    *  streams. Streming data is structured as follows:
    *  
    *  int      :   Entry Type
    *  int      :   Timestamp (0 if not included)
    *  int      :   Size of data
    *  byte[]   :   Data
    *
    */
    synchronized private void sendToStream(int entry, int timestamp,  int length, byte[] data) throws IOException {
        // Wrap Data
        ByteBuffer buff = ByteBuffer.wrap(data);

        // Header overhead
        int overhead = Integer.BYTES; //Entry ID
        overhead    += Integer.BYTES; //Timestamp
        overhead    += Integer.BYTES; //Length

        bytesWritten += length + overhead;

        if (buff.position() + streamBuffer.position() + overhead < streamBuffer.capacity())
        {
            // TODO evaluate posibility of race condition in buffer write
            this.streamBuffer.putInt(entry);
            this.streamBuffer.putInt(timestamp);
            this.streamBuffer.putInt(length);
            this.streamBuffer.put(buff);
            return;
        } else if (length + overhead < streamBuffer.capacity()) {
            logger.info("Sending firehose record (" + Integer.toString(streamBuffer.position()) + ") bytes");
            // Put records on stream
            Record record = new Record().withData(ByteBuffer.wrap(streamBuffer.array()));
            PutRecordRequest recordRequest = new PutRecordRequest();
            recordRequest.setRecord(record);
            recordRequest.setDeliveryStreamName(streamName);

            

            // Put record into the DeliveryStream
            // TODO measure performace of put_record 
            firehoseClient.putRecord(recordRequest);

            // Clear the dependent data buffer
            streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);

            // Add the data that didn't fit
            streamBuffer.putInt(entry);
            streamBuffer.putInt(length);
            streamBuffer.putInt(timestamp);
            streamBuffer.put(buff);
            
        } else {
            logger.info("Sending firehose record (" + Integer.toString(streamBuffer.position()) + ") bytes");
            logger.info("Sending firehose record (" + Integer.toString(length) + ") bytes");

            // Send what was there
            Record record = new Record().withData(ByteBuffer.wrap(streamBuffer.array()));
            PutRecordRequest recordRequest = new PutRecordRequest();
            recordRequest.setRecord(record);
            recordRequest.setDeliveryStreamName(streamName);
            firehoseClient.putRecord(recordRequest);
            
            streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);

            streamBuffer.putInt(entry);
            streamBuffer.putInt(length);
            streamBuffer.putInt(timestamp);

            int bytesRead = 0;
            int bytesWritten = overhead;

            for(int i = 0; i < (length / streamBuffer.capacity()); i++){
                int numBytes = FIREHOSE_BUFFER_LIMIT - bytesWritten;
                System.arraycopy(buff, bytesRead, streamBuffer.array(), bytesWritten, numBytes);

                record = new Record().withData(ByteBuffer.wrap(streamBuffer.array()));
                recordRequest = new PutRecordRequest();
                recordRequest.setRecord(record);
                recordRequest.setDeliveryStreamName(streamName);
                firehoseClient.putRecord(recordRequest);
         
                bytesRead += numBytes;
                bytesWritten = 0;
                streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);
            }
        }
    }

    private void flushToStream(){
        if( streamBuffer.position() != 0){
            // Put records on stream
            Record record = new Record().withData(ByteBuffer.wrap(streamBuffer.array()));
            this.putRecordRequest.setRecord(record);

            // Put record into the DeliveryStream
            // TODO measure performace of put_record 
            firehoseClient.putRecord(putRecordRequest);

            // Clear the dependent data buffer
            streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);
        }
    }

    @Override
    public void writePackets(int timestamp, int length, byte[] data) throws IOException {
        sendToStream(ENTRY_RECORDING, timestamp, length, data);
    }

    @Override
    public void writeMetaData(ReplayMetaData metaData) throws IOException {
        metaData.setFileFormat("MCPR");
        metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
        if (metaData.getGenerator() == null) {
            metaData.setGenerator("ReplayStudio v" + studio.getVersion());
        }

        String json = new Gson().toJson(metaData);
        sendToStream(ENTRY_META_DATA, 0, json.length(), json.getBytes());
    }

    @Override
    public void writeResourcePackIndex(Map<Integer, String> index) throws IOException {
        String json = new Gson().toJson(index);
        sendToStream(ENTRY_RESOURCE_PACK_INDEX, 0, json.length(), json.getBytes());
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
        sendToStream(ENTRY_VISIBILITY, 0, json.length(), json.getBytes());
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
        sendToStream(ENTRY_MODS, 0, json.length(), json.getBytes());
        
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
        sendToStream(ENTRY_MARKERS, 0, json.length(), json.getBytes());
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
        return write(String.format(Integer.toString(ENTRY_RESOURCE_PACK), hash));
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

