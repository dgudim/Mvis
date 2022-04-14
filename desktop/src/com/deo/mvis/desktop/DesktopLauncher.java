package com.deo.mvis.desktop;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.deo.mvis.Launcher;

public class DesktopLauncher {

	public static void main (String[] arg) {
		Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
		
		cfg.setTitle("Mvis");
		cfg.setWindowedMode(WIDTH, HEIGHT);

		new Lwjgl3Application(new Launcher(), cfg);
	}
}
