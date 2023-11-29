package ru.example.macrofocusstacking;

import android.app.Application;

import androidx.room.Room;

import ru.example.macrofocusstacking.Database.MacroFocusStackingDatabase;


public class App extends Application {
    MacroFocusStackingDatabase macroFocusStackingDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public MacroFocusStackingDatabase getMacroFocusStackingDatabase() {
        if (macroFocusStackingDatabase == null) {
            macroFocusStackingDatabase = Room.databaseBuilder(this, MacroFocusStackingDatabase.class, "MacroFocusStackingDatabase").fallbackToDestructiveMigration().build();
        }

        return macroFocusStackingDatabase;
    }
}