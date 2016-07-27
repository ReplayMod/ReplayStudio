package com.replaymod.replaystudio.filter;

import com.google.gson.JsonObject;
import com.replaymod.replaystudio.Studio;
import com.replaymod.replaystudio.collection.ReplayPart;

/**
 * A manipulation that applies some effect on a supplied replay part and returns the resulting replay part.
 */
public interface Filter {

    /**
     * Returns a unique but simple name for this filter. This name is used when referring to the filter
     * in configs and in {@link Studio#loadFilter(String)}.
     * It may not contain whitespace or special characters except underscores.
     * It should be all lowercase, however this is not a requirement.
     * @return Name of this filter
     */
    String getName();

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
