package com.deo.mvis.otherScreens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.deo.mvis.utils.MusicWave;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;

public class VideoScreen implements Screen {

    private float[] samplesNormalised;
    private final int maxSpeedMultiplier = 5;
    private int frameCount = 5000;
    private Array<Integer> frames;
    private int currentFrame;
    private final int FPS = 0;
    private int musicProgress;
    private int step;

    VideoScreen() {

        frames = new Array<>();

        step = 44100 / FPS;
        musicProgress = 0;

        MusicWave musicWave = new MusicWave(null, false);

        float[] samples = musicWave.getSamples();

        float maxValue = 0;

        for (int i = 0; i < samples.length; i++) {
            if (samples[i] > maxValue) {
                maxValue = samples[i];
            }
        }

        samplesNormalised = new float[samples.length];

        for (int i = 0; i < samples.length; i++) {
            samplesNormalised[i] = samples[i] / maxValue;
        }

        currentFrame = 0;
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);

        try {
            currentFrame += MathUtils.clamp(samplesNormalised[musicProgress] * maxSpeedMultiplier, 1, maxSpeedMultiplier);
        }catch (Exception e){
            for(int i = 0; i<frameCount; i++){
                if(!frames.contains(i, true)){
                    FileHandle file = Gdx.files.external("image2MusicSync/pict"+i+".jpg");
                    file.delete();
                }
            }
        }

        musicProgress += step;

        frames.add(currentFrame);

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
