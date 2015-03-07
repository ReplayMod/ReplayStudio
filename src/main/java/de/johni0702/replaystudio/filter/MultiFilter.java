package de.johni0702.replaystudio.filter;

import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.manipulation.Filter;
import de.johni0702.replaystudio.api.manipulation.StreamFilter;
import de.johni0702.replaystudio.api.packet.IteratorStream;

/**
 * Base class for stream filter which also export their functionality as a regular filter.
 */
public abstract class MultiFilter implements StreamFilter, Filter {

    @Override
    public ReplayPart apply(ReplayPart part) {
        IteratorStream stream = new IteratorStream(part.iterator());
        stream.addFilter(this);
        stream.processAll();
        return part;
    }

}
