package com.deo.mvis;
import com.badlogic.gdx.Game;

public final class BaseEngine extends Game {

    @Override
    public void create() {
        this.setScreen(new OsciloscopeScreen());
    }
}