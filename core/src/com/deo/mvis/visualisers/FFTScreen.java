package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.jtransforms.fft.FloatFFT_1D;
import com.deo.mvis.utils.GradientShape;
import com.deo.mvis.utils.SettingsArray;
import com.deo.mvis.utils.SyncedWord;

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
    private static float waterfallColorAmplitude = 1;
    private static float waterfallColorShift = 0;
    private static float baseRadius = 0;

    private static boolean displayLyrics = false;
    private boolean lyricsAvailable = false;
    private Array<SyncedWord> songWords;

    private static int type;
    private static int palette;

    private final int FIRE = 1;
    private final int BANANA = 2;
    private final int GRASS = 3;

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

        songWords = new Array<>();

        if (Gdx.files.external("Mvis/" + musicFile.nameWithoutExtension() + ".txt").exists() && displayLyrics) {
            JsonValue lyricsJson = new JsonReader().parse(Gdx.files.external("Mvis/" + musicFile.nameWithoutExtension() + ".txt").readString());
            for (int i = 0; i < lyricsJson.size - 1; i++) {
                songWords.add(new SyncedWord(-WIDTH / 2f, 300, Integer.parseInt(lyricsJson.get(i).name), Integer.parseInt(lyricsJson.get(i + 1).name), lyricsJson.get(i).asString(), new BitmapFont(Gdx.files.internal("font2(old).fnt"))));
            }
            lyricsAvailable = true;
        }

        switch (palette) {
            case (FIRE):
                colorShift = 14.21f;
                colorShift2 = 18.947f;
                colorAmplitude = 3;
                waterfallColorAmplitude = 7;
                waterfallColorShift = 7.895f;
                break;
            case (BANANA):
                colorShift = 164.2f;
                colorShift2 = 97.9f;
                colorAmplitude = 1.79f;
                waterfallColorAmplitude = 7;
                waterfallColorShift = 140;
                break;
            case (GRASS):
                colorShift = 143.7f;
                colorShift2 = 33.1f;
                colorAmplitude = 1.79f;
                waterfallColorAmplitude = 7;
                waterfallColorShift = 66.3f;
                break;

        }

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
                float neighbours = Math.abs(samples[i - 2]) + Math.abs(samples[i + 2]) + Math.abs(samples[i - 1]) + Math.abs(samples[i + 1]);
                samples[i] = (neighbours + Math.abs(samples[i])) / 5f;
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
        batch.setProjectionMatrix(camera.combined);

        utils.bloomBegin(true, pos);

        if (!(type == DEFAULT && waterfall)) {
            if (!outline) {
                renderer.begin(ShapeRenderer.ShapeType.Filled);
            } else {
                renderer.begin();
            }
        }

        float delta;

        if (render) {
            delta = 1 / (float) FPS;
        } else {
            delta = Gdx.graphics.getDeltaTime();
        }

        batch.begin();
        if (lyricsAvailable) {
            for (int i = 0; i < songWords.size; i++) {
                songWords.get(i).drawAndUpdate(music.getPosition(), batch, new Color().fromHsv(displaySamples[0] / 2048 * colorAmplitude + colorShift + waterfallColorShift, 0.75f, 1));
            }
        }
        batch.end();

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
                            float colorHSV = -displaySamples[i] / 2048 * waterfallColorAmplitude + colorShift - waterfallColorShift;
                            if (invertColors) {
                                colorHSV = displaySamples[i] / 2048 * waterfallColorAmplitude + colorShift + waterfallColorShift;
                            }
                            glassShards.add(new GradientShape().buildGradientPolygon(displaySamples[i] / (2024 - maxRadius * 100) + baseRadius, gradientSteps, 90, -i * step + step / 2f, 0, faces, 0, new Color().fromHsv(colorHSV, 0.75f, 1), Color.CLEAR, 1 / (samplesSmoothed[pos] + 0.5f)));
                            glassShards.add(new GradientShape().buildGradientPolygon(displaySamples[i] / (2024 - maxRadius * 100) + baseRadius, gradientSteps, 90, i * step + step / 2f, 0, faces, 0, new Color().fromHsv(colorHSV, 0.75f, 1), Color.CLEAR, 1 / (samplesSmoothed[pos] + 0.5f)));
                            shardTimers[i] = minSpawnDelay * delta;
                        }

                        shardTimers[i] -= delta;

                    }

                    int index = i + 5;
                    displaySamples[i] += samples[index] / 16 * (i * 0.01 + 1);

                    renderer.setColor(new Color().fromHsv(MathUtils.clamp(displaySamples[i] / 2048 * colorAmplitude, 0, 130) + colorShift + colorShift2, 0.75f, 0.9f));
                    renderer.rect(-i * step, 0, step, displaySamples[i] / 1024 * fftHeight + 0.5f);
                    renderer.rect(+i * step, 0, step, displaySamples[i] / 1024 * fftHeight + 0.5f);

                    renderer.setColor(new Color().fromHsv(-MathUtils.clamp(displaySamples[i] / 2048 * colorAmplitude, 0, 130) + colorShift - colorShift2, 0.75f, 0.9f));
                    renderer.rect(-i * step, 0, step, -displaySamples[i] / 1024 * fftHeight + 0.5f);
                    renderer.rect(+i * step, 0, step, -displaySamples[i] / 1024 * fftHeight + 0.5f);

                    displaySamples[i] /= 1.7f;
                }

                break;

            case (TRIANGLE):

                for (int i = 0; i < littleTriangles.size; i++) {

                    float[] triangle = calculatePolygon(littleTriangles.get(i).x, littleTriangles.get(i).y, littleTriangles.get(i).z, -30, 3, 0);

                    renderer.setColor(littleTrianglesColors.get(i));
                    renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);

                    float xSpeed = littleTrianglesSpeeds.get(i).x * samplesSmoothed[pos] * triangleFlyingSpeed * delta * 30;
                    float ySpeed = littleTrianglesSpeeds.get(i).y * samplesSmoothed[pos] * triangleFlyingSpeed * delta * 30;

                    littleTriangles.set(i, littleTriangles.get(i).add(xSpeed, ySpeed, 37 * delta * (triangleFlyingSpeed / 49)));

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
                    displaySamples[i] += samples[i + 5] / 16 * (i * 0.01 + 1);
                }

                renderFFTForTriangle(triangleStep, L);
                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, 120));
                renderFFTForTriangle(triangleStep, L);
                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, -120));
                renderFFTForTriangle(triangleStep, L);

                for (int i = 0; i < fftSize - 5; i++) {
                    displaySamples[i] /= 1.7f;
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
        initialiseArrays();
        paletteNames = new String[]{"Default", "Chemical fire", "Purple banana", "Frozen grass"};
        typeNames = new String[]{"Basic", "Triangle"};

        addSetting("Type", "int", 0.0f, 1.0f, 0.0f);
        addSetting("Palette", "int", 0.0f, 3.0f, 0.0f);
        addSetting("Triangle flying speed", "float", 0.0f, 200.0f, 75.0f);
        addSetting("Max fft height", "float", 1.0f, 4.0f, 1.0f);
        addSetting("Color shift", "float", 0.0f, 180.0f, 0.0f);
        addSetting("Color difference", "float", 0.0f, 180.0f, 0.0f);
        addSetting("Color amplitude", "float", 1.0f, 7.0f, 1.0f);
        addSetting("Outline", "boolean", 0.0f, 1.0f, 0.0f);
        addSetting("Waterfall", "boolean", 0.0f, 1.0f, 0.0f);
        addSetting("Number of holes", "int", 1.0f, 25.0f, 11.0f);
        addSetting("Faces", "int", 3.0f, 15.0f, 6.0f);
        addSetting("Base radius", "float", 0.0f, 10.0f, 0.0f);
        addSetting("Max radius", "float", 0.0f, 20.2f, 5.2f);
        addSetting("Flying Speed", "float", 5.0f, 50.0f, 35.0f);
        addSetting("Gradient steps", "int", 1.0f, 15.0f, 5.0f);
        addSetting("Spawn threshold", "float", 0.0f, 60.0f, 30.7f);
        addSetting("Min spawn delay", "float", 0.0f, 30.0f, 10.0f);
        addSetting("Waterfall color amplitude", "float", 1.0f, 17.0f, 1.0f);
        addSetting("Waterfall color shift", "float", 0.0f, 180.0f, 0.0f);
        addSetting("Invert colors", "boolean", 0.0f, 1.0f, 1.0f);
        addSetting("Display lyrics", "boolean", 0.0f, 1.0f, 0.0f);
        addSetting("Render", "boolean", 0.0f, 1.0f, 0.0f);

    }

    public static String getName() {
        return "Frequency spectrum";
    }

    public static void setSettings(float[] newSettings) {
        migrateSettings(newSettings);
        type = (int) settings.getSettingByName("Type");
        palette = (int) settings.getSettingByName("Palette");
        triangleFlyingSpeed = settings.getSettingByName("Triangle flying speed");
        fftHeight = settings.getSettingByName("Max fft height");
        colorShift = settings.getSettingByName("Color shift");
        colorShift2 = settings.getSettingByName("Color difference");
        colorAmplitude = settings.getSettingByName("Color amplitude") / 2f;
        outline = settings.getSettingByName("Outline") > 0;
        waterfall = settings.getSettingByName("Waterfall") > 0;
        numOfHoles = (int) settings.getSettingByName("Number of holes");
        faces = (int) settings.getSettingByName("Faces");
        baseRadius = settings.getSettingByName("Base radius");
        maxRadius = settings.getSettingByName("Max radius");
        flyingSpeed = settings.getSettingByName("Flying Speed");
        gradientSteps = (int) settings.getSettingByName("Gradient steps");
        spawnThreshold = settings.getSettingByName("Spawn threshold");
        minSpawnDelay = settings.getSettingByName("Min spawn delay");
        waterfallColorAmplitude = settings.getSettingByName("Waterfall color amplitude");
        waterfallColorShift = settings.getSettingByName("Waterfall color shift");
        invertColors = settings.getSettingByName("Invert colors") > 0;
        displayLyrics = settings.getSettingByName("Display lyrics") > 0;
        render = settings.getSettingByName("Render") > 0;
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
        for (int i = 0; i < songWords.size; i++) {
            songWords.get(i).dispose();
        }
    }
}
