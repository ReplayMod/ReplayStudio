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
import com.google.common.collect.Multiset.Entry;
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
import com.replaymod.replaystudio.io.StreamingOutputStream;
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
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesisfirehose.model.Record;

import org.apache.logging.log4j.Logger;

public class StreamReplayFile extends AbstractReplayFile {
    protected static final String ENTRY_END_OF_STREAM = "eof";

    protected int indexOf(String entry){
             if (entry.equals(ENTRY_META_DATA))             {return 1;}
        else if (entry.equals(ENTRY_RECORDING))             {return 2;}
        else if (entry.equals(ENTRY_RESOURCE_PACK))         {return 3;}
        else if (entry.equals(ENTRY_RESOURCE_PACK_INDEX))   {return 4;}
        else if (entry.equals(ENTRY_THUMB))                 {return 5;}
        else if (entry.equals(ENTRY_VISIBILITY_OLD))        {return 6;}
        else if (entry.equals(ENTRY_VISIBILITY))            {return 7;}
        else if (entry.equals(ENTRY_MARKERS))               {return 8;}
        else if (entry.equals(ENTRY_ASSET))                 {return 9;}
        else if (entry.equals(PATTERN_ASSETS))              {return 10;}
        else if (entry.equals(ENTRY_MODS))                  {return 11;}
        else if (entry.equals(ENTRY_END_OF_STREAM))         {return 12;}
        else return -1;
    }

    private static final byte[] THUMB_MAGIC_NUMBERS = {0, 1, 1, 2, 3, 5, 8};

    private static final int FIREHOSE_BUFFER_LIMIT = 1000; //Making records 1 KB
    private static final int BATCH_PUT_MAX_SIZE = 500;       //Batch of 0.5 MB

    private ByteBuffer streamBuffer;
    private final AmazonKinesisFirehose firehoseClient;
    private final String streamName;

    private final Map<String, OutputStream> outputStreams = new HashMap<>();

    private long bytesWritten = 0;
    private int sequenceNumber = 0;

    private final List<Record> recordList = new ArrayList<Record>();
    private int recordListLength = 0;

    private final Logger logger;

    //TODO add a gzip compression step before streaming to firehose
    public StreamReplayFile(Studio studio, AmazonKinesisFirehose firehoseClient, String streamName, Logger logger) throws IOException {
        super(studio);

        this.logger = logger;

        this.firehoseClient = firehoseClient;
        this.streamName = streamName;

        //Allocate buffer for stream
        this.streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);

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
    public void save() throws IOException {
        logger.info("Saving");
        logger.info("Wrote " + Long.toString(bytesWritten) + " bytes in total");
        flushToStream();
    }

    @Override
    public void close() throws IOException {
        logger.info("Closing stream");

        for (OutputStream out : outputStreams.values()) {
            Closeables.close(out, true);
        }
        outputStreams.clear();

        byte[] EOF = "This is the end.".getBytes();
        sendToStream(ENTRY_END_OF_STREAM, 0, EOF, EOF.length);
        flushToStream();
    }

    private void putBatchRecords(){
        logger.info("Puting Records (" + Integer.toString(recordListLength) + ") in batch");
        PutRecordBatchRequest recordBatchRequest = new PutRecordBatchRequest();
        recordBatchRequest.setDeliveryStreamName(streamName);
        recordBatchRequest.setRecords(recordList);
        PutRecordBatchResult result = firehoseClient.putRecordBatch(recordBatchRequest);
        logger.info("Put Batch Result: " + result.getFailedPutCount() + " records failed - http:" + Integer.toString(result.getSdkHttpMetadata().getHttpStatusCode()));

        recordList.clear();
        recordListLength = 0;
    }

    // Removed to provide single syncronous access to recordList 
    // synchronized private void addRecord(Record record){
    //     recordList.add(record);
    //     recordListLength += 1;
    //     if (recordListLength == BATCH_PUT_MAX_SIZE){
    //         putBatchRecords();
    //     }
    // }

    /*
    * Adds the given buffer to the current batch of records
    * Allocates a new stream buffer and calls putBatchRecords 
    * if the record list is BATCH_PUT_MAX_SIZE
    * TODO add a proper lock to recordList 
    */
    synchronized private void batchAddStreamBuffer(){
        recordList.add(new Record().withData(ByteBuffer.wrap(streamBuffer.array(), 0, streamBuffer.position())));
        recordListLength += 1;
        if (recordListLength == BATCH_PUT_MAX_SIZE){
            putBatchRecords();
        }
        streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);
    }

    /* 
    *  Write methods
    *  
    *  The following methods are adapted to write data to aws kinesis firehose
    *  streams. Streming data is structured as follows:
    *  
    *  int      :   Entry Type
    *  int      :   Timestamp (0 if not defined)
    *  int      :   Size of data
    *  byte[]   :   Data
    *
    */
    synchronized private void sendToStream(String entry, int timestamp, byte[] data, int length) throws IOException {
        // Determine index
        int entry_id = indexOf(entry);

        if(length != data.length){
            logger.error("Warning! Data size is not consistent");
        }
        
        // Calculate header overhead
        int overhead = Integer.BYTES; //Entry ID
        overhead    += Integer.BYTES; //Sequence Number
        overhead    += Integer.BYTES; //Timestamp
        overhead    += Integer.BYTES; //Length

        bytesWritten += length + overhead;

        if (streamBuffer.position() + length + overhead < streamBuffer.capacity())
        {
            streamBuffer.putInt(entry_id);
            streamBuffer.putInt(sequenceNumber++);
            streamBuffer.putInt(timestamp);
            streamBuffer.putInt(length);
            streamBuffer.put(data, 0 , length);
            return;
        } else if (length + overhead < streamBuffer.capacity()) {
            // Send existing data (clearing streamBuffer)
            batchAddStreamBuffer();

            // Add the data that didn't fit
            streamBuffer.putInt(entry_id);
            streamBuffer.putInt(sequenceNumber++);
            streamBuffer.putInt(timestamp);
            streamBuffer.putInt(length);
            streamBuffer.put(data, 0, length);
        } else {
            // Send existing data if there is not enough space for the header
            if (streamBuffer.position() + overhead >= streamBuffer.capacity()){
                batchAddStreamBuffer();                
            }
            
            //Add the header
            streamBuffer.putInt(entry_id);
            streamBuffer.putInt(sequenceNumber++);
            streamBuffer.putInt(timestamp);
            streamBuffer.putInt(length);

            // Add the data up to FIREHOSE_BUFFER_LIMIT bytes at a time
            int bytesRead = 0;            
            while (bytesRead < length) {
                int numBytes = Math.min(streamBuffer.capacity() - streamBuffer.position(), length - bytesRead);
                try {
                    streamBuffer.put(data, bytesRead, numBytes);
                } catch (Exception e) {
                    logger.info("Excepton" + e.toString());
                }
                
                bytesRead += numBytes;
                batchAddStreamBuffer();
            }
        }
    }

    private void flushToStream(){
        if( streamBuffer.position() != 0){
            // Put records on stream
            batchAddStreamBuffer();
        }
        //Flush all records by putRecordBatch
        putBatchRecords();       
    }

    public void writeByte(String entry, int data) throws IOException {
        logger.info("Tried to call writeByte - cmd unsupported");
        throw new UnsupportedOperationException("writeByte is not supported for replay type StreamReplayFile");
    }

    public void writeEntry(String entry, int timestamp, int len, byte[] bytes) throws IOException {
        logger.info("Wrote Entry (" + Integer.toString(len) + ") bytes");
        sendToStream(entry, timestamp, bytes, len);
    }


    @Override
    public void writePackets(int timestamp, int length, byte[] data) throws IOException {
        sendToStream(ENTRY_RECORDING, timestamp, data, length);
    }

    @Override
    public void writeMetaData(ReplayMetaData metaData) throws IOException {
        metaData.setFileFormat("MCPR");
        metaData.setFileFormatVersion(ReplayMetaData.CURRENT_FILE_FORMAT_VERSION);
        if (metaData.getGenerator() == null) {
            metaData.setGenerator("ReplayStudio v" + studio.getVersion());
        }

        String json = new Gson().toJson(metaData);
        sendToStream(ENTRY_META_DATA, 0, json.getBytes(), json.length());
    }

    @Override
    public void writeResourcePackIndex(Map<Integer, String> index) throws IOException {
        String json = new Gson().toJson(index);
        sendToStream(ENTRY_RESOURCE_PACK_INDEX, 0, json.getBytes(), json.length());
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
        sendToStream(ENTRY_VISIBILITY, 0, json.getBytes(), json.length());
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
        sendToStream(ENTRY_MODS, 0, json.getBytes(), json.length());
        
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
        sendToStream(ENTRY_MARKERS, 0, json.getBytes(), json.length());
    }

    @Override
    public OutputStream write(String entry) throws IOException {
        //Create buffered output stream
        logger.info("Gave out Output stream for " + entry);
        OutputStream out = new BufferedOutputStream(new StreamingOutputStream(entry, this));
        Closeables.close(outputStreams.put(entry, out), true);
        return out;
    }




    /* 
    *  Removed / Unsupported methods
    *  
    *  The following methods are not supporeted by the streaming replay file
    *  so they have been overidden to prevent confusion
    *
    */

    @Override
    public Map<String, InputStream> getAll(Pattern pattern) throws IOException {
        logger.info("Tried to call getAll - cmd unsupported");
        throw new UnsupportedOperationException("getAll is not supported for replay type StreamReplayFile");
    }

    @Override
    public void saveTo(File target) throws IOException {
        logger.info("SaveTo Failed");
        throw new UnsupportedOperationException("Save to file not supported for replay type StreamReplayFile");
    }

    @Override
    public void remove(String entry) throws IOException {
        logger.info("Remove Failed");
        throw(new IOException());
    }

    @Override
    public ReplayInputStream getPacketData() throws IOException {
        logger.info("getPacketData Failed");
        return getPacketData(studio);
    }

    @Override
    public ReplayInputStream getPacketData(Studio studio) throws IOException {
        logger.info("getPacketData Failed");
        throw new UnsupportedOperationException("getPacketData not supported for replay type StreamReplayFile");
    }

    @Override
    public ReplayOutputStream writePacketData() throws IOException {
        logger.info("writePacketData Failed");
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public Replay toReplay() throws IOException {
        logger.info("toReplay Failed");
        throw new UnsupportedOperationException("toReplay not supported");
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
        return write(String.format(ENTRY_ASSET, asset.getUuid().toString(), asset.getName(), asset.getFileExtension()));
    }

    @Override
    public void removeAsset(UUID uuid) throws IOException {
        logger.info("removeAsset Failed");
        throw new UnsupportedOperationException("remove asset not supported");
        // Function not supported by streaming replay files
    }

}

