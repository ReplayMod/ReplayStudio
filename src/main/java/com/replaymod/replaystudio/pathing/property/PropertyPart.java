package com.replaymod.replaystudio.pathing.property;

/**
 * Represents an (optionally) interpolatable part of a property (such as the X coordinate of the position property).
 * @param <T> Type of the property
 */
public interface PropertyPart<T> {
    Property<T> getProperty();

    /**
     * Returns whether this part should be interpolated between keyframes.
     * Examples would be x/y/z coordinates or sun location.
     * Counterexamples are spectated entity id or
     * @return {@code true} if this part should be interpolated, {@code false} otherwise
     */
    boolean isInterpolatable();

    /**
     * Returns the upper bound (exclusive) of the range of this part, the lower bound is always 0 (inclusive).<br>
     * If {@link Double#NaN} is returned, no range restrictions are imposed on this part.<br>
     * If the range is well defined (> 0), then this part is treated as a cyclic value which wraps around.
     * Cyclic values will be interpolated differently than normal values as there might be a shortcut by passing
     * over the bounds.<br>
     * Negative values are wrapped around to positive values automatically.
     * <br>
     * E.g. a value with {@code getUpperBound() == 360} when interpolated from 358 to 1 will go 358, 359, 0, 1 whereas
     * one with {@code Double.isNaN(getUpperBound())} will go 358, 357, 356, ..., 3, 2, 1
     * @return The upper bound or {@link Double#NaN}
     */
    double getUpperBound();

    /**
     * Convert this part of the value to a double for interpolation.
     * @param value The value, may be {@code null}
     * @return A double representing this value
     * @throws UnsupportedOperationException if this part is not interpolatable
     */
    double toDouble(T value);

    /**
     * Convert the specified double to this part of the value and return it combined with the value for other parts.
     * @param value Value of other parts
     * @param d Value for this part
     * @return Combined value
     * @throws UnsupportedOperationException if this part is not interpolatable
     */
    T fromDouble(T value, double d);
}
