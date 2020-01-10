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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Launcher {
    public static void main(String[] args) throws Exception {
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
