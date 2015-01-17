package de.johni0702.replaystudio.api;

import lombok.Data;

/**
 * Meta data for replay files.
 */
@Data
public class ReplayMetaData {

    /**
     * Whether this is a singleplayer recording.
     */
    private boolean singleplayer;

    /**
     * The server address or the singleplayer world name.
     */
    private String serverName;

    /**
     * Duration of the replay in milliseconds.
     */
    private int duration;

    /**
     * Unix timestamp of when the recording was started in milliseconds.
     */
    private long date;

    /**
     * File format. Defaults to 'MCPR'
     */
    private String fileFormat;

    /**
     * Version of the file format.
     */
    private int fileFormatVersion;

}
