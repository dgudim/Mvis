package com.deo.mvis.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.deo.mvis.BaseEngine;
import com.deo.mvis.MainScreen;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "Mvis";

		cfg.width = MainScreen.WIDTH;
		cfg.height = MainScreen.HEIGHT;
		cfg.fullscreen = true;
		cfg.foregroundFPS = 7680;

		cfg.samples = 0;
		new LwjglApplication(new BaseEngine(), cfg);
	}
}
