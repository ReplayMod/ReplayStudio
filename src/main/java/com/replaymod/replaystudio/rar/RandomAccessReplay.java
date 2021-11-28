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

package com.replaymod.replaystudio.rar;

import com.github.steveice10.netty.buffer.ByteBuf;
import com.github.steveice10.netty.buffer.Unpooled;
import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.io.stream.StreamNetInput;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import com.replaymod.replaystudio.lib.guava.base.Optional;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.rar.analyse.ReplayAnalyzer;
import com.replaymod.replaystudio.rar.cache.ReadableCache;
import com.replaymod.replaystudio.rar.cache.WriteableCache;
import com.replaymod.replaystudio.rar.state.Replay;
import com.replaymod.replaystudio.replay.ReplayFile;
import org.apache.commons.lang3.tuple.Pair;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows random access (i.e. very quick backwards and forwards seeking) to a replay. This is what powers the Quick Mode
 * in the Replay Mod.
 * Supports 1.9+ only.
 *
 * To do so, it performs an initial analysis of the replay, scanning all of its packets and storing entity positions
 * and chunk states while doing so.
 * This allows it to later jump to any time by doing a diff from the current time (including backwards jumping).
 *
 * Exactly replicating the input replay is not realistically doable (and much less so if you consider doing to for
 * all versions supported), as such only entity positions, chunk state, world time and weather will be replicated.
 * This is by design and any further additions should be carefully considered as it'll probably cause significant
 * maintenance work in the future.
 */
public abstract class RandomAccessReplay {
    private static final String CACHE_ENTRY = "quickModeCache.bin";
    private static final String CACHE_INDEX_ENTRY = "quickModeCacheIndex.bin";
    private static final int CACHE_VERSION = 7;
    private static final Logger LOGGER = Logger.getLogger(RandomAccessReplay.class.getName());

    private final ReplayFile replayFile;
    private final PacketTypeRegistry registry;

    private int currentTimeStamp;

    private Replay state;
    private ReadableCache cache;

    public RandomAccessReplay(ReplayFile replayFile, PacketTypeRegistry registry) {
        this.replayFile = replayFile;
        this.registry = registry;
    }

    protected abstract void dispatch(Packet packet);

    public void load(Consumer<Double> progress) throws IOException {
        if (!tryLoadFromCache(progress)) {
            double progressSplit = 0.9; // 90% of progress time for analysing, 10% for loading
            analyseReplay(d -> progress.accept(d * progressSplit));
            tryLoadFromCache(d -> progress.accept(d * (1 - progressSplit) + progressSplit));
        }
    }

    private boolean tryLoadFromCache(Consumer<Double> progress) throws IOException {
        release();

        Optional<InputStream> cacheIndexOpt = replayFile.getCache(CACHE_INDEX_ENTRY);
        if (!cacheIndexOpt.isPresent()) return false;
        try (InputStream indexIn = cacheIndexOpt.get()) {
            Optional<InputStream> cacheOpt = replayFile.getCache(CACHE_ENTRY);
            if (!cacheOpt.isPresent()) return false;
            try (InputStream cacheIn = cacheOpt.get()) {
                Pair<Replay, ReadableCache> result = loadFromCache(cacheIn, indexIn, progress);
                if (result == null) return false;
                Replay replay = result.getLeft();
                ReadableCache cache = result.getRight();
                replay.load(Packet::release, cache);
                this.state = replay;
                this.cache = cache;
                return true;
            }
        } catch (EOFException e) {
            LOGGER.log(Level.WARNING, "Re-analysing replay due to premature EOF while loading the cache:", e);
            return false;
        }
    }

    private Pair<Replay, ReadableCache> loadFromCache(InputStream rawCacheIn, InputStream rawIndexIn, Consumer<Double> progress) throws IOException {
        long sysTimeStart = System.currentTimeMillis();

        NetInput cacheIn = new StreamNetInput(rawCacheIn);
        NetInput in = new StreamNetInput(rawIndexIn);
        if (in.readVarInt() != CACHE_VERSION) return null; // Incompatible cache version
        if (cacheIn.readVarInt() != CACHE_VERSION) return null; // Incompatible cache version
        if (in.readVarInt() != registry.getVersion().getOriginalVersion()) return null; // Cache of incompatible protocol version
        if (cacheIn.readVarInt() != registry.getVersion().getOriginalVersion()) return null; // Cache of incompatible protocol version

        Replay replay = new Replay(registry, in);

        int size = in.readVarInt();
        LOGGER.info("Creating quick mode buffer of size: " + size / 1024 + "KB");
        ByteBuf buf = Unpooled.buffer(size);
        int read = 0;
        while (true) {
            int len = buf.writeBytes(rawCacheIn, Math.min(size - read, 4096));
            if (len <= 0) break;
            read += len;
            progress.accept((double) read / size);
        }
        ReadableCache cache = new ReadableCache(buf);

        LOGGER.info("Loaded quick replay from cache in " + (System.currentTimeMillis() - sysTimeStart) + "ms");
        return Pair.of(replay, cache);
    }

    private void analyseReplay(Consumer<Double> progress) throws IOException {
        double sysTimeStart = System.currentTimeMillis();
        try (ReplayInputStream in = replayFile.getPacketData(registry);
             OutputStream cacheOut = replayFile.writeCache(CACHE_ENTRY);
             OutputStream cacheIndexOut = replayFile.writeCache(CACHE_INDEX_ENTRY)) {
            NetOutput out = new StreamNetOutput(cacheOut);
            out.writeVarInt(CACHE_VERSION);
            out.writeVarInt(registry.getVersion().getOriginalVersion());
            NetOutput indexOut = new StreamNetOutput(cacheIndexOut);
            indexOut.writeVarInt(CACHE_VERSION);
            indexOut.writeVarInt(registry.getVersion().getOriginalVersion());

            WriteableCache cache = new WriteableCache(cacheOut);

            double duration = replayFile.getMetaData().getDuration();
            new ReplayAnalyzer(registry, indexOut, cache)
                    .analyse(in, time -> progress.accept(time / duration));

            indexOut.writeVarInt(cache.index()); // store size of cache
        }
        LOGGER.info("Analysed replay in " + (System.currentTimeMillis() - sysTimeStart) + "ms");
    }

    public void release() {
        if (state != null && cache != null) {
            try {
                state.unload(Packet::release, cache);
            } catch (IOException e) {
                e.printStackTrace();
            }
            state = null;

            cache.release();
            cache = null;
        }
    }

    public void reset() {
        currentTimeStamp = -1;
    }

    public void seek(int targetTime) throws IOException {
        if (targetTime > currentTimeStamp) {
            state.play(this::dispatch, currentTimeStamp, targetTime);
        } else {
            state.rewind(this::dispatch, currentTimeStamp, targetTime);
        }
        currentTimeStamp = targetTime;
    }
}
