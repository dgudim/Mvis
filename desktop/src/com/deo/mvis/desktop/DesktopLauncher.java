package com.deo.mvis.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.deo.mvis.Launcher;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

public class DesktopLauncher {

	public static void main (String[] arg) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "Mvis";

		cfg.width = WIDTH;
		cfg.height = HEIGHT;
		cfg.fullscreen = true;
		cfg.foregroundFPS = 7680;

		new LwjglApplication(new Launcher(), cfg);
	}
}
