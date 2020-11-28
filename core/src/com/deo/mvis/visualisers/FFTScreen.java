package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
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
import com.deo.mvis.utils.GradientShape;

import java.util.Arrays;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;
import static com.deo.mvis.utils.GradientShape.calculatePolygon;
import static com.deo.mvis.utils.Utils.getRandomInRange;

public class FFTScreen extends BaseVisualiser implements Screen {

    private FloatFFT_1D fft;
    private float[] displaySamples;
    private float[] shardTimers;

    private Array<GradientShape> glassShards;

    private Array<Vector3> littleTriangles;
    private Array<Vector2> littleTrianglesSpeeds;
    private Array<Color> littleTrianglesColors;

    private final int DEFAULT = 0;
    private static final int TRIANGLE = 1;
    private static float triangleFlyingSpeed = 75;
    private static float fftHeight = 1;
    private static float colorAmplitude = 1;
    private static float colorShift = 0;
    private static float colorShift2 = 0;
    private static boolean outline = false;
    private static boolean waterfall = false;
    private static int numOfHoles = 11;
    private static int faces = 6;
    private static float maxRadius = 5.2f;
    private static float flyingSpeed = 35;
    private static int gradientSteps = 5;
    private static float spawnThreshold = 30.7f;
    private static float minSpawnDelay = 10;
    private static boolean invertColors = false;

    private static int type;
    private static int palette;

    private final int fftSize = 512;

    public FFTScreen(Game game) {
        super(game, new boolean[]{type == TRIANGLE, false, false});

        fft = new FloatFFT_1D(fftSize);

        displaySamples = new float[fftSize];
        shardTimers = new float[fftSize];
        Arrays.fill(displaySamples, 0);
        Arrays.fill(shardTimers, 0);

        littleTriangles = new Array<>();
        littleTrianglesSpeeds = new Array<>();
        littleTrianglesColors = new Array<>();
        glassShards = new Array<>();

        if (render) {
            triangleFlyingSpeed *= 1.7f;
        }
    }

    @Override
    public void show() {
        super.show();
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
            pos = (int) (music.getPosition() * sampleRate);
        }

        float[] samples = musicWave.getSamplesForFFT(pos, fftSize);
        fft.realForward(samples);

        for (int t = 0; t < 2; t++) {
            for (int i = 2; i < samples.length - 2; i++) {
                float neighbours = samples[i - 2] + samples[i + 2] + samples[i - 1] + samples[i + 1];
                samples[i] = (Math.abs(neighbours) + Math.abs(samples[i])) / 5f;
            }
        }

        samples[samples.length - 1] = Math.abs(samples[samples.length - 1]);
        samples[samples.length - 2] = Math.abs(samples[samples.length - 2]);

        displayFFT(samples, pos);

        if (render) {
            utils.makeAScreenShot(recorderFrame);
            utils.displayData(recorderFrame, frame, camera.combined);
        }

        batch.begin();
        drawExitButton();
        batch.end();

    }

    public void displayFFT(float[] samples, int pos) {

        utils.bloomBegin(true, pos);

        float step = WIDTH / (float) fftSize / 2f;
        float L = (float) ((fftSize) * Math.sqrt(3d));
        float triangleStep = WIDTH / L - .07f;

        renderer.setProjectionMatrix(camera.combined);

        utils.bloomBegin(true, pos);

        if(!(type == DEFAULT && waterfall)) {
            if (!outline) {
                renderer.begin(ShapeRenderer.ShapeType.Filled);
            } else {
                renderer.begin();
            }
        }

        float delta;

        if(render){
            delta = 1/(float)FPS;
        }else{
            delta = Gdx.graphics.getDeltaTime();
        }

        switch (type) {
            case (DEFAULT):

                if (waterfall) {
                    renderer.begin();
                    for (int i2 = 0; i2 < glassShards.size; i2++) {
                        glassShards.get(i2).draw(renderer, 1 / (samplesSmoothed[pos] + 0.5f));
                        glassShards.get(i2).y -= flyingSpeed * 10 * delta;
                        if (glassShards.get(i2).y < -HEIGHT / 2f - glassShards.get(i2).radius) {
                            glassShards.removeIndex(i2);
                        }
                    }
                    renderer.end();
                    if (!outline) {
                        renderer.begin(ShapeRenderer.ShapeType.Filled);
                    } else {
                        renderer.begin();
                    }
                }

                for (int i = 0; i < fftSize - 5; i++) {

                    if (waterfall) {
                        if (displaySamples[i] > spawnThreshold * 100 && shardTimers[i] <= 0 && i % numOfHoles == 0) {
                            float colorHSV = -displaySamples[i] / 2048 * colorAmplitude + colorShift - colorShift2;
                            if(invertColors){
                                colorHSV = displaySamples[i] / 2048 * colorAmplitude + colorShift + colorShift2;
                            }
                            glassShards.add(new GradientShape().buildGradientPolygon(displaySamples[i] / (2024 - maxRadius * 100), gradientSteps, 90, -i * step + step / 2f, 0, faces, 0, new Color().fromHsv(colorHSV, 0.75f, 1), Color.CLEAR, 1 / (samplesSmoothed[pos] + 0.5f)));
                            glassShards.add(new GradientShape().buildGradientPolygon(displaySamples[i] / (2024 - maxRadius * 100), gradientSteps, 90, i * step + step / 2f, 0, faces, 0, new Color().fromHsv(colorHSV, 0.75f, 1), Color.CLEAR, 1 / (samplesSmoothed[pos] + 0.5f)));
                            shardTimers[i] = minSpawnDelay * delta;
                        }

                        shardTimers[i] -= delta;

                    }

                    int index = i + 5;
                    displaySamples[i] += samples[index] / 16;

                    renderer.setColor(new Color().fromHsv(displaySamples[i] / 2048 * colorAmplitude + colorShift + colorShift2, 0.75f, 0.9f));
                    renderer.rect(-i * step, 0, step, displaySamples[i] / 512 * fftHeight);
                    renderer.rect(+i * step, 0, step, displaySamples[i] / 512 * fftHeight);

                    renderer.setColor(new Color().fromHsv(-displaySamples[i] / 2048 * colorAmplitude + colorShift - colorShift2, 0.75f, 0.9f));
                    renderer.rect(-i * step, 0, step, -displaySamples[i] / 512 * fftHeight);
                    renderer.rect(+i * step, 0, step, -displaySamples[i] / 512 * fftHeight);

                    displaySamples[i] /= 2f;
                }

                break;

            case (TRIANGLE):

                for (int i = 0; i < littleTriangles.size; i++) {

                    float[] triangle = calculatePolygon(littleTriangles.get(i).x, littleTriangles.get(i).y, littleTriangles.get(i).z, -30, 3, 0);

                    renderer.setColor(littleTrianglesColors.get(i));
                    renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);

                    float xSpeed = littleTrianglesSpeeds.get(i).x * samplesSmoothed[pos] * triangleFlyingSpeed;
                    float ySpeed = littleTrianglesSpeeds.get(i).y * samplesSmoothed[pos] * triangleFlyingSpeed;

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
                    littleTrianglesSpeeds.add(new Vector2(x / max, y / max));
                    littleTrianglesColors.add(new Color().fromHsv(samplesRaw[pos] * 120 - 60, 0.75f, 0.9f));
                }

                for (int i = 0; i < fftSize - 5; i++) {
                    float[] triangle = calculatePolygon(0, 0, fftSize - i, -30, 3, 0);

                    renderer.setColor(new Color().fromHsv(displaySamples[i] / 256 * colorAmplitude + colorShift - colorShift2, 0.75f, 0.9f));
                    renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
                }

                for (int i = 0; i < fftSize - 5; i++) {
                    displaySamples[i] += samples[i + 5] / 4;
                }

                renderFFTForTriangle(triangleStep, L);
                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, 120));
                renderFFTForTriangle(triangleStep, L);
                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, -120));
                renderFFTForTriangle(triangleStep, L);

                for (int i = 0; i < fftSize - 5; i++) {
                    displaySamples[i] /= 2f;
                }

                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, 0));

                break;

        }
        renderer.end();
        utils.bloomRender();
    }

    private void renderFFTForTriangle(float triangleStep, float L) {
        for (int i = 0; i < fftSize - 5; i++) {
            renderer.setColor(new Color().fromHsv((displaySamples[i] / 2048 - 60) * colorAmplitude + colorShift - colorShift2, 0.75f, 0.9f));
            renderer.rect(i * triangleStep - L / 2f, 256, triangleStep, displaySamples[i] / 512 * fftHeight);
        }
    }

    public static void init() {
        paletteNames = new String[]{"Default"};
        typeNames = new String[]{"Basic", "Triangle"};

        settings = new String[]{"Type", "Pallet", "Triangle flying speed", "Max fft height", "Color shift", "Color difference", "Color amplitude",
                "Outline", "Waterfall", "Number of holes", "Faces", "Max radius", "Flying Speed", "Gradient steps", "Spawn threshold", "Min spawn delay", "Invert colors", "Render"};
        settingTypes = new String[]{"int", "int", "float", "float", "float", "float", "float",
                "boolean", "boolean", "int", "int", "float", "float", "int", "float", "float", "boolean", "boolean"};

        settingMaxValues = new float[]{typeNames.length - 1, paletteNames.length - 1, 200, 4, 180, 180, 7,
                1, 1, 25, 15, 20.2f, 50, 15, 60, 30, 1, 1};
        settingMinValues = new float[]{0, 0, 0, 1, 0, 0, 1,
                0, 0, 1, 3, 0, 5, 1, 10, 0, 0, 0};

        defaultSettings = new float[]{0, 0, 75, 1, 0, 0, 1,
                0, 0, 11, 6, 5.2f, 35, 5, 30.7f, 10, 1, 0};
    }

    public static String getName() {
        return "Frequency spectrum";
    }

    public static void setSettings(float[] newSettings) {
        type = (int) newSettings[0];
        palette = (int) newSettings[1];
        triangleFlyingSpeed = newSettings[2];
        fftHeight = newSettings[3];
        colorShift = newSettings[4];
        colorShift2 = newSettings[5];
        colorAmplitude = newSettings[6];
        outline = newSettings[7] > 0;
        waterfall = newSettings[8] > 0;
        numOfHoles = (int) newSettings[9];
        faces = (int) newSettings[10];
        maxRadius = newSettings[11];
        flyingSpeed = newSettings[12];
        gradientSteps = (int) newSettings[13];
        spawnThreshold = newSettings[14];
        minSpawnDelay = newSettings[15];
        invertColors = newSettings[16] > 0;
        render = newSettings[17] > 0;
    }

    @Override
    public void resize(int width, int height) {
        if (type == TRIANGLE) {
            super.resize(width, height, -90, false);
        } else {
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
