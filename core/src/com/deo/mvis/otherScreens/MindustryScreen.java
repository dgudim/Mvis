package com.deo.mvis.otherScreens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deo.mvis.utils.MusicWave;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;

public class MindustryScreen implements Screen {

    private ShapeRenderer renderer;
    private float[] samplesNormalised;
    private Music music;
    private float prevSample;
    private final int FPS = 15;
    private float step;
    private int musicProgress;
    private int frame;

    MindustryScreen(){

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        step = 44100 / FPS;
        musicProgress = 0;
        frame = 0;

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

        music = musicWave.getMusic();
        music.play();

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);

        float sample = samplesNormalised[musicProgress];

        float scale = 5;

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(prevSample/3f, sample/3f, 0, 1);
        renderer.rect(0, 0, 80*scale, 16*scale);
        renderer.rect(0, 64*scale, 80*scale, 16*scale);
        renderer.setColor(prevSample/2f, sample, 0, 1);
        renderer.rect(0, 16*scale, 80*scale, 16*scale);
        renderer.rect(0, 48*scale, 80*scale, 16*scale);
        renderer.setColor(sample, prevSample, 0, 1);
        renderer.rect(0, 32*scale, 80*scale, 16*scale);
        renderer.end();

        prevSample = samplesNormalised[musicProgress];

        /*
        String command = new StringBuilder().append("draw color ").append(prevSample / 3f * 255).append(" ").append(sample / 3f * 255).append(" ").append(0).append("\n").append("draw rect 0 0 80 16\ndraw rect 0 64 80 16\n").append("draw color ").append(prevSample / 2f * 255).append(" ").append(sample * 255).append(" ").append(0).append("\n").append("draw rect 0 16 80 16\ndraw rect 0 48 80 16\n").append("draw color ").append(sample * 255).append(" ").append(prevSample * 255).append(" ").append(0).append("\ndraw rect 0 32 80 16\n").append("drawflush display1\n").toString();

        FileHandle file = Gdx.files.external("GollyRender/vis.txt");
        file.writeString(command, true);
        
         */

        musicProgress += step;

        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, 400, 400, true);

        for (int i = 4; i < pixels.length; i += 4) {
            pixels[i - 1] = (byte) 255;
        }

        frame++;

        Pixmap pixmap = new Pixmap(400, 400, Pixmap.Format.RGBA8888);
        BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
        //PixmapIO.writePNG(Gdx.files.external("GollyRender/pict" + frame + ".png"), pixmap);
        pixmap.dispose();

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
