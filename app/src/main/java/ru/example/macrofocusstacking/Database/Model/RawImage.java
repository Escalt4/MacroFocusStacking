package ru.example.macrofocusstacking.Database.Model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class RawImage {
    @PrimaryKey
    @NonNull
    public Long id;

    @NonNull
    public String rawImageKey;

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] rawImageData;

    public RawImage() {
    }

    public RawImage(@NonNull Long id, @NonNull String rawImageKey, byte[] rawImageData) {
        this.id = id;
        this.rawImageKey = rawImageKey;
        this.rawImageData = rawImageData;
    }

    @NonNull
    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    @NonNull
    public String getRawImageKey() {
        return rawImageKey;
    }

    public void setRawImageKey(@NonNull String rawImageKey) {
        this.rawImageKey = rawImageKey;
    }

    public byte[] getRawImageData() {
        return rawImageData;
    }

    public void setRawImageData(byte[] rawImageData) {
        this.rawImageData = rawImageData;
    }
}

