package com.deo.mvis.visualisers;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.Utils;

public class BaseVisualiser {

    public Utils utils;
    public ScreenViewport viewport;
    public OrthographicCamera camera;

    public ShapeRenderer renderer;

    final int FPS = 30;
    public final int step = 44100 / FPS;
    public final boolean render = false;
    public int frame;
    public int recorderFrame;

    MusicWave musicWave;
    public Music music;

    float[] samplesRaw;
    public float[] samplesSmoothed;

    float[] rSamplesNormalised;
    float[] lSamplesNormalised;

    float[] rSamplesNormalisedSmoothed;
    float[] lSamplesNormalisedSmoothed;

    int numOfSamples;

    public BaseVisualiser() {

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        musicWave = new MusicWave();
        music = musicWave.getMusic();

        samplesRaw = musicWave.normaliseSamples(false, false, musicWave.getSamples());
        samplesSmoothed = musicWave.smoothSamples(samplesRaw, 2, 32);

        rSamplesNormalised = musicWave.normaliseSamples(false, false, musicWave.getRightChannelSamples());
        lSamplesNormalised = musicWave.normaliseSamples(false, false, musicWave.getLeftChannelSamples());

        rSamplesNormalisedSmoothed = musicWave.smoothSamples(musicWave.getRightChannelSamples(), 2, 32);
        lSamplesNormalisedSmoothed = musicWave.smoothSamples(musicWave.getLeftChannelSamples(), 2, 32);

        numOfSamples = samplesRaw.length;

        utils = new Utils(FPS, step, samplesSmoothed, 3, 1, 1, true);

        if (!render) {
            music.play();
            music.setLooping(true);
        }

    }

    public void resize(int width, int height, int yOffset, boolean maxScale) {
        viewport.update(width, height);
        camera.position.set(0, yOffset, 0);
        float tempScaleH = height / 900f;
        float tempScaleW = width / 1600f;
        float zoom = Math.min(tempScaleH, tempScaleW);
        float additionalZoom = 0.5f;
        if(maxScale){
            additionalZoom = 0;
        }
        camera.zoom = 1 / zoom + additionalZoom;
        camera.update();
    }

    public void dispose() {
        renderer.dispose();
        utils.dispose();
        musicWave.dispose();
    }

}
