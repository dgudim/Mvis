package com.deo.mvis.utils;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deo.mvis.postprocessing.PostProcessor;
import com.deo.mvis.postprocessing.effects.Bloom;

import java.util.Locale;

public class Utils {
    
    private static final Preferences prefs = Gdx.app.getPreferences("MvisPrefs");
    
    private final int step;
    private final int FPS;
    private final BitmapFont font;
    private final SpriteBatch batch;
    private final MusicWave musicWave;
    private final float[] smoothedSamples;
    private final PostProcessor blurProcessor;
    public Bloom bloom;
    private boolean enableBloom;
    
    public float maxSaturation = 1;
    
    public Utils(int FPS, int step, MusicWave musicWave, float[] smoothedSamples, int bloomPasses, float bloomIntensity, float bloomSaturation, boolean enableBloom, SpriteBatch batch) {
        this.step = step;
        this.FPS = FPS;
        this.musicWave = musicWave;
        this.enableBloom = enableBloom;
        this.batch = batch;
        this.smoothedSamples = smoothedSamples;
        font = new BitmapFont(Gdx.files.internal("font2(old).fnt"));
        
        ShaderLoader.BasePath = "shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(bloomPasses);
        bloom.setBloomIntensity(bloomIntensity);
        bloom.setBloomSaturation(bloomSaturation);
        blurProcessor.addEffect(bloom);
    }
    
    public void bloomBegin(boolean syncToMusic, int pos) {
        if (enableBloom) {
            if (syncToMusic) {
                bloom.setBloomSaturation(Math.abs(smoothedSamples[pos]) * maxSaturation + 1);
            }
            blurProcessor.capture();
        }
    }
    
    public void bloomRender() {
        if (enableBloom) {
            blurProcessor.render();
        }
    }
    
    public void changeBloomEnabledState(boolean enableBloom) {
        this.enableBloom = enableBloom;
    }
    
    public void setBloomIntensity(float intensity) {
        bloom.setBloomIntensity(intensity);
    }
    
    public void setBloomSaturation(float saturation) {
        bloom.setBloomSaturation(saturation);
    }
    
    public void makeAScreenShot(int recorderFrame) {
        
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        
        for (int i4 = 4; i4 < pixels.length; i4 += 4) {
            pixels[i4 - 1] = (byte) 255;
        }
        
        FileHandle file;
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            file = Gdx.files.external("Mvis/rendered/pict" + recorderFrame + ".png");
        } else {
            file = Gdx.files.external("GollyRender/pict" + recorderFrame + ".png");
        }
        
        Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
        BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
        PixmapIO.writePNG(file, pixmap);
        pixmap.dispose();
    }
    
    // a method for displaying render debug data
    public void displayData(int recorderFrame, int frame, Matrix4 projMat) {
        batch.setProjectionMatrix(projMat);
        batch.begin();
        font.draw(batch, String.format(Locale.ROOT, "% 2f", recorderFrame / (float) FPS) + "s", -WIDTH / 2f + 100, -HEIGHT / 2f + 120);
        boolean normal = frame / (float) musicWave.sampleRate == recorderFrame / (float) FPS;
        font.draw(batch, frame + "fr " + recorderFrame + "fr " + normal, -WIDTH / 2f + 100, -HEIGHT / 2f + 170);
        font.draw(batch, frame / (float) smoothedSamples.length * 100 + "%", -WIDTH / 2f + 100, -HEIGHT / 2f + 70);
        font.draw(batch, computeTime(recorderFrame) + "h", -WIDTH / 2f + 100, -HEIGHT / 2f + 220);
        batch.end();
    }
    
    private float computeTime(int recorderFrame) {
        return Gdx.graphics.getDeltaTime() * (smoothedSamples.length / (float) step - recorderFrame) / 3600;
    }
    
    public static int getRandomInRange(int min, int max) {
        return (MathUtils.random(max - min) + min);
    }
    
    public static float getFloat(String key) {
        return prefs.getFloat(key);
    }
    
    public static void putFloat(String key, float val) {
        prefs.putFloat(key, val);
        prefs.flush();
    }
    
    public static boolean getBoolean(String key) {
        return (prefs.getBoolean(key));
    }
    
    public static void putBoolean(String key, boolean val) {
        prefs.putBoolean(key, val);
        prefs.flush();
    }
    
    public static void putInteger(String key, int val) {
        prefs.putInteger(key, val);
        prefs.flush();
    }
    
    public static int getInteger(String key) {
        return (prefs.getInteger(key));
    }
    
    public static void deleteKey(String key) {
        prefs.remove(key);
        prefs.flush();
    }
    
    public void dispose() {
        blurProcessor.dispose();
        font.dispose();
    }
    
}
