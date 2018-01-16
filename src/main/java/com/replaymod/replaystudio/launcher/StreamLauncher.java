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
package com.replaymod.replaystudio.launcher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.filter.StreamFilter;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.stream.PacketStream;
import com.replaymod.replaystudio.studio.ReplayStudio;
import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.in;

public class StreamLauncher {

    private final Studio studio = new ReplayStudio();

    public void launch(CommandLine cmd) throws IOException {
        if (cmd.hasOption('n')) {
            studio.setWrappingEnabled(false);
        }
        // Removes the first minute, applies sample_filter on the whole stream and applies some_other at 3m for 10s:
        //   remove(-1m),sample_filter,some_other(3m-3m10s)
        List<PacketStream.FilterInfo> filters = new ArrayList<>();
        String[] instructions;
        if (cmd.hasOption('q')) {
            instructions = new String[]{"squash"};
        } else {
            instructions = cmd.getOptionValue('s').split(",");
        }
        for (String instruction : instructions) {
            long start, end;
            if (instruction.charAt(instruction.length()-1) == ')') {
                int index = instruction.indexOf('(');
                String time = instruction.substring(index+1, instruction.length()-1);
                instruction = instruction.substring(0, index);
                start = timeStampToMillis(time.split("-", 2)[0]);
                end = timeStampToMillis(time.split("-", 2)[1]);
            } else {
                start = end = -1;
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

        String input = cmd.getArgs()[0];
        String output = cmd.getArgs()[1];

        long start = System.nanoTime();
        System.out.println("Generating " + ("x".equals(output) ? 0 : 1) + " replay via 1 stream from 1 input applying " + filters.size() + " filter(s)");

        ReplayFile inFile = new ZipReplayFile(studio, new File(input));
        ReplayOutputStream out;
        if (!"x".equals(output)) {
            OutputStream buffOut = new BufferedOutputStream(new FileOutputStream(output));
            out = new ReplayOutputStream(studio, buffOut, null);
        } else {
            out = null;
        }
        ReplayMetaData meta = inFile.getMetaData();
        PacketStream stream = inFile.getPacketData().asPacketStream();

        // Process stream
        stream.start();

        stream.addFilter(new ProgressFilter(meta.getDuration()));
        for (PacketStream.FilterInfo info : filters) {
            stream.addFilter(info.getFilter(), info.getFrom(), info.getTo());
        }

        System.out.println("Built pipeline: " + stream);

        PacketData data;
        if (out != null) { // Write output
            while ((data = stream.next()) != null) {
                out.write(data);
            }

            for (PacketData d : stream.end()) {
                out.write(d);
            }

            out.close();
        } else { // Drop output
            while (stream.next() != null);
            stream.end();
        }

        in.close();

        System.out.println("Done after " + (System.nanoTime() - start) + "ns");
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

    private static class ProgressFilter implements StreamFilter {

        private final long total;
        private int lastUpdate;

        public ProgressFilter(long total) {
            this.total = total;
        }

        @Override
        public String getName() {
            return "progress";
        }

        @Override
        public void init(Studio studio, JsonObject config) {

        }

        @Override
        public void onStart(PacketStream stream) {
            lastUpdate = -1;
        }

        @Override
        public boolean onPacket(PacketStream stream, PacketData data) {
            int pct = (int) (data.getTime() * 100 / total);
            if (pct > lastUpdate) {
                lastUpdate = pct;
                System.out.print("Processing... " + pct + "%\r");
            }
            return true;
        }

        @Override
        public void onEnd(PacketStream stream, long timestamp) {
            System.out.println();
        }
    }
}
