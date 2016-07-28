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

public class ZipReplayFile extends AbstractReplayFile {

    private final File input;
    private final File output;

    // Temporary folder structure
    private final File tmpFiles;
    private final File changedFiles;
    private final File removedFiles;
    private final File sourceFile;

    /**
     * Whether the input file path should be written to the tmp folder on next write.
     */
    private boolean shouldSaveInputFile;

    private final Map<String, OutputStream> outputStreams = new HashMap<>();
    private final Map<String, File> changedEntries = new HashMap<>();
    private final Set<String> removedEntries = new HashSet<>();

    private ZipFile zipFile;

    public ZipReplayFile(Studio studio, File file) throws IOException {
        this(studio, file, file);
    }

    public ZipReplayFile(Studio studio, File input, File output) throws IOException {
        super(studio);

        tmpFiles = new File(output.getParentFile(), output.getName() + ".tmp");
        changedFiles = new File(tmpFiles, "changed");
        removedFiles = new File(tmpFiles, "removed");
        sourceFile = new File(tmpFiles, "source");

        if (input != null && input.exists()) {
            // Save input file path in case of crash
            shouldSaveInputFile = true;
        } else if (input == null && sourceFile.exists()) {
            // Recover input file
            input = new File(new String(readAllBytes(sourceFile.toPath()), Charsets.UTF_8));
            if (!input.exists()) {
                throw new IOException("Recovered source file no longer exists.");
            }
        }

        this.output = output;
        this.input = input;

        if (input != null && input.exists()) {
            this.zipFile = new ZipFile(input);
        }

        // Try to restore any changes if we weren't able to save them last time
        if (changedFiles.exists()) {
            fileTreeTraverser()
                    .breadthFirstTraversal(changedFiles)
                    .filter(isFile())
                    .forEach(f -> changedEntries.put(changedFiles.toURI().relativize(f.toURI()).getPath(), f));
        }
        if (removedFiles.exists()) {
            fileTreeTraverser()
                    .breadthFirstTraversal(removedFiles)
                    .filter(isFile())
                    .transform(f -> removedFiles.toURI().relativize(f.toURI()).getPath())
                    .forEach(removedEntries::add);
        }
    }

    /**
     * Saves the input file path to the source file in the temp folder structure.
     * @throws IOException
     */
    private void saveInputFile() throws IOException {
        if (shouldSaveInputFile) {
            createParentDirs(sourceFile);
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(sourceFile))) {
                out.write(input.getCanonicalPath().getBytes(Charsets.UTF_8));
            }
            shouldSaveInputFile = false;
        }
    }

    @Override
    public Optional<InputStream> get(String entry) throws IOException {
        if (changedEntries.containsKey(entry)) {
            return Optional.of(new BufferedInputStream(new FileInputStream(changedEntries.get(entry))));
        }
        if (zipFile == null || removedEntries.contains(entry)) {
            return Optional.absent();
        }
        ZipEntry zipEntry = zipFile.getEntry(entry);
        if (zipEntry == null) {
            return Optional.absent();
        }
        return Optional.of(new BufferedInputStream(zipFile.getInputStream(zipEntry)));
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
        saveInputFile();
        Closeables.close(outputStreams.remove(entry), true);
        File file = changedEntries.remove(entry);
        if (file != null && file.exists()) {
            delete(file);
        }
        removedEntries.add(entry);
        File removedFile = new File(removedFiles, entry);
        createParentDirs(removedFile);
        touch(removedFile);
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
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        Files.deleteIfExists(file.toPath());
    }
}
