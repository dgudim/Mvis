package com.deo.golly;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.golly.postprocessing.PostProcessor;
import com.deo.golly.postprocessing.effects.Bloom;

public class MushroomScreen implements Screen {

    private SpriteBatch batch;
    private BitmapFont font;
    private float[] rSamplesNormalised;
    private float[] lSamplesNormalised;
    private float[] averageSamplesNormalised;
    private ShapeRenderer renderer;
    private Music music;
    private Array<Vector2> branches;

    private PostProcessor blurProcessor;
    private Bloom bloom;

    private final boolean realtime = true;
    private final int recordingFPS = 60;
    private float step;
    private int recorderFrame;
    int frame;
    private final float fadeout = 0.005f;
    private Array<Vector3> colors;

    MushroomScreen() {
        step = 44100 / (float) recordingFPS;

        ShaderLoader.BasePath = "core/assets/shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(3);
        bloom.setBloomIntesity(4f);
        blurProcessor.addEffect(bloom);

        MusicWave musicWave = new MusicWave();

        float[] lSamples = musicWave.getLeftChannelSamples();
        float[] rSamples = musicWave.getRightChannelSamples();
        float[] averageSamples = musicWave.getSamples();
        music = musicWave.getMusic();

        batch = new SpriteBatch();
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        font = new BitmapFont(Gdx.files.internal("core/assets/font2(old).fnt"));

        branches = new Array<>();
        colors = new Array<>();

        float maxValue = 0;
        float maxValue_average = 0;

        rSamplesNormalised = new float[rSamples.length];
        lSamplesNormalised = new float[rSamples.length];
        averageSamplesNormalised = new float[rSamples.length];

        for (int i = 0; i < rSamples.length; i++) {
            if (Math.abs(lSamples[i]) > maxValue) {
                maxValue = Math.abs(lSamples[i]);
            }
            if (Math.abs(rSamples[i]) > maxValue) {
                maxValue = Math.abs(rSamples[i]);
            }
        }

        for (int i = 0; i < averageSamples.length; i++) {
            if (Math.abs(averageSamples[i]) > maxValue_average) {
                maxValue_average = Math.abs(averageSamples[i]);
            }
        }

        for (int i = 0; i < rSamples.length; i++) {
            rSamplesNormalised[i] = rSamples[i] / maxValue;
            lSamplesNormalised[i] = lSamples[i] / maxValue;
            averageSamplesNormalised[i] = averageSamples[i] / maxValue_average;
            rSamplesNormalised[i] = rSamplesNormalised[i] * 450;
            lSamplesNormalised[i] = lSamplesNormalised[i] * 450;
        }

        if (realtime) {
            music.play();
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        renderer.begin();
        buildTree(lSamplesNormalised[(int) (music.getPosition() * 44100)], rSamplesNormalised[(int) (music.getPosition() * 44100)]);
        for (int i = 1; i < branches.size; i++) {
            Vector3 colorV = colors.get(i-1);
            renderer.setColor(colorV.x, colorV.y, colorV.z, 1);
            renderer.line(branches.get(i - 1), branches.get(i));
        }
        renderer.end();
        fadeOut();
    }

    private void buildTree(float leftSample, float rightSample) {

    }

    private void fadeOut() {
        for (int i = 0; i < branches.size; i++) {

            Vector3 colorV = colors.get(i);

            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - fadeout / 1.5f, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - fadeout * 1.5f, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - fadeout, 0, 1);
                colors.set(i, colorV);
            } else {
                branches.removeIndex(i);
                colors.removeIndex(i);
            }
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
