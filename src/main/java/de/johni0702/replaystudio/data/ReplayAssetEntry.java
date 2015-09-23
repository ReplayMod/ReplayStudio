package de.johni0702.replaystudio.data;

import java.util.UUID;

public class ReplayAssetEntry {
    private final UUID uuid;
    private final String fileExtension;
    private String name;

    public ReplayAssetEntry(UUID uuid, String fileExtension) {
        this.uuid = uuid;
        this.fileExtension = fileExtension;
    }

    public ReplayAssetEntry(UUID uuid, String fileExtension, String name) {
        this.uuid = uuid;
        this.fileExtension = fileExtension;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReplayAssetEntry that = (ReplayAssetEntry) o;

        return uuid.equals(that.uuid);

    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "ReplayAssetEntry{" +
                "uuid=" + uuid +
                ", fileExtension='" + fileExtension + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
