package de.johni0702.replaystudio.api.manipulation;

import com.google.gson.JsonObject;
import de.johni0702.replaystudio.api.ReplayPart;
import de.johni0702.replaystudio.api.Studio;

/**
 * A manipulation that applies some effect on a supplied replay part and returns the resulting replay part.
 */
public interface Filter {

    /**
     * Initializes this filter.
     * Read the configuration of this filter from the supplied json.
     * This can be called multiple times.
     */
    void init(Studio studio, JsonObject config);

    /**
     * Apply this manipulation on the specified replay part and returns the result.
     * Unless the result is a copy (which should be avoided) this should return the same {@code part}.
     * @param part The part on which to apply this manipulation
     * @return The result
     */
    ReplayPart apply(ReplayPart part);

}
