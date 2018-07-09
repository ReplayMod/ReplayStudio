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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
    protected static final String ENTRY_EXP_METADATA  = "experement_metadata.json";
    

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
        else if (entry.equals(ENTRY_EXP_METADATA))          {return 12;}
        else if (entry.equals(ENTRY_END_OF_STREAM))         {return 13;}
        else if (entry.equals(ENTRY_ACTIONS))               {return 14;}
        else return -1;
    }

    private static final byte[] THUMB_MAGIC_NUMBERS = {0, 1, 1, 2, 3, 5, 8};

    private static final int FIREHOSE_BUFFER_LIMIT = 1000; //Making records 1 KB
    private static final int BATCH_PUT_MAX_SIZE = 500;       //Batch of 0.5 MB

    private ByteBuffer streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);;
    private final AmazonKinesisFirehose firehoseClient;
    private final String streamName;

    private final Map<String, OutputStream> outputStreams = new HashMap<>();

    private long bytesWritten = 0;
    private int sequenceNumber = 0;

    private final List<Record> recordList = new ArrayList<Record>();
    private int recordListLength = 0;

    private final Logger logger;

    private final String streamMetadata;

    //TODO add a gzip compression step before streaming to firehose
    public StreamReplayFile(Studio studio, AmazonKinesisFirehose firehoseClient, String streamName, String metaData, Logger logger) throws IOException {
        super(studio);

        this.logger = logger;

        this.firehoseClient = firehoseClient;
        this.streamName = streamName;
        this.streamMetadata = metaData;

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

        sendToStream(ENTRY_EXP_METADATA, 0, streamMetadata.getBytes(), 0, streamMetadata.getBytes().length);
        byte[] EOF = "This is the end.".getBytes();
        sendToStream(ENTRY_END_OF_STREAM, 0, EOF, 0, EOF.length);
        flushToStream();
    }

    private void putBatchRecords(){
        logger.info("Puting Records (" + Integer.toString(recordListLength) + ") in batch");
        try{
            PutRecordBatchRequest recordBatchRequest = new PutRecordBatchRequest();
            recordBatchRequest.setDeliveryStreamName(streamName);
            recordBatchRequest.setRecords(recordList);
            PutRecordBatchResult result = firehoseClient.putRecordBatch(recordBatchRequest);
            recordList.clear();
            recordListLength = 0;
            logger.info("Put Batch Result: " + result.getFailedPutCount() + " records failed - http:" + Integer.toString(result.getSdkHttpMetadata().getHttpStatusCode()));
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Put Batch Threw Exception");
        } 


        
    }

    /*
    * Adds the given buffer to the current batch of records
    * Allocates a new stream buffer and calls putBatchRecords 
    * if the record list is BATCH_PUT_MAX_SIZE
    * TODO add a proper lock to recordList 
    */
    synchronized private void batchAddStreamBuffer(){
        try {
            recordList.add(new Record().withData(ByteBuffer.wrap(streamBuffer.array(), 0, streamBuffer.position())));
            recordListLength += 1;
            if (recordListLength == BATCH_PUT_MAX_SIZE){
                putBatchRecords();
            }
            streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("batchAddStreamBuffer threw exception!");
        }

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
    synchronized private void sendToStream(String entry, int timestamp, byte[] data, int offset, int length) throws IOException {
        // Determine index
        int entry_id = indexOf(entry);

        if(length > data.length){
            logger.error("Warning! Data size is not consistent for entry " + entry);
            length = Math.min(length, data.length);
        }
        
        // Calculate header overhead
        int overhead = Integer.BYTES; //Entry ID
        overhead    += Integer.BYTES; //Sequence Number
        overhead    += Integer.BYTES; //Timestamp
        overhead    += Integer.BYTES; //Length

        bytesWritten += length + overhead;

        try {
            //streamBufferLock.lock();
            if (streamBuffer.position() + length + overhead < streamBuffer.capacity())
            {
                streamBuffer.putInt(entry_id);
                streamBuffer.putInt(sequenceNumber++);
                streamBuffer.putInt(timestamp);
                streamBuffer.putInt(length);
                streamBuffer.put(data, offset, length);
                return;
            } else if (length + overhead < streamBuffer.capacity()) {
                // Send existing data (clearing streamBuffer)
                batchAddStreamBuffer();

                // Add the data that didn't fit
                streamBuffer.putInt(entry_id);
                streamBuffer.putInt(sequenceNumber++);
                streamBuffer.putInt(timestamp);
                streamBuffer.putInt(length);
                streamBuffer.put(data, offset, length);
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
                int bytesRead = offset;            
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
            //streamBufferLock.unlock();
        } catch (Exception e) {
            //if (streamBufferLock.tryLock()) {
            //    streamBufferLock.unlock();
            //} 
            e.printStackTrace();
            logger.error("batchAddStreamBuffer threw exception!");
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
        byte[] buff = ByteBuffer.allocate(4).putInt(data).array();
        sendToStream(entry, 0, buff, 0, 4);
    }

    public void writeEntry(String entry, int timestamp, int offset, int len, byte[] bytes) throws IOException {
        sendToStream(entry, timestamp, bytes, offset, len);
    }

    @Override
    public void writeThumb(BufferedImage image) throws IOException {
        // Not supported
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
        logger.error("Tried to call getAll - cmd unsupported");
        throw new UnsupportedOperationException("getAll is not supported for replay type StreamReplayFile");
    }

    @Override
    public void saveTo(File target) throws IOException {
        logger.error("SaveTo Failed");
        throw new UnsupportedOperationException("Save to file not supported for replay type StreamReplayFile");
    }

    @Override
    public void remove(String entry) throws IOException {
        logger.error("Remove Failed");
        throw(new IOException());
    }

    @Override
    public ReplayInputStream getPacketData() throws IOException {
        logger.error("getPacketData Failed");
        return getPacketData(studio);
    }

    @Override
    public ReplayInputStream getPacketData(Studio studio) throws IOException {
        logger.error("getPacketData Failed");
        throw new UnsupportedOperationException("getPacketData not supported for replay type StreamReplayFile");
    }

    @Override
    public Replay toReplay() throws IOException {
        logger.error("toReplay Failed");
        throw new UnsupportedOperationException("toReplay not supported");
    }

    @Override
    public Optional<InputStream> get(String entry) throws IOException {
        logger.error("Get inputstreams Failed");
        return Optional.absent();
    }

    @Override
    public void removeAsset(UUID uuid) throws IOException {
        logger.error("removeAsset Failed");
        throw new UnsupportedOperationException("remove asset not supported");
        // Function not supported by streaming replay files
    }

}

