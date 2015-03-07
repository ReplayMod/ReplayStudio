package de.johni0702.replaystudio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.johni0702.replaystudio.api.Studio;
import de.johni0702.replaystudio.api.manipulation.StreamFilter;
import de.johni0702.replaystudio.api.packet.PacketData;
import de.johni0702.replaystudio.api.packet.PacketStream;
import de.johni0702.replaystudio.io.ReplayOutputStream;
import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StreamLauncher {

    private final Studio studio = new ReplayStudio();

    public void launch(CommandLine cmd) throws IOException {
        // Removes the first minute, applies sample_filter on the whole stream and applies some_other at 3m for 10s:
        //   remove(-1m),sample_filter,some_other(3m-3m10s)
        List<PacketStream.FilterInfo> filters = new ArrayList<>();
        String[] instructions = cmd.getOptionValue('s').split(",");
        for (String instruction : instructions) {
            long start, end;
            if (instruction.charAt(instruction.length()-1) == ')') {
                int index = instruction.indexOf('(');
                String time = instruction.substring(index+1, instruction.length()-1);
                instruction = instruction.substring(0, index);
                start = timeStampToMillis(time.split("-", 2)[0]);
                end = timeStampToMillis(time.split("-", 2)[1]);
            } else {
                start = end = 0;
            }
            JsonObject config;
            if (instruction.charAt(instruction.length()-1) == ']') {
                int index = instruction.indexOf('[');
                String str = instruction.substring(index+1, instruction.length()-1);
                instruction = instruction.substring(0, index);
                config = new JsonParser().parse("{" + str + "}").getAsJsonObject();
            } else {
                config = new JsonObject();
            }
            StreamFilter filter = studio.loadStreamFilter(instruction);
            if (filter == null) {
                throw new IllegalStateException("Filter not found: " + instruction);
            }
            filter.init(studio,config);
            filters.add(new PacketStream.FilterInfo(filter, start, end));
        }

        long start = System.nanoTime();
        System.out.println("Generating 1 replay via 1 stream from 1 input applying " + filters.size() + " filter(s)");

        InputStream in = new BufferedInputStream(new FileInputStream(cmd.getArgs()[0]));
        OutputStream buffOut = new BufferedOutputStream(new FileOutputStream(cmd.getArgs()[1]));
        ReplayOutputStream out = new ReplayOutputStream(studio, buffOut, null);
        PacketStream stream = studio.createReplayStream(in, false);

        // Process stream
        stream.start();

        for (PacketStream.FilterInfo info : filters) {
            stream.addFilter(info.getFilter(), info.getFrom(), info.getTo());
        }

        System.out.println("Built pipeline: " + stream);

        PacketData data;
        while ((data = stream.next()) != null) {
            out.write(data);
        }

        for (PacketData d : stream.end()) {
            out.write(d);
        }

        in.close();
        out.close();

        System.out.println("Done after " + (System.nanoTime() - start) + "ms");
    }

    private long timeStampToMillis(String string) {
        if (string.length() == 0) {
            return -1;
        }
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            long time = 0;
            int hIndex = string.indexOf('h');
            if (hIndex != -1) {
                time += 3600000 * Integer.parseInt(string.substring(0, hIndex));
                if (string.length() - 1 > hIndex) {
                    string = string.substring(hIndex + 1);
                }
            }
            int mIndex = string.indexOf('m');
            if (mIndex != -1) {
                time += 60000 * Integer.parseInt(string.substring(0, mIndex));
                if (string.length() - 1 > mIndex) {
                    string = string.substring(mIndex + 1);
                }
            }
            int sIndex = string.indexOf('s');
            if (sIndex != -1) {
                time += 1000 * Integer.parseInt(string.substring(0, sIndex));
                if (string.length() - 1 > sIndex) {
                    string = string.substring(sIndex + 1);
                }
            }
            int msIndex = string.indexOf("ms");
            if (msIndex != -1) {
                time += Integer.parseInt(string.substring(0, msIndex));
            }
            return time;
        }
    }

}
