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
import com.google.common.io.Closeables;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.util.Utils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.google.common.io.Files.*;
import static java.nio.file.Files.*;
import static java.nio.file.Files.move;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.kinesisfirehose.model.DeliveryStreamDescription;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamRequest;
import com.amazonaws.services.kinesisfirehose.model.DescribeDeliveryStreamResult;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;

public class StreamReplayFile extends AbstractReplayFile {

    private final File input;
    private final File output;

    // Temporary folder structure
    private final File tmpFiles;
    private final File changedFiles;
    private final File removedFiles;
    private final File sourceFile;

    private final Map<String, OutputStream> outputStreams = new HashMap<>();
    private final Map<String, File> changedEntries = new HashMap<>();
    private final Set<String> removedEntries = new HashSet<>();

    private ZipFile zipFile;

    public StreamReplayFile(Studio studio, AmazonKinesisFirehose firehoseClient) throws IOException {
        super(studio);

        tmpFiles = new File(output.getParentFile(), output.getName() + ".tmp");
        changedFiles = new File(tmpFiles, "changed");
        removedFiles = new File(tmpFiles, "removed");
        sourceFile = new File(tmpFiles, "source");

        if (input != null && input.exists()) {
            this.zipFile = new ZipFile(input);
        }
    }

    /**
     * Saves the input file path to the source file in the temp folder structure.
     * @throws IOException
     */
    private void saveInputFile() throws IOException {
    }

    @Override
    public Optional<InputStream> get(String entry) throws IOException {
        return Optional.absent();
    }

    @Override
    public Map<String, InputStream> getAll(Pattern pattern) throws IOException {
        Map<String, InputStream> streams = new HashMap<>();

        for (Map.Entry<String, File> entry : changedEntries.entrySet()) {
            String name = entry.getKey();
            if (pattern.matcher(name).matches()) {
                streams.put(name, new BufferedInputStream(new FileInputStream(changedEntries.get(name))));
            }
        }

        if (zipFile != null) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (pattern.matcher(name).matches()) {
                    if (!streams.containsKey(name) && !removedEntries.contains(name)) {
                        streams.put(name, new BufferedInputStream(zipFile.getInputStream(entry)));
                    }
                }
            }
        }

        return streams;
    }

    @Override
    public OutputStream write(String entry) throws IOException {
        saveInputFile();
        File file = changedEntries.get(entry);
        if (file == null) {
            file = new File(changedFiles, entry);
            createParentDirs(file);
            changedEntries.put(entry, file);
        }
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        Closeables.close(outputStreams.put(entry, out), true);
        if (removedEntries.remove(entry)) {
            deleteIfExists(new File(removedFiles, entry).toPath());
        }
        return out;
    }

    @Override
    public void remove(String entry) throws IOException {
    }

    @Override
    public void save() throws IOException {
        if (zipFile != null && changedEntries.isEmpty() && removedEntries.isEmpty()) {
            return; // No changes, no need to save
        }
        File outputFile = createTempFile("replaystudio", "replayfile").toFile();
        saveTo(outputFile);
        close();
        if (output.exists()) {
            delete(output);
        }
        move(outputFile.toPath(), output.toPath());
        zipFile = new ZipFile(output);
    }

    @Override
    public void saveTo(File target) throws IOException {
        for (OutputStream out : outputStreams.values()) {
            Closeables.close(out, false);
        }
        outputStreams.clear();

        try (ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)))) {
            if (zipFile != null) {
                for (ZipEntry entry : Collections.list(zipFile.entries())) {
                    if (!changedEntries.containsKey(entry.getName()) && !removedEntries.contains(entry.getName())) {
                        out.putNextEntry(entry);
                        Utils.copy(zipFile.getInputStream(entry), out);
                    }
                }
            }
            for (Map.Entry<String, File> e : changedEntries.entrySet()) {
                out.putNextEntry(new ZipEntry(e.getKey()));
                Utils.copy(new BufferedInputStream(new FileInputStream(e.getValue())), out);
            }
        }
    }

    @Override
    public void close() throws IOException {
        // TODO Send MC server return firehose key command

        if (zipFile != null) {
            zipFile.close();
        }
        for (OutputStream out : outputStreams.values()) {
            Closeables.close(out, true);
        }
        outputStreams.clear();

        changedEntries.clear();
        removedEntries.clear();
        delete(tmpFiles);
    }

    private void delete(File file) throws IOException {
    }
}
