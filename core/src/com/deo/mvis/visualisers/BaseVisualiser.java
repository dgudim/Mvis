package com.deo.mvis.visualisers;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.MenuScreen;
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.SettingsEntry;
import com.deo.mvis.utils.UIComposer;
import com.deo.mvis.utils.Utils;

public class BaseVisualiser {
    
    public Utils utils;
    public ScreenViewport viewport;
    public OrthographicCamera camera;
    
    public ShapeRenderer renderer;
    public SpriteBatch batch;
    
    final int FPS = 30;
    public final int sampleStep;
    public static boolean render = false;
    public int frame;
    public int recorderFrame;
    
    MusicWave musicWave;
    public Music music;
    
    private boolean musicStarted = false;
    
    float[] samplesForFFT;
    float[] samplesNormalizedRaw;
    public float[] samplesNormalizedSmoothed;
    
    float[] rSamplesNormalised;
    float[] lSamplesNormalised;
    
    float[] rSamplesNormalisedSmoothed;
    float[] lSamplesNormalisedSmoothed;
    
    public int sampleRate;
    
    public static FileHandle musicFile = Gdx.files.internal("away.wav");
    
    private final TextButton exit;
    public Stage stage;
    float transparency = 1;
    
    private final AssetManager assetManager;
    
    public static final byte DEFAULT = -100;
    public static final byte FFT = -1;
    public static final byte RAW = 0;
    public static final byte FFT_AND_RAW = 2;
    public static final byte LEFT_AND_RIGHT_RAW = 3;
    public static final byte LEFT_AND_RIGHT_SMOOTHED = 4;
    public static final byte LEFT_AND_RIGHT_RAW_AND_SMOOTHED = 5;
    public static final byte ALL_SAMPLES_RAW = 6;
    
    public BaseVisualiser(final Game game, final byte sampleMode) {
        
        camera = new OrthographicCamera(WIDTH, HEIGHT);
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
                music.stop();
                game.setScreen(new MenuScreen(game));
                dispose();
            }
        });
        
        stage = new Stage(viewport);
        stage.addActor(exit);
        
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        
        batch = new SpriteBatch();
        
        musicWave = new MusicWave(musicFile, sampleMode == LEFT_AND_RIGHT_RAW ||
                sampleMode == LEFT_AND_RIGHT_RAW_AND_SMOOTHED ||
                sampleMode == LEFT_AND_RIGHT_SMOOTHED ||
                sampleMode == ALL_SAMPLES_RAW);
        sampleRate = musicWave.sampleRate;
        sampleStep = sampleRate / FPS;
        music = musicWave.getMusic();
        
        if (sampleMode == FFT || sampleMode == FFT_AND_RAW) {
            samplesForFFT = musicWave.getSamples().clone();
        } else {
            samplesForFFT = musicWave.getSamples();
        }
        
        switch (sampleMode) {
            case (RAW):
            case (FFT_AND_RAW):
                samplesNormalizedRaw = musicWave.normalizeSamples(false, false, musicWave.getSamples());
                break;
            case (LEFT_AND_RIGHT_RAW):
                lSamplesNormalised = musicWave.normalizeSamples(false, false, musicWave.getLeftChannelSamples());
                rSamplesNormalised = musicWave.normalizeSamples(false, false, musicWave.getRightChannelSamples());
                break;
            case (LEFT_AND_RIGHT_SMOOTHED):
                lSamplesNormalisedSmoothed = musicWave.smoothSamples(musicWave.getLeftChannelSamples(), 2, 32, false, true);
                rSamplesNormalisedSmoothed = musicWave.smoothSamples(musicWave.getRightChannelSamples(), 2, 32, false, true);
                break;
            case (LEFT_AND_RIGHT_RAW_AND_SMOOTHED):
                lSamplesNormalised = musicWave.normalizeSamples(false, false, musicWave.getLeftChannelSamples());
                rSamplesNormalised = musicWave.normalizeSamples(false, false, musicWave.getRightChannelSamples());
                lSamplesNormalisedSmoothed = musicWave.smoothSamples(musicWave.getLeftChannelSamples().clone(), 2, 32, false, true);
                rSamplesNormalisedSmoothed = musicWave.smoothSamples(musicWave.getRightChannelSamples().clone(), 2, 32, false, true);
                break;
            case (ALL_SAMPLES_RAW):
                samplesNormalizedRaw = musicWave.normalizeSamples(false, false, musicWave.getSamples());
                lSamplesNormalised = musicWave.normalizeSamples(false, false, musicWave.getLeftChannelSamples());
                rSamplesNormalised = musicWave.normalizeSamples(false, false, musicWave.getRightChannelSamples());
                break;
        }
        if (!(sampleMode == DEFAULT)) {
            samplesNormalizedSmoothed = musicWave.smoothSamples(musicWave.getSamples().clone(), 2, 32, false, true);
        } else {
            samplesNormalizedSmoothed = musicWave.smoothSamples(musicWave.getSamples(), 2, 32, false, true);
        }
        
        utils = new Utils(FPS, sampleStep, musicWave, samplesNormalizedSmoothed, 3, 1, 1, true, batch);
        
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
    
    protected static <E extends Enum<E>> Array<String> enumToArray(Class<E> enumData) {
        Array<String> array = new Array<>();
        for (Enum<E> enumVal : enumData.getEnumConstants()) {
            array.add(enumVal.name().toLowerCase().replace("_", " "));
        }
        return array;
    }
    
    protected static float getSettingByName(Array<SettingsEntry> settings, String name) {
        for (int i = 0; i < settings.size; i++) {
            if (settings.get(i).getName().equals(name)) {
                return settings.get(i).getCurrent();
            }
        }
        System.out.println("Setting not found: " + name);
        return 0;
    }
    
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }
    
    public void drawExitButton() {
        
        if (!musicStarted && transparency == 0 && !render) {
            music.play();
            musicStarted = true;
        }
        
        exit.setColor(1, 1, 1, transparency);
        stage.draw();
        stage.act();
        if (Gdx.graphics.getDeltaTime() < 1f) {
            transparency = MathUtils.clamp(transparency - 0.5f * Gdx.graphics.getDeltaTime(), 0, 1);
        }
    }
    
    public void resize(int width, int height, int yOffset, boolean maxScale) {
        viewport.update(width, height);
        camera.position.set(0, yOffset, 0);
        float tempScaleH = height / (float) HEIGHT;
        float tempScaleW = width / (float) WIDTH;
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
        samplesNormalizedRaw = null;
        samplesNormalizedSmoothed = null;
        lSamplesNormalised = null;
        rSamplesNormalised = null;
        lSamplesNormalisedSmoothed = null;
        rSamplesNormalisedSmoothed = null;
        System.gc();
    }
    
    public static void setMusic(FileHandle fileHandle) {
        musicFile = fileHandle;
    }
    
}
