package com.deo.mvis;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;

import static com.deo.mvis.BaseEngine.HEIGHT;
import static com.deo.mvis.BaseEngine.WIDTH;
import static com.deo.mvis.Utils.getRandomInRange;

public class FFTScreen implements Screen {

    private MusicWave musicWave;
    private float[] averageSamplesNormalised;
    private Music music;
    private ShapeRenderer renderer;
    private FloatFFT_1D fft;
    private float[] displaySamples;
    private Array<Vector3> littleTriangles;
    private Array<Vector2> littleTrianglesSpeeds;
    private Array<Color> littleTrianglesColors;
    private Utils utils;

    private final int FPS = 30;
    private final int step = 44100 / FPS;
    private final boolean render = false;
    private int frame;
    private int recorderFrame;

    private final int DEFAULT = 0;
    private final int TRIANGLE = 1;
    private float triangleFlyingSpeed = 75;

    private final int type = TRIANGLE;

    private final int fftSize = 512;

    private OrthographicCamera camera;
    private Viewport viewport;

    FFTScreen() {

        camera = new OrthographicCamera(1600, 900);
        viewport = new ScreenViewport(camera);

        fft = new FloatFFT_1D(fftSize);
        musicWave = new MusicWave();

        averageSamplesNormalised = musicWave.smoothSamples(musicWave.getSamples(), 2, 32);

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        displaySamples = new float[fftSize];

        Arrays.fill(displaySamples, 0);

        littleTriangles = new Array<>();
        littleTrianglesSpeeds = new Array<>();
        littleTrianglesColors = new Array<>();

        utils = new Utils(FPS, step, musicWave.smoothSamples(musicWave.getSamples(), 2, 32), 3, 1, 1, true);

        music = musicWave.getMusic();
        if(!render) {
            music.play();
        }else{
            triangleFlyingSpeed *= 1.7f;
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int pos;
        if (render) {
            frame += step;
            recorderFrame++;
            pos = frame;
        } else {
            pos = (int) (music.getPosition() * 44100);
        }

        float[] samples = musicWave.getSamplesForFFT(pos, fftSize);
        fft.realForward(samples);

        samples[0] = (samples[1] + samples[2]) / 2f;
        for (int t = 0; t < 2; t++) {
            for (int i = 2; i < samples.length - 2; i++) {
                float neighbours = samples[i - 2] + samples[i + 2] + samples[i - 1] + samples[i + 1];
                samples[i] = (Math.abs(neighbours) + Math.abs(samples[i])) / 5f;
            }
        }

        displayFFT(samples, pos);

        if (render) {
            utils.makeAScreenShot(recorderFrame);
            utils.displayData(recorderFrame, frame);
        }

    }

    public void displayFFT(float[] samples, int pos) {

        utils.bloomBegin(true, pos);

        float step = WIDTH / (float) fftSize;
        float L = (float) ((fftSize) * Math.sqrt(3d));
        float triangleStep = WIDTH / L - .07f;

        renderer.setProjectionMatrix(camera.combined);

        utils.bloomBegin(true, pos);
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        switch (type) {
            case (DEFAULT):

                for (int i = 0; i < fftSize - 2; i++) {
                    int index = i + 2;
                    displaySamples[i] += samples[index] / 16;

                    renderer.setColor(new Color().fromHsv(displaySamples[i] / 2048, 0.75f, 0.9f));
                    renderer.rect(-i * step, 0, step, displaySamples[i] / 512);
                    renderer.rect(+i * step, 0, step, displaySamples[i] / 512);

                    renderer.setColor(new Color().fromHsv(-displaySamples[i] / 2048, 0.75f, 0.9f));
                    renderer.rect(-i * step, 0, step, -displaySamples[i] / 512);
                    renderer.rect(+i * step, 0, step, -displaySamples[i] / 512);

                    displaySamples[i] /= 2f;
                }
                break;

            case (TRIANGLE):

                for (int i = 0; i < littleTriangles.size; i++) {

                    float[] triangle = calculateTriangle(littleTriangles.get(i).x, littleTriangles.get(i).y, littleTriangles.get(i).z, -30);

                    renderer.setColor(littleTrianglesColors.get(i));
                    renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);

                    float xSpeed = littleTrianglesSpeeds.get(i).x * averageSamplesNormalised[pos]*triangleFlyingSpeed;
                    float ySpeed = littleTrianglesSpeeds.get(i).y * averageSamplesNormalised[pos]*triangleFlyingSpeed;

                    littleTriangles.set(i, littleTriangles.get(i).add(xSpeed, ySpeed, 0.7f));

                    if (littleTriangles.get(i).z > 30 || Math.abs(littleTriangles.get(i).x) > WIDTH + 10 || Math.abs(littleTriangles.get(i).y) > HEIGHT + 10) {
                        littleTriangles.removeIndex(i);
                        littleTrianglesSpeeds.removeIndex(i);
                        littleTrianglesColors.removeIndex(i);
                    }
                }

                for (int i = 0; i < averageSamplesNormalised[pos] * 50; i++) {

                    float x = getRandomInRange(-WIDTH, WIDTH);
                    float y = getRandomInRange(-HEIGHT, HEIGHT);

                    float max = Math.max(Math.abs(x), Math.abs(y));

                    littleTriangles.add(new Vector3(x, y, 0));
                    littleTrianglesSpeeds.add(new Vector2(x/max, y/max));
                    littleTrianglesColors.add(new Color().fromHsv(averageSamplesNormalised[pos]*120-60, 0.75f, 0.9f));
                }

                for (int i = 0; i < fftSize - 2; i++) {
                    float[] triangle = calculateTriangle(0, 0, fftSize - i, -30);

                    renderer.setColor(new Color().fromHsv(displaySamples[i] / 256, 0.75f, 0.9f));
                    renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
                }

                for (int i = 0; i < fftSize - 2; i++) {
                    displaySamples[i] += samples[i + 2] / 4;
                }

                renderFFTForTriangle(triangleStep, L);
                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, 120));
                renderFFTForTriangle(triangleStep, L);
                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, -120));
                renderFFTForTriangle(triangleStep, L);

                for (int i = 0; i < fftSize - 2; i++) {
                    displaySamples[i] /= 2f;
                }

                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, 0));

                break;

        }
        renderer.end();
        utils.bloomRender();
    }

    private float[] calculateTriangle(float x, float y, float size, float dOffset) {
        float x1 = -MathUtils.cosDeg(dOffset) * size + x;
        float y1 = -MathUtils.sinDeg(dOffset) * size + y;

        float x2 = -MathUtils.cosDeg(120 + dOffset) * size + x;
        float y2 = -MathUtils.sinDeg(120 + dOffset) * size + y;

        float x3 = -MathUtils.cosDeg(240 + dOffset) * size + x;
        float y3 = -MathUtils.sinDeg(240 + dOffset) * size + y;
        return new float[]{x1, y1, x2, y2, x3, y3};
    }

    private void renderFFTForTriangle(float triangleStep, float L) {
        for (int i = 0; i < fftSize - 2; i++) {
            renderer.setColor(new Color().fromHsv(displaySamples[i] / 2048 - 60, 0.75f, 0.9f));
            renderer.rect(i * triangleStep - L / 2f, 256, triangleStep, displaySamples[i] / 512);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(0, -90, 0);
        float tempScaleH = height / 900f;
        float tempScaleW = width / 1600f;
        float zoom = Math.min(tempScaleH, tempScaleW);
        camera.zoom = 1 / zoom + 0.5f;
        camera.update();
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
