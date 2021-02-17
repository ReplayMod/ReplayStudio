/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.launcher;

import com.replaymod.replaystudio.util.ThreadLocalOutputStream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for launching a daemon which listens on port 4002 (specified through env var {@code replaystudio.port}) for
 * requests and executes these.
 * No authentication is performed!
 */
public class DaemonLauncher {
    private static final int PORT = Integer.parseInt(System.getProperty("replaystudio.port", "4002"));

    private ExecutorService worker;
    private ThreadLocalOutputStream systemOut;

    public void launch(CommandLine cmd) throws Exception {
        int threads = Integer.parseInt(cmd.getOptionValue('d', "" + Runtime.getRuntime().availableProcessors()));
        worker = Executors.newFixedThreadPool(threads);

        System.setOut(new PrintStream(systemOut = new ThreadLocalOutputStream(System.out)));
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Daemon started on port " + PORT + " with " + threads + " worker threads.");
        while (!Thread.interrupted()) {
            Socket socket = serverSocket.accept();
            try {
                Client client = new Client(socket);
                new Thread(client).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Client implements Runnable {
        private final Socket socket;
        private final BufferedReader in;
        private final DataOutputStream out;

        public Client(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        @SuppressWarnings("deprecation")
        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    String command = in.readLine();
                    out.write(0);
                    out.write(0);
                    Future future = worker.submit(() -> {
                        System.out.println("[" + Thread.currentThread().getName() + "] Running: " + command);
                        systemOut.setOutput(out);
                        List<String> parts = new ArrayList<>();
                        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command);
                        while (m.find())
                            parts.add(m.group(1).replace("\"", ""));

                        try {
                            out.write(0);
                            out.write(1);
                            Launcher.run(parts.toArray(new String[parts.size()]));
                        } catch (Exception e) {
                            e.printStackTrace();
                            try {
                                out.write(0);
                                out.write(3);
                                out.writeUTF(ExceptionUtils.getStackTrace(e));
                                out.close();
                                socket.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        systemOut.setOutput(systemOut.getDefault());
                        System.out.println("[" + Thread.currentThread().getName() + "] Done: " + command);
                    });
                    try {
                        future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    if (!socket.isClosed()) {
                        out.write(0);
                        out.write(2);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (!socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
