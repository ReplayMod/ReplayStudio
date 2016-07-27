package com.replaymod.replaystudio.filter;

import com.replaymod.replaystudio.collection.ReplayPart;
import com.replaymod.replaystudio.stream.IteratorStream;

/**
 * Base class for stream filter which also want export their functionality as a regular filter.
 */
public abstract class StreamFilterBase implements StreamFilter, Filter {

    @Override
    public ReplayPart apply(ReplayPart part) {
        new IteratorStream(part.iterator(), this).processAll();
        return part;
    }

}
