package com.deo.golly.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.deo.golly.BaseEngine;
import com.deo.golly.MainScreen;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "game of life";

		cfg.width = MainScreen.WIDTH;
		cfg.height = MainScreen.HEIGHT;
		cfg.fullscreen = true;
		cfg.foregroundFPS = 7680;

		cfg.samples = 0;
		new LwjglApplication(new BaseEngine(), cfg);
	}
}
