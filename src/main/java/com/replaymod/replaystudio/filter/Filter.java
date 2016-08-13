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
