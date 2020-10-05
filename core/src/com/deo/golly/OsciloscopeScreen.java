package com.deo.golly;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deo.golly.postprocessing.PostProcessor;
import com.deo.golly.postprocessing.effects.Bloom;

public class OsciloscopeScreen implements Screen {

    private float[] rSamplesNormalised;
    private float[] lSamplesNormalised;
    private float[] averageSamplesNormalised;
    private ShapeRenderer renderer;
    private Music music;
    private Array<Vector3> dots;
    private Array<Vector3> colors;

    private PostProcessor blurProcessor;
    private Bloom bloom;

    private final boolean realtime = true;
    private final int recordingFPS = 30;
    private float step;
    private int recorderFrame;
    int frame;
    private float fadeout = 0.005f;

    private final int STANDARD = 0;
    private final int RADIAL = 1;
    private final int BUBBLE = 2;
    private final int RADIAL_BUBBLE = 3;
    private final int SHAPES = 4;
    private final int SINUS = 5;

    private final int LIME = 103;
    private final int FIRE = 104;
    private final int CYAN = 105;
    private Color palletColor;

    private float maxSaturation = 4;

    private final int type = SINUS;
    private final int pallet = FIRE;
    private int skipOver;

    private BitmapFont font;

    private SpriteBatch batch;

    OsciloscopeScreen() {

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

        dots = new Array<>();
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

        if (type == BUBBLE || type == SHAPES || type == RADIAL_BUBBLE || type == SINUS) {
            skipOver = (int) step;
            maxSaturation = 0;
        } else {
            skipOver = 1;
        }

        if (type == SINUS) {
            fadeout *= 30;
            maxSaturation = 3;
            bloom.setBloomIntesity(2f);
        }
        if (type == SHAPES) {
            fadeout *= 5;
        }

        for (int i = 0; i < rSamples.length; i++) {
            rSamplesNormalised[i] = rSamples[i] / maxValue;
            lSamplesNormalised[i] = lSamples[i] / maxValue;
            averageSamplesNormalised[i] = averageSamples[i] / maxValue_average;
            averageSamplesNormalised[i] *= maxSaturation;
            if (type == STANDARD || type == SHAPES || type == RADIAL) {
                rSamplesNormalised[i] = rSamplesNormalised[i] * 450;
                lSamplesNormalised[i] = lSamplesNormalised[i] * 450;
            } else if (type == BUBBLE || type == RADIAL_BUBBLE) {
                rSamplesNormalised[i] = rSamplesNormalised[i] * 300;
                lSamplesNormalised[i] = lSamplesNormalised[i] * 300;
            }
        }

        if (realtime) {
            music.play();
        }

        palletColor = new Color();

        switch (pallet) {
            case (FIRE):
                palletColor.r = 1;
                palletColor.g = 1;
                break;
            case (CYAN):
                palletColor.b = 1;
                palletColor.g = 1;
                break;
            case (LIME):
                palletColor.r = 0.5f;
                palletColor.g = 1;
                break;
        }

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        if (realtime) {
            bloom.setBloomSaturation(MathUtils.clamp(averageSamplesNormalised[(int) (music.getPosition() * 44100)] + 1, 1, maxSaturation + 1));
        } else {
            bloom.setBloomSaturation(MathUtils.clamp(averageSamplesNormalised[(int) (frame + step)] + 1, 1, maxSaturation + 1));
        }

        blurProcessor.capture();

        renderer.begin();

        if (realtime) {

            addCoords((int) (music.getPosition() * 44100));
            render();
            fadeOut();

        } else {

            for (int i = 0; i < step; i += skipOver) {
                addCoords(frame);
                frame += skipOver;
                fadeOut();
            }

            recorderFrame++;

            render();

        }

        renderer.end();

        blurProcessor.render();

        if (!realtime) {
            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

            for (int i4 = 4; i4 < pixels.length; i4 += 4) {
                pixels[i4 - 1] = (byte) 255;
            }

            Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
            BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
            PixmapIO.writePNG(Gdx.files.external("GollyRender/pict" + recorderFrame + ".png"), pixmap);
            pixmap.dispose();

            batch.begin();
            font.draw(batch, String.format("% 2f", recorderFrame / (float) recordingFPS) + "s", 100, 120);
            boolean normal = frame / (float) 44100 == recorderFrame / (float) recordingFPS;
            font.draw(batch, frame + "fr "+recorderFrame+"fr " + normal, 100, 170);
            font.draw(batch, frame/(float)averageSamplesNormalised.length*100+"%", 100, 70);
            batch.end();
        }

    }

    private void addCoords(int pos) {

        Vector3 color = new Vector3(palletColor.r, palletColor.g, palletColor.b);
        Vector3 coords = new Vector3();

        float x, y, angle, rad;
        int repeat = 1;

        if (!realtime) {
            repeat = 3;
        }

        switch (type) {
            case (STANDARD):
                coords.set(lSamplesNormalised[pos], rSamplesNormalised[pos], 0);
                break;
            case (RADIAL):
                angle = -lSamplesNormalised[pos] * 180;
                x = -MathUtils.cosDeg(angle) * rSamplesNormalised[pos];
                y = -MathUtils.sinDeg(angle) * rSamplesNormalised[pos];
                coords.set(x, y, 0);
                break;
            case (BUBBLE):
                for (int i = 0; i < repeat; i++) {
                    rad = Math.abs(Math.max(lSamplesNormalised[pos], rSamplesNormalised[pos]));
                    rad = (float) (Math.random() * rad);
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(lSamplesNormalised[pos], rSamplesNormalised[pos], rad));
                }
                break;
            case (RADIAL_BUBBLE):
                for (int i = 0; i < repeat; i++) {
                    rad = Math.abs(Math.max(lSamplesNormalised[pos], rSamplesNormalised[pos]));
                    rad = (float) (Math.random() * rad);
                    angle = -lSamplesNormalised[pos] * 180;
                    x = -MathUtils.cosDeg(angle) * rSamplesNormalised[pos];
                    y = -MathUtils.sinDeg(angle) * rSamplesNormalised[pos];
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(x, y, rad));
                }
                break;
            case (SHAPES):
                x = 0;
                y = 0;
                dots.add(new Vector3().set(x, y, 0));
                colors.add(new Vector3(1, 1, 0));
                angle = lSamplesNormalised[pos] * 90 / 450;
                for (int i = 0; i < 50; i++) {
                    x = x - (float) Math.cos(angle) * rSamplesNormalised[pos];
                    y = y - (float) Math.sin(angle) * rSamplesNormalised[pos];
                    angle = angle + Math.abs(lSamplesNormalised[pos] / 450) + 1;
                    dots.add(new Vector3().set(x, y, 0));
                    colors.add(new Vector3(palletColor.r, i / 50f * palletColor.g, palletColor.b));
                }
                break;
            case (SINUS):
                for (float i = 0; i < 1600; i += 0.5f) {
                    if (i % 7 == 0) {
                        y = (float) (Math.sin(i / 32) * lSamplesNormalised[pos] * 150);
                    } else {
                        y = (float) (Math.cos(i / 32) * rSamplesNormalised[pos] * 150);
                    }
                    dots.add(new Vector3().set(i - 800, y, 0));
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                }
                dots.add(new Vector3().set(800, 0, 0));
                colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                dots.add(new Vector3().set(-800, 0, 0));
                colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
        }


        if (type != SHAPES && type != RADIAL_BUBBLE && type != BUBBLE && type != SINUS) {
            colors.add(color);
            dots.add(coords);
        }
    }

    private void render() {
        if (type == BUBBLE || type == RADIAL_BUBBLE) {
            for (int i = 0; i < dots.size; i++) {
                Vector3 colorV = colors.get(i);
                renderer.setColor(colorV.x, colorV.y, colorV.z, 1);
                float x = dots.get(i).x + 800;
                float y = dots.get(i).y + 450;
                float radius = dots.get(i).z;
                renderer.circle(x, y, radius);
            }
        } else {
            for (int i = 1; i < dots.size; i++) {
                Vector3 colorV = colors.get(i);
                renderer.setColor(colorV.x, colorV.y, colorV.z, 1);
                float x = dots.get(i - 1).x + 800;
                float y = dots.get(i - 1).y + 450;
                float x2 = dots.get(i).x + 800;
                float y2 = dots.get(i).y + 450;
                renderer.line(x, y, x2, y2);
            }
        }
    }

    private void fadeOut() {
        for (int i = 1; i < dots.size; i++) {

            Vector3 colorV = colors.get(i);

            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - fadeout / 1.5f, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - fadeout * 1.5f, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - fadeout, 0, 1);
                if (type == BUBBLE || type == RADIAL_BUBBLE) {
                    dots.get(i).z = MathUtils.clamp(dots.get(i).z - fadeout * 4, 0, 450);
                }
                colors.set(i, colorV);
            } else {
                dots.removeIndex(i);
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
