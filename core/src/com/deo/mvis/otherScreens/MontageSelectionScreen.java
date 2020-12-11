package com.deo.mvis.otherScreens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.deo.mvis.utils.MusicWave;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;

public class MontageSelectionScreen implements Screen {

    private Game game;
    private float[] samplesForFrames;
    private float[] samplesForFrames2;
    private float step;
    private ShapeRenderer renderer;
    private float threshold = 2;
    private final float desiredPeaks = 300;

    private float step2;
    private final float FPS = 30;

    private BitmapFont font;

    private SpriteBatch batch;

    MontageSelectionScreen(Game game) {

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        batch = new SpriteBatch();

        this.game = game;

        com.deo.mvis.utils.MusicWave musicWave = new MusicWave();

        float[] samples = musicWave.getSamples();

        float maxValue = 0;

        for (int i = 0; i < samples.length; i++) {
            if (samples[i] > maxValue) {
                maxValue = samples[i];
            }
        }

        step = samples.length / 16000f;
        step2 = 44100 / FPS;

        float[] samplesNormalised = new float[samples.length];

        for (int i = 0; i < samples.length; i++) {
            samplesNormalised[i] = samples[i] / maxValue;
        }

        samplesForFrames = new float[16000];

        for (int i = 0; i < 16000; i++) {
            samplesForFrames[i] = samplesNormalised[(int) (i * step)];
        }

        samplesForFrames2 = new float[(int) (samples.length / step2) + 1];

        for (int i = 0; i < samplesNormalised.length; i += step2) {
            samplesForFrames2[(int) (i / step2)] = samplesNormalised[i];
        }

        int peaks = 0;

        while (peaks<desiredPeaks){
            peaks = 0;
            for (int i = 0; i < samplesForFrames2.length; i++) {
                if (samplesForFrames2[i] >= threshold) {
                    peaks++;
                }
            }
            threshold -= 0.001;
        }

        font = new BitmapFont(Gdx.files.internal("core/assets/font2(old).fnt"));

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);

        renderer.begin();
        renderer.setColor(Color.WHITE);
        for (int i = 0; i < samplesForFrames.length; i++) {
            renderer.line(i/10f, 0, i/10f, samplesForFrames[i] * 100);
        }
        renderer.setColor(Color.RED);
        renderer.line(0, threshold * 100, 1600, threshold * 100);

        int peaks = 0;
        for (int i = 0; i < samplesForFrames2.length; i++) {
            if (samplesForFrames2[i] >= threshold) {
                peaks++;
            }
        }

        renderer.end();
        batch.begin();
        font.draw(batch, peaks + "  " + threshold, 200, 200);
        batch.end();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            game.setScreen(new MontageScreen(threshold));
        }
        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            threshold += 0.001;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            threshold -= 0.001;
        }
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
