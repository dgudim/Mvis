package com.deo.mvis.utils;

public class Setting {
    public float value;
    public String name;
    float maxValue;
    float minValue;
    float defaultValue;
    String type;

    public Setting(String name, String type, float minValue, float maxValue, float defaultValue){
        this.name = name;
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.defaultValue = defaultValue;
        value = defaultValue;
    }

    @Override
    public String toString() {
        return name + ":" + value;
    }
}
