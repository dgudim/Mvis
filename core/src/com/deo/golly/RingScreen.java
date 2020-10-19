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

public class RingScreen implements Screen {

    private ShapeRenderer renderer;
    private Array<Float> radiuses, radiuses2;
    private Array<Vector2> positions;
    private Array<Vector3> colors;
    private float fadeout, ringGrowSpeed;

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

    RingScreen() {
        batch = new SpriteBatch();

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        radiuses = new Array<>();
        radiuses2 = new Array<>();
        positions = new Array<>();
        colors = new Array<>();
        fadeout = 0.006f;
        ringGrowSpeed = 12f;

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

        fadeout();
        float radiusx, radiusy;
        if (!render) {
            radiusx = lSamplesNormalised[(int) (music.getPosition() * 44100)] * 720;
            radiusy = rSamplesNormalised[(int) (music.getPosition() * 44100)] * 720;
            bloom.setBloomSaturation(averageSamplesNormalised[(int) (music.getPosition() * 44100)] + 1);
        } else {
            radiusx = lSamplesNormalised[frame] * 720;
            radiusy = rSamplesNormalised[frame] * 720;
            bloom.setBloomSaturation(averageSamplesNormalised[frame] + 1);
            frame += step;
            recorderFrame++;
        }

        positions.add(new Vector2(800, 450));
        colors.add(new Vector3(1, 1, 0));

        radiuses.add(radiusx);
        radiuses2.add(radiusy);

        blurProcessor.capture();
        renderer.begin();
        for (int i = 0; i < positions.size; i++) {
            renderer.setColor(colors.get(i).x, colors.get(i).y, colors.get(i).z, 1);
            renderer.ellipse(positions.get(i).x - radiuses.get(i) / 2f, positions.get(i).y - radiuses2.get(i) / 2f, radiuses.get(i), radiuses2.get(i));
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

    void fadeout() {
        for (int i = 0; i < positions.size; i++) {
            radiuses.set(i, radiuses.get(i) + ringGrowSpeed);
            radiuses2.set(i, radiuses2.get(i) + ringGrowSpeed);
            Vector3 colorV = colors.get(i);
            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - fadeout / 1.5f, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - fadeout * 1.5f, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - fadeout, 0, 1);
                colors.set(i, colorV);
            } else {
                positions.removeIndex(i);
                radiuses.removeIndex(i);
                radiuses2.removeIndex(i);
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
