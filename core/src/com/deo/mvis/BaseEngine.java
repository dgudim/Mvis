package com.deo.mvis;
import com.badlogic.gdx.Game;

public final class BaseEngine extends Game {

    public static int WIDTH = 1600;
    public static int HEIGHT = 900;

    @Override
    public void create() {
        this.setScreen(new FFTScreen());
    }
}