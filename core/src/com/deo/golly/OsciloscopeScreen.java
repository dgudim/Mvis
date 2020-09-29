package com.deo.golly;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

public class OsciloscopeScreen implements Screen {

    private float[] lSamples, rSamples;
    private float[] rSamplesNormalised;
    private float[] lSamplesNormalised;
    private ShapeRenderer renderer;
    private Music music;
    private Array<Vector2> dots;
    private Array<Vector3> colors;

    private PostProcessor blurProcessor;
    private Bloom bloom;

    private final boolean realtime = false;
    private final int recordingFPS = 60;
    private float step;
    private int recorderFrame;
    int frame;
    private final float fadeout = 0.005f;

    private final int STANDARD = 0;
    private final int RADIAL = 1;

    private final int type = RADIAL;

    private BitmapFont font;

    private SpriteBatch batch;

    OsciloscopeScreen() {

        step = 44100 / (float)recordingFPS;

        ShaderLoader.BasePath = "core/assets/shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(3);
        bloom.setBloomSaturation(2f);
        bloom.setBloomIntesity(2f);
        blurProcessor.addEffect(bloom);

        MusicWave musicWave = new MusicWave();

        lSamples = musicWave.getLeftChannelSamples();
        rSamples = musicWave.getRightChannelSamples();
        music = musicWave.getMusic();

        batch = new SpriteBatch();
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        font = new BitmapFont(Gdx.files.internal("core/assets/font2(old).fnt"));

        dots = new Array<>();
        colors = new Array<>();

        float maxValue = 0;

        rSamplesNormalised = new float[rSamples.length];
        lSamplesNormalised = new float[rSamples.length];

        for (int i = 0; i < rSamples.length; i++) {
            if (lSamples[i] > maxValue) {
                maxValue = lSamples[i];
            }
            if (rSamples[i] > maxValue) {
                maxValue = rSamples[i];
            }
        }

        for (int i = 0; i < rSamples.length; i++) {
                rSamplesNormalised[i] = rSamples[i] / maxValue;
                lSamplesNormalised[i] = lSamples[i] / maxValue;
            if(type == STANDARD) {
                rSamplesNormalised[i] = rSamplesNormalised[i] * 300;
                lSamplesNormalised[i] = lSamplesNormalised[i] * 300;
            }
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

        blurProcessor.capture();

        renderer.begin();

        if(realtime){

            addCoords((int) (music.getPosition() * 44100));
            render();
            fadeOut();

        }else{

            for(int i = 0; i<step; i++) {
                addCoords(frame);
                frame++;
                fadeOut();
            }

            recorderFrame++;

            render();

        }

        renderer.end();

        blurProcessor.render();

        if(!realtime){
            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

            for (int i4 = 4; i4 < pixels.length; i4 += 4) {
                pixels[i4 - 1] = (byte) 255;
            }

            Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
            BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
            PixmapIO.writePNG(Gdx.files.external("GollyRender/pict" + recorderFrame + ".png"), pixmap);
            pixmap.dispose();

            batch.begin();
            font.draw(batch, String.format("% 2f", frame/(float)44100) + "s", 100, 70);
            batch.end();
        }

    }

    private void addCoords(int pos){
        Vector3 color = new Vector3();
        color.set(0, 1, 1);

        Vector2 coords = new Vector2();

        switch (type) {
            case (STANDARD):
                coords.set(lSamplesNormalised[pos], rSamplesNormalised[pos]);
                break;
            case (RADIAL):
                float angle = -lSamplesNormalised[pos]*180;
                float x = - MathUtils.cosDeg(angle)*rSamplesNormalised[pos]*500;
                float y = - MathUtils.sinDeg(angle)*rSamplesNormalised[pos]*500;
                coords.set(x, y);
                break;
        }

        colors.add(color);
        dots.add(coords);
    }

    private void render(){
        for (int i = 1; i < dots.size; i++) {
            Vector3 colorV = colors.get(i);
            renderer.setColor(colorV.x, colorV.y, colorV.z, 1);
            renderer.line(dots.get(i).x + 800, dots.get(i).y + 450, dots.get(i - 1).x + 800, dots.get(i - 1).y + 450);
        }
    }

    private void fadeOut(){
        for (int i = 1; i < dots.size; i++) {

            Vector3 colorV = colors.get(i);

            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - fadeout, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - fadeout, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - fadeout, 0, 1);
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
