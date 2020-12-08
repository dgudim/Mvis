package com.deo.mvis;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deo.mvis.postprocessing.PostProcessor;
import com.deo.mvis.postprocessing.effects.Bloom;

public class Utils {

    private int step;
    private int FPS;
    private BitmapFont font;
    private SpriteBatch batch;
    private float[] samples;
    private PostProcessor blurProcessor;
    private Bloom bloom;
    private boolean enableBloom;

    Utils(int FPS, int step, float[] samples, int bloomPasses, float bloomIntensity, float bloomSaturation, boolean enableBloom){
        this.step = step;
        this.FPS = FPS;
        this.samples = samples;
        this.enableBloom = enableBloom;
        batch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("font2(old).fnt"));

        ShaderLoader.BasePath = "shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(bloomPasses);
        bloom.setBloomIntesity(bloomIntensity);
        bloom.setBloomSaturation(bloomSaturation);
        blurProcessor.addEffect(bloom);
    }

    public void bloomBegin(boolean syncToMusic, int pos){
        if(enableBloom) {
            if (syncToMusic) {
                bloom.setBloomSaturation(Math.abs(samples[pos]) + 1);
            }
            blurProcessor.capture();
        }
    }

    public void bloomRender(){
        if(enableBloom) {
            blurProcessor.render();
        }
    }

    public void setBloomIntensity(float intensity){
        bloom.setBloomIntesity(intensity);
    }

    public void setBloomSaturation(float saturation){
        bloom.setBloomSaturation(saturation);
    }

    public void makeAScreenShot(int recorderFrame){

        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

        for (int i4 = 4; i4 < pixels.length; i4 += 4) {
            pixels[i4 - 1] = (byte) 255;
        }

        Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
        BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
        PixmapIO.writePNG(Gdx.files.external("GollyRender/pict" + recorderFrame + ".png"), pixmap);
        pixmap.dispose();
    }

    // a method fod displaying render debug data
    public void displayData(int recorderFrame, int frame){
        batch.begin();
        font.draw(batch, String.format("% 2f", recorderFrame / (float) FPS) + "s", 100, 120);
        boolean normal = frame / (float) 44100 == recorderFrame / (float) FPS;
        font.draw(batch, frame + "fr " + recorderFrame + "fr " + normal, 100, 170);
        font.draw(batch, frame / (float) samples.length * 100 + "%", 100, 70);
        font.draw(batch, computeTime(recorderFrame) + "h", 100, 220);
        batch.end();
    }

    private float computeTime(int recorderFrame) {
        return Gdx.graphics.getDeltaTime() * (samples.length / (float) step - recorderFrame) / 3600;
    }

    public static int getRandomInRange(int min, int max) {
        return (MathUtils.random(max - min) + min);
    }

}
