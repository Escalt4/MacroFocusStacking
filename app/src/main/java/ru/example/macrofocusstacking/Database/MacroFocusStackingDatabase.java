package ru.example.macrofocusstacking.Database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import ru.example.macrofocusstacking.Database.Model.RawImage;


@Database(entities = {RawImage.class}, version = 1)
public abstract class MacroFocusStackingDatabase extends RoomDatabase {
    public abstract MacroFocusStackingDao macroFocusStackingDao();
}