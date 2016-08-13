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
package com.replaymod.replaystudio.filter;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.stream.PacketStream;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerChunkDataPacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerMultiChunkDataPacket;
import org.spacehq.packetlib.packet.Packet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class JsonConverterFilter extends StreamFilterBase {

    private final Gson gson;
    {
        gson = new GsonBuilder().registerTypeAdapter(Class.class,
                (JsonSerializer<Class<?>>) (cls, type, jsonSerializationContext) -> new JsonPrimitive(cls.getName()))
                .create();
    }
    private JsonWriter jsonWriter;
    private File output;
    private boolean dumpChunks;

    @Override
    public String getName() {
        return "to_json";
    }

    @Override
    public void init(Studio studio, JsonObject config) {
        studio.setWrappingEnabled(false);

        if (config.has("output")) {
            output = new File(config.get("output").getAsString());
        } else {
            output = new File("packets.json");
        }
        if (config.has("dumpChunks")) {
            dumpChunks = config.get("dumpChunks").getAsBoolean();
        }
    }

    @Override
    public void onStart(PacketStream stream) {
        try {
            jsonWriter = new JsonWriter(new FileWriter(output));
            jsonWriter.setIndent("    ");
            jsonWriter.beginArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onPacket(PacketStream stream, PacketData data) {
        try {
            Packet packet = data.getPacket();
            jsonWriter.beginObject();
            jsonWriter.name("time").value(data.getTime());
            jsonWriter.name("type").value(data.getPacket().getClass().getSimpleName());
            jsonWriter.name("data");
            if (packet instanceof ServerChunkDataPacket && !dumpChunks) {
                ServerChunkDataPacket p = (ServerChunkDataPacket) packet;
                jsonWriter.beginObject();
                jsonWriter.name("x").value(p.getX());
                jsonWriter.name("z").value(p.getZ());
                jsonWriter.name("chunks").value(p.getChunks().length);
                jsonWriter.name("biomeData").value(p.getBiomeData().length);
                jsonWriter.endObject();
            } else if (packet instanceof ServerMultiChunkDataPacket && !dumpChunks) {
                jsonWriter.beginObject();
                jsonWriter.name("chunks").value(((ServerMultiChunkDataPacket) packet).getColumns());
                jsonWriter.endObject();
            } else {
                gson.toJson(data.getPacket(), data.getPacket().getClass(), jsonWriter);
            }
            jsonWriter.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onEnd(PacketStream stream, long timestamp) {
        try {
            jsonWriter.endArray();
            jsonWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
