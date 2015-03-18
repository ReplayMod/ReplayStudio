package de.johni0702.replaystudio.filter;

import de.johni0702.replaystudio.collection.ReplayPart;
import de.johni0702.replaystudio.stream.IteratorStream;

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
