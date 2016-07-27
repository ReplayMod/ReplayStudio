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
