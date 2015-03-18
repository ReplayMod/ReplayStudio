package de.johni0702.replaystudio.collection;

/**
 * Provides a view on another replay part.
 * Changes to the view will be reflected on the parent and vice versa.
 */
public interface ReplayPartView extends ReplayPart {

    /**
     * Get the part to which this provides a view.
     * @return The viewed part
     */
    ReplayPart getViewed();

    /**
     * Returns the time in the parent at which this view starts.
     * @return Time in milliseconds
     */
    long getFrom();

    /**
     * Returns the time in the parent at which this view ends.
     * @return Time in milliseconds
     */
    long getTo();

}
