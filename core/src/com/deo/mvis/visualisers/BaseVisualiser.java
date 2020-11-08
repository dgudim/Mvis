package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.MenuScreen;
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.UIComposer;
import com.deo.mvis.utils.Utils;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

public class BaseVisualiser {

    public Utils utils;
    public ScreenViewport viewport;
    public OrthographicCamera camera;

    public ShapeRenderer renderer;
    public SpriteBatch batch;

    final int FPS = 30;
    public final int step = 44100 / FPS;
    public final boolean render = false;
    public int frame;
    public int recorderFrame;

    MusicWave musicWave;
    public Music music;

    private boolean musicStarted = false;

    float[] samplesRaw;
    public float[] samplesSmoothed;

    float[] rSamplesNormalised;
    float[] lSamplesNormalised;

    float[] rSamplesNormalisedSmoothed;
    float[] lSamplesNormalisedSmoothed;

    public static String[] typeNames, paletteNames, settings, settingTypes;
    public static float[] settingMaxValues, settingMinValues, defaultSettings;

    int numOfSamples;

    public static FileHandle musicFile = Gdx.files.internal("away.wav");

    private TextButton exit;
    public Stage stage;
    private float transparency = 1;

    private AssetManager assetManager;

    public BaseVisualiser(final Game game) {

        camera = new OrthographicCamera(1600, 900);
        viewport = new ScreenViewport(camera);

        assetManager = new AssetManager();

        assetManager.load("menuButtons.atlas", TextureAtlas.class);
        assetManager.load("font2.fnt", BitmapFont.class);
        assetManager.load("font2(old).fnt", BitmapFont.class);

        while (!assetManager.isFinished()) {
            assetManager.update();
        }

        UIComposer uiComposer = new UIComposer(assetManager);
        uiComposer.loadStyles("defaultLight");
        exit = uiComposer.addTextButton("defaultLight", "exit", 0.55f);
        exit.setColor(Color.LIGHT_GRAY);
        exit.setBounds(-WIDTH / 2f, -HEIGHT / 2f, 300, 300);

        exit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
                dispose();
            }
        });

        stage = new Stage(viewport);
        stage.addActor(exit);

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        batch = new SpriteBatch();

        musicWave = new MusicWave(musicFile);
        music = musicWave.getMusic();

        samplesRaw = musicWave.normaliseSamples(false, false, musicWave.getSamples().clone());
        samplesSmoothed = musicWave.smoothSamples(musicWave.getSamples().clone(), 2, 32);

        rSamplesNormalised = musicWave.normaliseSamples(false, false, musicWave.getRightChannelSamples());
        lSamplesNormalised = musicWave.normaliseSamples(false, false, musicWave.getLeftChannelSamples());

        rSamplesNormalisedSmoothed = musicWave.smoothSamples(musicWave.getRightChannelSamples().clone(), 2, 32);
        lSamplesNormalisedSmoothed = musicWave.smoothSamples(musicWave.getLeftChannelSamples().clone(), 2, 32);

        numOfSamples = samplesRaw.length;

        utils = new Utils(FPS, step, samplesSmoothed, 3, 1, 1, true, batch);

        if (!render) {
            music.setOnCompletionListener(new Music.OnCompletionListener() {
                @Override
                public void onCompletion(Music music) {
                    music.setPosition(0);
                    music.play();
                }
            });
        }

    }

    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    public void drawExitButton() {

        if(!musicStarted && transparency == 0){
            music.play();
            musicStarted = true;
        }

        exit.setColor(1, 1, 1, transparency);
        stage.draw();
        stage.act();
        transparency = MathUtils.clamp(transparency - 0.01f, 0, 1);
    }

    public void resize(int width, int height, int yOffset, boolean maxScale) {
        viewport.update(width, height);
        camera.position.set(0, yOffset, 0);
        float tempScaleH = height / 900f;
        float tempScaleW = width / 1600f;
        float zoom = Math.min(tempScaleH, tempScaleW);
        float additionalZoom = 0.5f;
        if (maxScale) {
            additionalZoom = 0;
        }
        camera.zoom = 1 / zoom + additionalZoom;
        camera.update();
    }

    public void dispose() {
        renderer.dispose();
        utils.dispose();
        musicWave.dispose();
        batch.dispose();
        assetManager.dispose();
        samplesRaw = null;
        samplesSmoothed = null;
        lSamplesNormalised = null;
        rSamplesNormalised = null;
        lSamplesNormalisedSmoothed = null;
        rSamplesNormalisedSmoothed = null;
        System.gc();
    }

    public static String[] getSettings() {
        return settings;
    }

    public static String[] getSettingTypes() {
        return settingTypes;
    }

    public static String[] getTypeNames() {
        return typeNames;
    }

    public static String[] getPaletteNames() {
        return paletteNames;
    }

    public static float[] getSettingMaxValues() {
        return settingMaxValues;
    }

    public static float[] getSettingMinValues() {
        return settingMinValues;
    }

    public static float[] getDefaultSettings() {
        return defaultSettings;
    }

    public static void setMusic(FileHandle fileHandle) {
        musicFile = fileHandle;
    }

}
