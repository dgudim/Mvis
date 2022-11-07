package com.deo.mvis.visualisers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
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
import com.deo.mvis.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;
import static com.deo.mvis.utils.Utils.getRandomInRange;

public class FFTScreen extends BaseVisualiser implements Screen {

    private FloatFFT_1D fft;
    private float[] displaySamples;

    private Array<Vector3> littleTriangles;
    private Array<Vector2> littleTrianglesSpeeds;
    private Array<Color> littleTrianglesColors;

    private final int DEFAULT = 0;
    private final int TRIANGLE = 1;
    private static float triangleFlyingSpeed = 75;

    private static int type;
    private static int palette;

    private final int fftSize = 512;

    public FFTScreen() {

        camera = new OrthographicCamera(1600, 900);
        viewport = new ScreenViewport(camera);

        fft = new FloatFFT_1D(fftSize);

        displaySamples = new float[fftSize];
        Arrays.fill(displaySamples, 0);

        littleTriangles = new Array<>();
        littleTrianglesSpeeds = new Array<>();
        littleTrianglesColors = new Array<>();

        if(render) {
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

        samples[samples.length-1] = Math.abs(samples[samples.length-1]);
        samples[samples.length-2] = Math.abs(samples[samples.length-2]);

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

                    float xSpeed = littleTrianglesSpeeds.get(i).x * samplesSmoothed[pos]*triangleFlyingSpeed;
                    float ySpeed = littleTrianglesSpeeds.get(i).y * samplesSmoothed[pos]*triangleFlyingSpeed;

                    littleTriangles.set(i, littleTriangles.get(i).add(xSpeed, ySpeed, 0.7f));

                    if (littleTriangles.get(i).z > 30) {
                        littleTriangles.removeIndex(i);
                        littleTrianglesSpeeds.removeIndex(i);
                        littleTrianglesColors.removeIndex(i);
                    }
                }

                for (int i = 0; i < samplesRaw[pos] * 50; i++) {

                    float x = getRandomInRange(-WIDTH, WIDTH);
                    float y = getRandomInRange(-HEIGHT, HEIGHT);

                    float max = Math.max(Math.abs(x), Math.abs(y));

                    littleTriangles.add(new Vector3(x, y, 0));
                    littleTrianglesSpeeds.add(new Vector2(x/max, y/max));
                    littleTrianglesColors.add(new Color().fromHsv(samplesRaw[pos]*120-60, 0.75f, 0.9f));
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

    public static void init() {
        paletteNames = new String[]{"Default"};
        typeNames = new String[]{"Basic", "Triangle"};

        settings = new String[]{"Type", "Pallet", "Triangle flying speed"};
        settingTypes = new String[]{"int", "int", "float"};

        settingMaxValues = new float[]{typeNames.length - 1, paletteNames.length - 1, 200};
        settingMinValues = new float[]{0, 0, 0};

        defaultSettings = new float[]{0, 0, 75};
    }

    public static String getName(){
        return "Frequency spectrum";
    }

    public static void setSettings(float[] newSettings) {
        type = (int) newSettings[0];
        palette = (int) newSettings[1];
        triangleFlyingSpeed = newSettings[2];
    }

    @Override
    public void resize(int width, int height) {
        if(type == TRIANGLE) {
            super.resize(width, height, -90, false);
        }else{
            super.resize(width, height, 0, true);
        }
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
        super.dispose();
    }
}
