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

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Longs;
import com.google.gson.*;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.ReplayPart;
import com.replaymod.replaystudio.filter.Filter;
import com.replaymod.replaystudio.replay.Replay;
import com.replaymod.replaystudio.studio.ReplayStudio;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @deprecated Use {@link StreamLauncher} instead.
 */
@Deprecated
public class Launcher {

    public static void main(String[] args) throws Exception {
//        Message msg = new TranslationMessage("key");
//        System.out.println(msg.toJsonString());
//        msg = Message.fromString(msg.toJsonString());
//        System.out.println(msg.toJsonString());

        try {
            run(args);
        } catch (CriticalException e) {
            System.exit(e.getReturnCode());
        }
    }

    public static void run(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("h", "help", false, "Shows the help page.");
        options.addOption("w", "wait", true, "[Debugging] Amount of seconds to wait before starting.");
        options.addOption("n", "no-wrapping", false, "[Debugging] Disables packet wrapping.");
        options.addOption("j", "json-config", true, "Use the supplied json config file to load instructions.");
        options.addOption("c", "config", true, "Use the supplied string to load instructions.");
        options.addOption("s", "stream", true, "Use streams instead of loading whole replays into RAM." +
                " (Only supported by stream filters)");
        options.addOption("p", "parts", true, "Splits the first replay at the specified position. " +
                "If supplied one timestamp either as milliseconds or as 10m37s420ms, splits at the target position. " +
                "If supplied multiple timestamps separated by \":\", splits at every position.\n" +
                "Every subsequent replay file will be used as the outputs file for each part of the split replay. " +
                "Use \"x\" as the outputs file to skip saving of that part.\n" +
                "This is equivalent to -c \"(<a,>b,>c,>d)(a|10m20s,1h42s|b,c,d)\"\n");
        options.addOption("a", "append", true, "Concatenates every supplied replay and saves the result in the last replay file.\n" +
                "This is equivalent to -c \"(<a,<b,<c,>d)(a,b,c&d)\"");
        options.addOption("f", "filter", true, "Applies the specified filter to the supplied replay and saves the result in the second replay file.\n" +
                "This is equivalent to -c \"(<a,>b)(a>SomeFilterName>b)\"");
        options.addOption("q", "squash", false, "Squash the supplied replay and save the result in the second replay file.\n" +
                "This is equivalent to -c \"(<a,>b)(a[b)\"");
        options.addOption("r", "reverse", false, "Reverses the packet order in the specified replay and writes it" +
                "to the specified output file. Note that the output file if raw packet data, not a zipped replay with meta data.");
        options.addOption("d", "daemon", true, "Runs replay studio as a daemon listening for requests. Pass in the amount" +
                "of concurrent worker threads. The port of the daemon is defined by the environment variable 'replaystudio.port' (default 4002).");

        CommandLineParser parser = new GnuParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            throw new CriticalException(2);
        }

        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            String footer = "Output files can be suffixed with \"!<name>!<singleplayer>!<time>\"\n" +
                    "where <singleplayer> is either \"true\" or \"false\", <name> is the server name or " +
                    "singleplayer world names and <time> the time in milliseconds at which the replay was recorded.\n" +
                    "Specifying only the name is also possible with \"!<name>\"." +
                    "Setting an output to \"x\" discards the replay.";
            formatter.printHelp(100, "<cmd> <options> <file> [file .. [file ..]]", "", options, footer);
            return;
        }

        if (cmd.hasOption('w')) {
            try {
                Thread.sleep((long) (Double.parseDouble(cmd.getOptionValue('w'))*1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (cmd.hasOption('s') || cmd.hasOption('q')) {
            new StreamLauncher().launch(cmd);
        } else if (cmd.hasOption('r')) {
            new ReverseLauncher().launch(cmd);
        } else if (cmd.hasOption('d')) {
            new DaemonLauncher().launch(cmd);
        } else {
            new Launcher().launch(cmd);
        }
    }

    private final Map<String, String> inputs = new HashMap<>();
    private final Set<String> pipes = new HashSet<>();
    private final Map<String, String> outputs = new HashMap<>();
    private final List<Instruction> instructions = new ArrayList<>();

    public void launch(CommandLine cmd) throws FileNotFoundException {
        Studio studio = new ReplayStudio();
        if (cmd.hasOption('n')) {
            studio.setWrappingEnabled(false);
        }

        if (cmd.hasOption('j')) {
            JsonObject o;
            try {
                o = new JsonParser().parse(cmd.getOptionValue('j')).getAsJsonObject();
            } catch (JsonParseException | IllegalStateException e) {
                Reader reader;
                if ("".equals(cmd.getOptionValue('j'))) {
                    reader = new InputStreamReader(System.in);
                } else {
                    reader = new FileReader(cmd.getOptionValue('j'));
                }
                o = new JsonParser().parse(reader).getAsJsonObject();
            }
            parseConfig(studio, o);
        } else if (cmd.hasOption('c')) {
            parseConfig(studio, cmd.getOptionValue('c'), cmd.getArgs());
        } else if (cmd.hasOption('p')) {
            SplitInstruction instruction = new SplitInstruction();
            instruction.getInputs().add("a");
            inputs.put("a", cmd.getArgs()[0]);
            for (int i = 1; i < cmd.getArgs().length; i++) {
                outputs.put(String.valueOf(i), cmd.getArgs()[i]);
                instruction.getOutputs().add(String.valueOf(i));
            }
            instructions.add(instruction);
        } else if (cmd.hasOption('f')) {
            Filter filter = studio.loadFilter(cmd.getOptionValue('f'));
            FilterInstruction instruction = new FilterInstruction(studio, filter, new JsonObject());
            instruction.getInputs().add("a");
            instruction.getOutputs().add("b");
            inputs.put("a", cmd.getArgs()[0]);
            outputs.put("b", cmd.getArgs()[1]);
            instructions.add(instruction);
        } else {
            System.out.println("Missing instruction. Use --help to show help.");
            throw new CriticalException(2);
        }

        System.out.println(String.format("Generating %d replay(s) via %d pipe(s) from %d input(s)",
                outputs.size(), pipes.size(), inputs.size()));

        assertPossible();

        Map<String, ReplayPart> replays = new HashMap<>();

        // Read inputs
        int i = 1;
        for (Map.Entry<String, String> entry : inputs.entrySet()) {
            System.out.print(String.format("Reading input %d of %d...", i, inputs.size()));
            long time = System.nanoTime();
            try (FileInputStream in = new FileInputStream(new File(entry.getValue()))) {
                Replay replay = studio.createReplay(in, entry.getKey().charAt(0) == 'r');
                replays.put(entry.getKey(), replay);
            } catch (Throwable t) {
                System.out.println("Exception while reading input from " + entry.getValue());
                t.printStackTrace();
                throw new CriticalException(3);
            }
            time = System.nanoTime() - time;
            System.out.println(" done after " + time + "ns");
        }

        // Process
        i = 1;
        long total = instructions.size();
        OUTER:
        while (!replays.keySet().containsAll(outputs.keySet())) {
            for (Instruction instruction : instructions) {
                if (replays.keySet().containsAll(instruction.getInputs())) {
                    System.out.print(String.format("Processing instruction %d of %d: ", i++, total));
                    System.out.print(StringUtils.join(instruction.getInputs(), ","));
                    System.out.print(" -> " + instruction + " -> ");
                    System.out.print(StringUtils.join(instruction.getOutputs(), ","));
                    long time = System.nanoTime();
                    try {
                        List<ReplayPart> in = new ArrayList<>();
                        List<ReplayPart> out = new ArrayList<>();

                        for (String inName : instruction.getInputs()) {
                            in.add(replays.get(inName));
                        }

                        instruction.perform(studio, in, out);

                        int j = 0;
                        for (String outName : instruction.getOutputs()) {
                            replays.put(outName, out.get(j++));
                        }
                    } catch (Throwable t) {
                        System.out.println("Exception while processing " + instruction);
                        t.printStackTrace();
                        throw new CriticalException(4);
                    }
                    time = System.nanoTime() - time;
                    System.out.println(" done after " + time + "ns");
                    for (String out : instruction.getOutputs()) {
                        ReplayPart part = replays.get(out).copy();
                        System.out.println("Got " + out + " of length " + part.length() + " (" + part.size() + " packets)");
                    }
                    instructions.remove(instruction);
                    continue OUTER;
                }
            }
            System.out.println("Cannot run all instructions with active config!");
            System.out.println("Instructions missing: " + StringUtils.join(instructions, ','));
            throw new CriticalException(5);
        }

        // Write outputs
        i = 1;
        for (Map.Entry<String, String> entry : outputs.entrySet()) {
            System.out.print(String.format("Writing output %d of %d...", i, outputs.size()));
            long time = System.nanoTime();
            try {
                if (entry.getValue().equals("x")) {
                    System.out.print("discarded...");
                } else {
                    ReplayPart replayPart = replays.get(entry.getKey());
                    Replay replay = replayPart instanceof Replay ? (Replay) replayPart : studio.createReplay(replayPart);
                    // TODO: MetaData
                    replay.save(new File(entry.getValue()));
                }
            } catch (Throwable t) {
                System.out.println("Exception while writing output to " + entry.getValue());
                t.printStackTrace();
                throw new CriticalException(6);
            }
            time = System.nanoTime() - time;
            System.out.println(" done after " + time + "ns");
        }
    }

    private void assertPossible() {
        List<Instruction> instructions = new ArrayList<>(this.instructions);
        Set<String> replays = new HashSet<>(inputs.keySet());
        OUTER:
        while (!replays.containsAll(outputs.keySet())) {
            for (Instruction instruction : instructions) {
                if (replays.containsAll(instruction.getInputs())) {
                    replays.addAll(instruction.getOutputs());
                    instructions.remove(instruction);
                    continue OUTER;
                }
            }
            System.out.println("Cannot run all instructions with active config!");
            System.out.println("Instructions missing: " + StringUtils.join(instructions, ','));
            throw new CriticalException(5);
        }
    }

    public void parseConfig(Studio studio, String line, String[] args) {
        String ios = line.substring(1, line.indexOf(')'));
        String instructions = line.substring(line.indexOf(')') + 2, line.length()-1);

        int arg = 0;
        try {
            for (String ioi : ios.split(",")) {
                String io = ioi.substring(1);
                if (ioi.charAt(0) == '<') {
                    inputs.put(io, args[arg++]);
                } else if (ioi.charAt(0) == '>') {
                    outputs.put(io, args[arg++]);
                } else if (ioi.charAt(0) == '-') {
                    pipes.add(io);
                } else {
                    throw new IllegalArgumentException("Config input/output is invalid: " + ioi);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Insufficient input/output files. Need at least " + arg);
            throw new CriticalException(7);
        }

        for (String instructionStr : instructions.split(Pattern.quote(")("))) {
            String ins = instructionStr;
            List<String> inputs = new ArrayList<>();
            List<String> outputs = new ArrayList<>();

            // Read inputs
            OUTER:
            while (true) {
                for (int i = 0; i < ins.length(); i++) {
                    char c = ins.charAt(i);
                    if (!Character.isAlphabetic(c)) {
                        inputs.add(ins.substring(0, i));
                        if (c == ',') {
                            ins = ins.substring(i + 1);
                            continue OUTER;
                        } else {
                            ins = ins.substring(i);
                            break OUTER;
                        }
                    }
                }
                throw new IllegalArgumentException("Config input/output is invalid: " + instructionStr);
            }

            // Read outputs
            OUTER:
            while (true) {
                for (int i = ins.length() - 1; i >= 0; i--) {
                    char c = ins.charAt(i);
                    if (!Character.isAlphabetic(c)) {
                        outputs.add(ins.substring(i + 1));
                        if (c == ',') {
                            ins = ins.substring(0, i);
                            continue OUTER;
                        } else {
                            ins = ins.substring(0, i+1);
                            break OUTER;
                        }
                    }
                }
                throw new IllegalArgumentException("Config instruction input/output is invalid: " + instructionStr);
            }
            Collections.reverse(outputs);

            String options = ins.length() > 1 ? ins.substring(1, ins.length()-1) : "";
            Instruction instruction;
            switch (ins.charAt(0)) {
                case '|': // split
                    Collection<Long> splitAt = Collections2.transform(Arrays.asList(options.split(",")), this::timeStampToMillis);
                    instruction = new SplitInstruction(Longs.toArray(splitAt));
                    break;
                case '&': // append
                    instruction = new AppendInstruction();
                    break;
                case '[': // squash
                    instruction = new SquashInstruction(studio);
                    break;
                case ':': // copy
                    instruction = new CopyInstruction();
                    break;
                case '>': // filter
                    String filterName;
                    JsonObject filterOptions;
                    if (options.contains(",")) {
                        String[] parts = options.split(",", 2);
                        filterName = parts[0];
                        filterOptions = new JsonParser().parse(parts[1]).getAsJsonObject();
                    } else {
                        filterName = options;
                        filterOptions = new JsonObject();
                    }
                    Filter filter = studio.loadFilter(filterName);
                    if (filter == null) {
                        throw new IllegalStateException("Filter not found: " + filterName);
                    }
                    instruction = new FilterInstruction(studio, filter, filterOptions);
                    break;
                default:
                    throw new IllegalArgumentException("Config instruction is unknown: " + instructionStr);
            }

            instruction.getInputs().addAll(inputs);
            instruction.getOutputs().addAll(outputs);

            this.instructions.add(instruction);
        }
    }

    public void parseConfig(Studio studio, JsonObject root) {
        JsonArray instructions = root.getAsJsonArray("Instructions");
        for (JsonElement e : instructions) {
            JsonObject o = e.getAsJsonObject();
            Instruction instruction;
            switch (o.get("Name").getAsString().toLowerCase()) {
                case "split":
                    if (o.get("at").isJsonArray()) {
                        List<Long> at = new ArrayList<>();
                        Iterables.addAll(at, Iterables.transform(o.getAsJsonArray("at"),
                                (e1) -> timeStampToMillis(e1.toString())));
                        instruction = new SplitInstruction(Longs.toArray(at));
                    } else {
                        instruction = new SplitInstruction(timeStampToMillis(o.get("at").toString()));
                    }
                    break;
                case "append":
                    instruction = new AppendInstruction();
                    break;
                case "squash":
                    instruction = new SquashInstruction(studio);
                    break;
                case "copy":
                    instruction = new CopyInstruction();
                    break;
                case "filter":
                    Filter filter = studio.loadFilter(o.get("Filter").toString());
                    instruction = new FilterInstruction(studio, filter, o.getAsJsonObject("Config"));
                    break;
                default:
                    System.out.println("Warning: Unrecognized instruction in json config: " + o.get("Name"));
                    continue;
            }

            JsonElement inputs = o.get("Inputs");
            if (inputs.isJsonArray()) {
                for (JsonElement e1 : inputs.getAsJsonArray()) {
                    instruction.getInputs().add(e1.getAsString());
                }
            } else {
                instruction.getInputs().add(inputs.getAsString());
            }

            JsonElement outputs = o.get("Outputs");
            if (outputs.isJsonArray()) {
                for (JsonElement e1 : outputs.getAsJsonArray()) {
                    instruction.getOutputs().add(e1.getAsString());
                }
            } else {
                instruction.getOutputs().add(outputs.getAsString());
            }

            this.instructions.add(instruction);
        }

        // Get all inputs
        JsonObject inputs = root.getAsJsonObject("Inputs");
        for (Map.Entry<String, JsonElement> e : inputs.entrySet()) {
            this.inputs.put(e.getKey(), e.getValue().getAsString());
        }

        // Get all outputs
        JsonObject outputs = root.getAsJsonObject("Outputs");
        for (Map.Entry<String, JsonElement> e : outputs.entrySet()) {
            this.outputs.put(e.getKey(), e.getValue().getAsString());
        }

        // Calculate all pipes
        for (Instruction instruction : this.instructions) {
            pipes.addAll(instruction.getInputs());
            pipes.addAll(instruction.getOutputs());
        }
        pipes.removeAll(this.inputs.keySet());
        pipes.removeAll(this.outputs.keySet());
    }

    private long timeStampToMillis(String string) {
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

    private interface Instruction {
        List<String> getInputs();
        List<String> getOutputs();
        void perform(Studio studio, List<ReplayPart> inputs, List<ReplayPart> outputs);
    }

    private static abstract class AbstractInstruction implements Instruction {
        private List<String> inputs = new ArrayList<>();
        private List<String> outputs = new ArrayList<>();

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }

        public List<String> getInputs() {
            return this.inputs;
        }

        public List<String> getOutputs() {
            return this.outputs;
        }
    }

    private static class SplitInstruction extends AbstractInstruction {
        private final long[] splitAt;
        public SplitInstruction(long...splitAt) {
            this.splitAt = splitAt;
        }

        @Override
        public void perform(Studio studio, List<ReplayPart> inputs, List<ReplayPart> outputs) {
            ReplayPart input = inputs.get(0);
            long from = 0;
            for (long to : splitAt) {
                outputs.add(input.copyOf(from, to - 1));
                from = to;
            }
            outputs.add(input.copyOf(from));
        }
    }

    private static class AppendInstruction extends AbstractInstruction {
        @Override
        public void perform(Studio studio, List<ReplayPart> inputs, List<ReplayPart> outputs) {
            ReplayPart result = null;
            for (ReplayPart input : inputs) {
                if (result == null) {
                    result = input.copy();
                } else {
                    long first = input.size() == 0 ? 0 : input.iterator().next().getTime();
                    result.addAt(result.length() - first, input);
                }
            }
            outputs.add(result);
        }
    }

    private static class SquashInstruction extends FilterInstruction {
        public SquashInstruction(Studio studio) {
            super(studio, studio.loadFilter("squash"), new JsonObject());
        }
    }

    private static class CopyInstruction extends AbstractInstruction {
        @Override
        public void perform(Studio studio, List<ReplayPart> inputs, List<ReplayPart> outputs) {
            for (ReplayPart p : inputs) {
                outputs.add(p.copy());
            }
        }
    }

    private static class FilterInstruction extends AbstractInstruction {
        private final Filter filter;

        public FilterInstruction(Studio studio, Filter filter, JsonObject config) {
            this.filter = filter;
            filter.init(studio, config);
        }

        @Override
        public void perform(Studio studio, List<ReplayPart> inputs, List<ReplayPart> outputs) {
            outputs.add(filter.apply(inputs.get(0).copy()));
        }

        @Override
        public String toString() {
            return super.toString() + " (" + filter.getName() + ")";
        }
    }

    public static class CriticalException extends RuntimeException {
        private final int returnCode;

        public CriticalException(int returnCode) {
            this.returnCode = returnCode;
        }

        public int getReturnCode() {
            return returnCode;
        }
    }
}
