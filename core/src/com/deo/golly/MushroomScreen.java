package com.deo.golly;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deo.golly.postprocessing.PostProcessor;
import com.deo.golly.postprocessing.effects.Bloom;

public class MushroomScreen implements Screen {

    private ShapeRenderer renderer;
    private Array<Array<Vector2>> branches;
    private Array<Vector3> colors;
    private float fadeout;

    private SpriteBatch batch;
    private PostProcessor blurProcessor;
    private Bloom bloom;
    private Music music;
    private BitmapFont font;

    private float[] rSamplesNormalised;
    private float[] lSamplesNormalised;
    private float[] averageSamplesNormalised;

    private final int FPS = 30;
    private final int step = 44100 / FPS;
    private final boolean render = false;
    private int frame;
    private int recorderFrame;

    private final int SINGLE = 0;
    private final int DOUBLE = 1;
    private final int TRIPLE = 2;
    private final int DOUBLE_CHANNEL = 3;
    private boolean EXPONENTIAL = true;

    private final int type = DOUBLE_CHANNEL;

    MushroomScreen() {

        batch = new SpriteBatch();

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        branches = new Array<>();
        colors = new Array<>();
        fadeout = 0.05f;

        ShaderLoader.BasePath = "core/assets/shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(3);
        bloom.setBloomIntesity(4f);
        blurProcessor.addEffect(bloom);

        font = new BitmapFont(Gdx.files.internal("core/assets/font2(old).fnt"));

        MusicWave musicWave = new MusicWave();
        music = musicWave.getMusic();

        rSamplesNormalised = musicWave.normaliseSamples(false, false, musicWave.getRightChannelSamples());
        lSamplesNormalised = musicWave.normaliseSamples(false, false, musicWave.getLeftChannelSamples());
        averageSamplesNormalised = musicWave.normaliseSamples(false, true, musicWave.getSamples());

        for (int i = 0; i < averageSamplesNormalised.length; i++) {
            averageSamplesNormalised[i] *= 3;
        }

        if (!render) {
            music.play();
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        fadeOut();
        float angle;
        int iterations;
        if (!render) {
            angle = rSamplesNormalised[(int) (music.getPosition() * 44100)] * 45;
            iterations = (int) (lSamplesNormalised[(int) (music.getPosition() * 44100)] * 10) + 5;
            bloom.setBloomSaturation(averageSamplesNormalised[(int) (music.getPosition() * 44100)] + 1);
        } else {
            angle = rSamplesNormalised[frame] * 45;
            iterations = (int) (lSamplesNormalised[frame] * 10) + 5;
            bloom.setBloomSaturation(averageSamplesNormalised[frame] + 1);
            frame += step;
            recorderFrame++;
        }

        switch (type) {
            case (SINGLE):
                buildMushroom(-90, angle, 40, iterations, 800, 290);
                break;
            case (DOUBLE):
                buildMushroom(-90, angle, 40, iterations, 800, 450);
                buildMushroom(90, angle, 40, iterations, 800, 450);
                break;
            case(DOUBLE_CHANNEL):
                angle = rSamplesNormalised[(int) (music.getPosition() * 44100)] * 45;
                iterations = (int) (rSamplesNormalised[(int) (music.getPosition() * 44100)] * 10) + 5;
                buildMushroom(-90, angle, 40, iterations, 800, 450);
                angle = lSamplesNormalised[(int) (music.getPosition() * 44100)] * 45;
                iterations = (int) (lSamplesNormalised[(int) (music.getPosition() * 44100)] * 10) + 5;
                buildMushroom(90, angle, 40, iterations, 800, 450);
                break;
            case (TRIPLE):
                buildMushroom(150, angle, 40, iterations, 800, 450);
                buildMushroom(270, angle, 40, iterations, 800, 450);
                buildMushroom(390, angle, 40, iterations, 800, 450);
                break;
        }

        blurProcessor.capture();

        renderer.begin();
        for (int i = 0; i < branches.size; i++) {
            renderer.setColor(colors.get(i).x, colors.get(i).y, (branches.get(i).get(0).y - 100) / 900f, 1);
            renderer.line(branches.get(i).get(0), branches.get(i).get(1));
        }
        renderer.end();

        blurProcessor.render();

        if (render) {
            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

            for (int i4 = 4; i4 < pixels.length; i4 += 4) {
                pixels[i4 - 1] = (byte) 255;
            }

            Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
            BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
            PixmapIO.writePNG(Gdx.files.external("GollyRender/pict" + recorderFrame + ".png"), pixmap);
            pixmap.dispose();

            batch.begin();
            font.draw(batch, String.format("% 2f", recorderFrame / (float) FPS) + "s", 100, 120);
            boolean normal = frame / (float) 44100 == recorderFrame / (float) FPS;
            font.draw(batch, frame + "fr " + recorderFrame + "fr " + normal, 100, 170);
            font.draw(batch, frame / (float) averageSamplesNormalised.length * 100 + "%", 100, 70);
            batch.end();
        }
    }

    private void fadeOut() {
        for (int i = 0; i < colors.size; i++) {
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

    void buildMushroom(float initialAngle, float offsetAngle, float branchLength, int iterations, float x, float y) {

        float angleRight = initialAngle + offsetAngle;
        float angleLeft = initialAngle - offsetAngle;

        float sinRight = MathUtils.sinDeg(angleRight) * branchLength;
        float cosRight = MathUtils.cosDeg(angleRight) * branchLength;

        float sinLeft = MathUtils.sinDeg(angleLeft) * branchLength;
        float cosLeft = MathUtils.cosDeg(angleLeft) * branchLength;

        float xRight = x - cosRight;
        float yRight = y - sinRight;

        float xLeft = x - cosLeft;
        float yLeft = y - sinLeft;

        Array<Vector2> branchRight = new Array<>();

        branchRight.add(new Vector2(x, y));
        branchRight.add(new Vector2(xRight, yRight));

        Array<Vector2> branchLeft = new Array<>();

        branchLeft.add(new Vector2(x, y));
        branchLeft.add(new Vector2(xLeft, yLeft));

        branches.add(branchRight, branchLeft);
        colors.add(new Vector3(1, 1, 0), new Vector3(1, 1, 0));

        if (iterations > 0 && branchLength > 0) {
            float offset = -0.9f;
            float bOffset = 0;
            if (EXPONENTIAL) {
                offset = 1;
                bOffset = 1.7f;
            }
            buildMushroom(angleRight, offsetAngle + offset, branchLength + bOffset, iterations - 1, xRight, yRight);
            buildMushroom(angleLeft, offsetAngle + offset, branchLength + bOffset, iterations - 1, xLeft, yLeft);
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
