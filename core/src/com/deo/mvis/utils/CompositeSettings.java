package com.deo.mvis.utils;

import com.badlogic.gdx.utils.Array;

public class CompositeSettings {
    
    private final Array<SettingsEntry> mainSettings;
    private final Array<String> paletteNames;
    private final Array<String> modeNames;
    
    public CompositeSettings(Array<String> paletteNames, Array<String> modeNames){
        mainSettings = new Array<>();
        this.paletteNames = paletteNames;
        this.modeNames = modeNames;
    }
    
    public Array<String> getModeNames() {
        return modeNames;
    }
    
    public Array<String> getPaletteNames() {
        return paletteNames;
    }
    
    public void addSetting(String name, float min, float max, float def, Type type){
        mainSettings.add(new SettingsEntry(name, min, max, def, type));
    }
    
    public Array<SettingsEntry> getMainSettings() {
        return mainSettings;
    }
}
