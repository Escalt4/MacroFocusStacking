package ru.example.macrofocusstacking.Database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import ru.example.macrofocusstacking.Database.Model.RawImage;

@Dao
public interface MacroFocusStackingDao {
    @Insert
    void insertRawImage(RawImage rawImage);

    @Query("SELECT COALESCE(MAX(id)+1,0) FROM RawImage")
    Long getNextRawImageId();

    @Query("SELECT * FROM RawImage")
    RawImage getAllRawImage();

    @Query("DELETE FROM RawImage")
    public void deleteAllRawImage();
}