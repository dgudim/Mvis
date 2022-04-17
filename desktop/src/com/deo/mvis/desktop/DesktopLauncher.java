package com.deo.mvis.desktop;

import static com.deo.mvis.Launcher.WIDTH;
import static com.deo.mvis.Launcher.HEIGHT;

import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.deo.mvis.Launcher;

public class DesktopLauncher {
	
	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
		
		Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
		
		cfg.setTitle("Mvis");
		cfg.setFullscreenMode(displayMode);
		cfg.setForegroundFPS(60);
		
		WIDTH = displayMode.width;
		HEIGHT = displayMode.height;
		
		new Lwjgl3Application(new Launcher(), cfg);
	}
}
