package com.deo.mvis.utils;

public class SettingsEntry {
    
    private final String name;
    private final float min;
    private final float max;
    private final float default_val;
    private float current_val;
    private final Type type;
    
    public SettingsEntry(String name, float min, float max, float def, Type type) {
        this.name = name;
        this.min = min;
        this.max = max;
        default_val = def;
        current_val = default_val;
        this.type = type;
    }
    
    public void setValue(float value) {
        current_val = value;
    }
    
    public String getName() {
        return name;
    }
    
    public float getMin() {
        return min;
    }
    
    public float getMax() {
        return max;
    }
    
    public float getDefault() {
        return default_val;
    }
    
    public float getCurrent() {
        return current_val;
    }
    
    public Type getType() {
        return type;
    }
}
