package com.deo.mvis;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.deo.mvis.Utils.getRandomInRange;

public class MontageScreen implements Screen {

    private float[] samplesNormalised;
    private Array<Integer> peaks;
    private float[] samplesForFrames;
    private int musicProgress;
    private float step;
    private final int FPS = 30;
    private float threshold;
    private Image image;
    private SpriteBatch batch;
    private final int imageCount = 67;
    private int frame;
    private Texture[] images;
    private boolean realtime = true;
    private Music music;
    private final int cooldown = 15;
    private int cFrame;

    public MontageScreen(float threshold) {
        step = 44100 / FPS;

        this.threshold = threshold;

        peaks = new Array<>();

        batch = new SpriteBatch();

        MusicWave musicWave = new MusicWave();

        float[] samples = musicWave.getSamples();

        float maxValue = 0;

        for (int i = 0; i < samples.length; i++) {
            if (samples[i] > maxValue) {
                maxValue = samples[i];
            }
        }

        samplesNormalised = new float[samples.length];
        samplesForFrames = new float[(int) (samples.length / step) + 1];

        for (int i = 0; i < samples.length; i++) {
            samplesNormalised[i] = samples[i] / maxValue;
        }

        for (int i = 0; i < samplesNormalised.length; i += step) {
            samplesForFrames[(int) (i / step)] = samplesNormalised[i];
        }

        for (int i = 0; i < samplesForFrames.length; i++) {
            if (samplesForFrames[i] >= threshold) {
                peaks.add(i);
            }
        }

        musicProgress = 0;

        images = new Texture[imageCount];
        for (int i = 0; i < imageCount; i++) {
            images[i] = new Texture(Gdx.files.internal("core/assets/images/pict" + i) + ".jpg");
        }

        image = new Image(images[getRandomInRange(0, imageCount - 1)]);
        image.setScaling(Scaling.fit);
        image.setSize(1600, 900);
        cFrame = 0;

        music = musicWave.getMusic();

        if (realtime) {
            music.play();
        }

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);

        cFrame--;

        if (!realtime) {
            if (peaks.contains(musicProgress, true)) {
                if (cFrame<=0) {
                    image = new Image(images[getRandomInRange(0, imageCount - 1)]);
                    image.setScaling(Scaling.fit);
                    image.setSize(1600, 900);
                    cFrame = cooldown;
                }
            }
            musicProgress++;
        } else {
            if (samplesNormalised[(int) (music.getPosition() * 44100)] >= threshold) {
                if (cFrame<=0) {
                    image = new Image(images[getRandomInRange(0, imageCount - 1)]);
                    image.setScaling(Scaling.fit);
                    image.setSize(1600, 900);
                    cFrame = cooldown;
                }
            }
        }
        batch.begin();
        image.draw(batch, 1);
        batch.end();
        if (!realtime) {
            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

            for (int i = 4; i < pixels.length; i += 4) {
                pixels[i - 1] = (byte) 255;
            }

            frame++;

            Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
            BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
            PixmapIO.writePNG(Gdx.files.external("GollyRender/pict" + frame + ".png"), pixmap);
            pixmap.dispose();

            if (frame == samplesForFrames.length) {
                Gdx.app.exit();
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
