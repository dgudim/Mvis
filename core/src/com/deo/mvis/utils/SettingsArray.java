package com.deo.mvis.utils;

import com.badlogic.gdx.utils.Array;

public class SettingsArray extends Array<Setting> {

    Array<String> names;

    public SettingsArray() {
        super();
        names = new Array<>();
    }

    @Override
    public void add(Setting value) {
        super.add(value);
        names.add(value.name);
    }

    public float getSettingByName(String name) {
        return get(names.indexOf(name, false)).value;
    }
}
