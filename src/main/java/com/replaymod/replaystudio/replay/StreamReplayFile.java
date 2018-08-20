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

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.services.kinesisfirehose.model.DeliveryStreamDescription;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResponseEntry;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.amazonaws.services.kinesisfirehose.model.ServiceUnavailableException;
import com.google.common.io.Closeables;
import com.google.common.base.Optional;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.io.StreamingOutputStream;

import org.apache.logging.log4j.Logger;

import org.apache.commons.lang3.tuple.Pair;

final class FirehosePair {
    private AmazonKinesisFirehose client;
    private String name;
    private DatagramSocket socket;

    public FirehosePair(AmazonKinesisFirehose client, String name, DatagramSocket socket){
        this.client = client;
        this.name = name;
        this.socket = socket;}
    public AmazonKinesisFirehose getClient() {return client;}
    public String getName() {return name;}
    public DatagramSocket getSocket() {return socket;}

    public void setClient(AmazonKinesisFirehose client) {this.client = client;}
    public void setName(String name) {this.name = name;}
}

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

    private static final int FIREHOSE_MAX_CLIENT_CREATION_DELAY = (10 * 60 * 1000);
    private static final int FIREHOSE_CLIENT_STATE_REFRESH_DELAY = 100;
    private static final int FIREHOSE_BUFFER_LIMIT = 1000 * 1024; //Making records 1000 KB (1000 KB max)
    private static final int BATCH_PUT_MAX_SIZE = 4;       //Batch of 4000 KB (4MiB max)

    private ByteBuffer streamBuffer = ByteBuffer.allocate(FIREHOSE_BUFFER_LIMIT);
    private final DatagramSocket userServerSocket;
    private final AmazonKinesisFirehose firehoseClient;
    private String version;
    private String streamName;
    private String streamVersion;
    private String uid;

    private final Map<String, OutputStream> outputStreams = new HashMap<>();

    private long bytesWritten = 0;
    private int sequenceNumber = 0;

    private final Queue<Record> recordList = new LinkedList<Record>();
    private int recordListLength = 0;

    private final Logger logger;

    private Set<Marker> markers = new HashSet<>();

    //TODO add a gzip compression step before streaming to firehose
    public StreamReplayFile(Studio studio, String uid, String version, Logger logger) throws IOException {
        super(studio);

        this.uid = uid;
        this.logger = logger;
        this.version = version;

        FirehosePair firehose = getFirehoseStream(uid);
        if (firehose == null) {
            throw(new IOException("Unable to get firehose stream "));}
        this.firehoseClient = firehose.getClient();
        this.streamName = firehose.getName();
        this.userServerSocket = firehose.getSocket();
        //Check that our firehose stream is open and active
        if (!checkFirehoseStreamActive(streamName, firehoseClient)){
            throw(new IOException("Delivery stream not active!"));
        }   

    }

    private boolean checkFirehoseStreamActive(String streamName, AmazonKinesisFirehose firehoseClient){
        DescribeDeliveryStreamRequest describeDeliveryStreamRequest = new DescribeDeliveryStreamRequest();
        describeDeliveryStreamRequest.withDeliveryStreamName(streamName);
        DescribeDeliveryStreamResult describeDeliveryStreamResponse =
            firehoseClient.describeDeliveryStream(describeDeliveryStreamRequest);
        DeliveryStreamDescription  deliveryStreamDescription = describeDeliveryStreamResponse.getDeliveryStreamDescription();
        String deliveryStreamStatus = deliveryStreamDescription.getDeliveryStreamStatus();
        streamVersion = deliveryStreamDescription.getVersionId();
        return deliveryStreamStatus.equals("ACTIVE");
    }

    private FirehosePair getFirehoseStream(String uid){
        ////////////////////////////////////////////
        //       FireHose Key Retrieval           //
        ////////////////////////////////////////////
        InetAddress userServerAddress;
        DatagramSocket userServerSocket;
        try {
            //Connect to UserServer
            userServerSocket = new DatagramSocket();
            
            userServerAddress = InetAddress.getByName("user.herobraine.stream");
            userServerSocket.connect(userServerAddress, 9999);
            userServerSocket.setSoTimeout(1000);                        
        } catch (SocketException | UnknownHostException e) {
            logger.info("Error establishing connection to user server");
            e.printStackTrace();
            logger.error("Error establishing connection to user server");
            return null;
        }
        
        // Send Firehose key request
        JsonObject firehoseJson  = new JsonObject();
        firehoseJson.addProperty("cmd", "get_firehose_key");
        firehoseJson.addProperty("version", version);
        firehoseJson.addProperty("uid", uid);
        String firehoseStr = firehoseJson.toString();
        DatagramPacket firehoseKeyRequest = new DatagramPacket(firehoseStr.getBytes(), firehoseStr.getBytes().length);
        try {
            userServerSocket.send(firehoseKeyRequest);
        } catch (IOException e) {
            e.printStackTrace();
            userServerSocket.close();
            return null;
        }
    
        // Get response
        String tmp= null; 
        JsonObject awsKeys = null;
        byte[] buff1 = new byte[65535];
        BasicSessionCredentials awsCredentials;
        DatagramPacket firehoseKeyData = new DatagramPacket(buff1, buff1.length);
        try {
            
            while (tmp == null){
                userServerSocket.receive(firehoseKeyData);
                String dataStr = new String(firehoseKeyData.getData(), firehoseKeyData.getOffset(), firehoseKeyData.getLength());
                awsKeys = new JsonParser().parse(dataStr).getAsJsonObject();
                if (awsKeys.get("stream_name") != null){
                    tmp = awsKeys.get("stream_name").getAsString();
                } else if (awsKeys.get("error").getAsBoolean()) {
                    logger.error("Error retreaving stream credentials");
                    logger.error(awsKeys.get("message"));
                    userServerSocket.close();
                    return null;
                }
            }
            
            this.streamName = tmp;
            awsCredentials = new BasicSessionCredentials(
                awsKeys.get("access_key").getAsString(),
                awsKeys.get("secret_key").getAsString(),
                awsKeys.get("session_token").getAsString());
            
        
        } catch (NullPointerException | IOException e) {
            logger.error("Could not parse returned firehose stream infromation");
            if (awsKeys != null){
                logger.error("Tried to parse " + awsKeys.toString());
            }
            e.printStackTrace();
            userServerSocket.close();
            //mcServerSocket.close();
            returnFirehoseStream();
            return null;
        }
        
        logger.info(String.format("StreamName:    %s%n", streamName));
        //logger.info(String.format("Access Key:    %s%n", accessKey));
        //logger.info(String.format("Secret Key:    %s%n", secretKey));
        //logger.info(String.format("Session Token: %s%n", sessionToken));

        // Firehose client
        AmazonKinesisFirehose firehoseClient = AmazonKinesisFirehoseClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .withRegion("us-east-1")
            .build();

        //Check if the given stream is open
        boolean timeout = true;
        long startTime = System.currentTimeMillis();
        long endTime = startTime + FIREHOSE_MAX_CLIENT_CREATION_DELAY; //TODO reduce maximum delay
        while (System.currentTimeMillis() < endTime) {
            try {
                Thread.sleep(FIREHOSE_CLIENT_STATE_REFRESH_DELAY);
            } catch (InterruptedException e) {
                // Ignore interruption (doesn't impact deliveryStream creation)
            }

            if (checkFirehoseStreamActive(streamName, firehoseClient)) {
                timeout = false;
                break;
            }
        }

        if (timeout) {
            logger.error("Waited too long for stream activation! Stream may be mis-configured!");
            // TODO handle this cleanly
        } else {
            logger.info("Active Firehose Stream Established!");
        }
        return new FirehosePair(firehoseClient, streamName, userServerSocket);
    }


    private void returnFirehoseStream(){
        if (streamName == null){ return;}
        // Send Minecraft dissconnect notification
        JsonObject mcKeyJson = new JsonObject();
        mcKeyJson.addProperty("cmd", "return_firehose_key");
        mcKeyJson.addProperty("uid", uid);
        mcKeyJson.addProperty("stream_name", streamName);
        String mcKeyStr = mcKeyJson.toString();
        DatagramPacket mcKeyRequest = new DatagramPacket(mcKeyStr.getBytes(), mcKeyStr.getBytes().length);
        try {
            userServerSocket.send(mcKeyRequest);
        } catch (IOException e) {
            logger.error(e.getMessage());
            return;
        }

        byte[] buff1 = new byte[2400];
        DatagramPacket returnResponse = new DatagramPacket(buff1, buff1.length);
        try {
            userServerSocket.receive(returnResponse);
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            String dataStr = new String(returnResponse.getData(), returnResponse.getOffset(), returnResponse.getLength());
            JsonElement json = new JsonParser().parse(dataStr).getAsJsonObject();
            logger.info("Return result: " + json.toString());   
        }
    }

    public String getStreamVersion() {
        return streamVersion;
    }
    public String getStreamName() {
        return streamName;
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
        sendToStream(ENTRY_END_OF_STREAM, 0, EOF, 0, EOF.length);
        flushToStream();
        returnFirehoseStream();
    }

    private PutRecordResult putRecord(Record record){
        PutRecordRequest recordRequest = new PutRecordRequest();
        recordRequest.setDeliveryStreamName(streamName);
        recordRequest.setRecord(record);
        return firehoseClient.putRecord(recordRequest);
    }

    private void putBatchRecords(){
        //BAH removed batch so records are put in order - record re-delivery will be attempted for Service Unavailable Excepetions
        if (recordList.size() == 0) {return;}
        logger.info("Puting Record...");
        Record record = recordList.remove();
        recordListLength--;

        boolean recordDelivered = false;
        while (!recordDelivered){
            try{
                PutRecordResult result = putRecord(record);
    
                if (result.getSdkHttpMetadata().getHttpStatusCode() == 200){
                    recordDelivered = true;
                    break;
                } else {
                    logger.error("Put record failed! Http status code " + Integer.toString(result.getSdkHttpMetadata().getHttpStatusCode()));
                } 
                
            } catch (ServiceUnavailableException e){
                logger.error("Put Record Service Unavailable - will retry");
            }  catch (ProvisionedThroughputExceededException e){
                logger.error("Put Record Connection Throttled - will retry");
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Put Record Threw Unrecorverable Exception");
                return;
            }    
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error("Interupted");
            }

            logger.info("Retrying record delivery");
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
            if (recordListLength > 0){
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
    *  streams. Streaming data is structured as follows:
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
            // If data fits in current record
            if (streamBuffer.position() + length + overhead < streamBuffer.capacity())
            {
                streamBuffer.putInt(entry_id);
                streamBuffer.putInt(sequenceNumber++);
                streamBuffer.putInt(timestamp);
                streamBuffer.putInt(length);
                streamBuffer.put(data, offset, length);
                return;
            } 
            // Data won't fit in current record but is less then a record
            else if (length + overhead < streamBuffer.capacity()) {
                // Send existing data (clearing streamBuffer)
                batchAddStreamBuffer();

                // Add the data that didn't fit
                streamBuffer.putInt(entry_id);
                streamBuffer.putInt(sequenceNumber++);
                streamBuffer.putInt(timestamp);
                streamBuffer.putInt(length);
                streamBuffer.put(data, offset, length);
            }
            // Data needs multiple records 
            else {
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
        } catch (Exception e) {
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

    @Override
    public Optional<Set<Marker>> getMarkers() throws IOException {
        if (markers == null) {return Optional.absent();}
        return Optional.of(markers);
    }

    @Override
    public void writeMarkers(Set<Marker> newMarkers) throws IOException {
        this.markers = newMarkers;
        super.writeMarkers(newMarkers);
    }

    /* 
    *  Removed / Unsupported methods
    *  
    *  The following methods are not supporeted by the streaming replay file
    *  so they have been overidden to prevent confusion
    *
    */

    @Override
    public ReplayMetaData getMetaData() throws IOException {
        logger.error("Tried to call getMetaData - cmd unsupported");
        throw new UnsupportedOperationException("Getting metadata is not supported for replay type StreamReplayFile");
    }

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

