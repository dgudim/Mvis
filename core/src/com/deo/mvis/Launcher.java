package com.deo.mvis;
import com.badlogic.gdx.Game;
import com.deo.mvis.visualisers.FourierScreen;

public final class Launcher extends Game {

    public static int WIDTH = 1600;
    public static int HEIGHT = 900;

    @Override
    public void create() {
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}