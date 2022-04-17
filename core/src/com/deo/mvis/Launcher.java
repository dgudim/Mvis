package com.deo.mvis;
import com.badlogic.gdx.Game;

public final class Launcher extends Game {
    
    public static int WIDTH;
    public static int HEIGHT;
    
    @Override
    public void create() {
        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}