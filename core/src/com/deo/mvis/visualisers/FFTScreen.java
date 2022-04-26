package com.deo.mvis.visualisers;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;
import static com.deo.mvis.utils.GradientShape.calculatePolygon;
import static com.deo.mvis.utils.Utils.getRandomInRange;

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
import com.deo.mvis.jtransforms.fft.FloatFFT_1D;
import com.deo.mvis.utils.CompositeSettings;
import com.deo.mvis.utils.GradientShape;
import com.deo.mvis.utils.SettingsEntry;
import com.deo.mvis.utils.SyncedWord;
import com.deo.mvis.utils.Type;

public class FFTScreen extends BaseVisualiser implements Screen {
    
    private final FloatFFT_1D fft;
    private final float[] displaySamples;
    private final int fftSize = 512;
    private final float FFTStep = WIDTH / (float) fftSize / 2f;
    
    float triangleFFTSize = (float) (fftSize * Math.sqrt(3));
    float triangleStep = WIDTH / triangleFFTSize - .07f;
    
    private final Array<GradientShape> glassShards;
    private final float[] shardTimers;
    
    private final Array<Vector3> littleTriangles;
    private final Array<Vector2> littleTrianglesSpeeds;
    private final Array<Color> littleTrianglesColors;
    
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
    private final Array<SyncedWord> songWords;
    
    private static Mode mode;
    private static Palette palette;
    
    private enum Palette {
        FIRE, BANANA, GRASS
    }
    
    private enum Mode {
        BASIC, ELLIPSE, TRIANGLE
    }
    
    public FFTScreen(Game game) {
        super(game, FFT_AND_RAW);
        
        fft = new FloatFFT_1D(fftSize);
        
        displaySamples = new float[fftSize];
        shardTimers = new float[fftSize];
        
        littleTriangles = new Array<>();
        littleTrianglesSpeeds = new Array<>();
        littleTrianglesColors = new Array<>();
        glassShards = new Array<>();
        
        songWords = new Array<>();
        
        if (Gdx.files.external("Mvis/" + musicFile.nameWithoutExtension() + ".txt").exists() && displayLyrics) {
            JsonValue lyricsJson = new JsonReader().parse(Gdx.files.external("Mvis/" + musicFile.nameWithoutExtension() + ".txt").readString());
            for (int i = 0; i < lyricsJson.size - 1; i++) {
                songWords.add(new SyncedWord(-WIDTH / 2f, 300, Integer.parseInt(lyricsJson.get(i).name), Integer.parseInt(lyricsJson.get(i + 1).name), lyricsJson.get(i).asString(), new BitmapFont(Gdx.files.internal("font2(old).fnt")), 10));
            }
            lyricsAvailable = true;
        }
        
        switch (palette) {
            case FIRE:
            default:
                colorShift = 14.21f;
                colorShift2 = 18.947f;
                colorAmplitude = 3;
                waterfallColorAmplitude = 7;
                waterfallColorShift = 7.895f;
                break;
            case BANANA:
                colorShift = 164.2f;
                colorShift2 = 97.9f;
                colorAmplitude = 1.79f;
                waterfallColorAmplitude = 7;
                waterfallColorShift = 140;
                break;
            case GRASS:
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
            frame += sampleStep;
            recorderFrame++;
            pos = frame;
        } else {
            pos = (int) (music.getPosition() * sampleRate);
        }
        
        displayFFT(musicWave.getSmoothedFFT(pos, fftSize, samplesForFFT, 5, fft), pos);
        
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
        
        renderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        
        utils.bloomBegin(true, pos);
        
        if (!(mode == Mode.BASIC && waterfall)) {
            renderer.begin(outline ? ShapeRenderer.ShapeType.Line : ShapeRenderer.ShapeType.Filled);
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
        
        switch (mode) {
            case BASIC:
            case ELLIPSE:
            default:
                
                if (waterfall) {
                    renderer.begin();
                    for (int i2 = 0; i2 < glassShards.size; i2++) {
                        glassShards.get(i2).draw(renderer, 1 / (samplesNormalizedSmoothed[pos] + 0.5f));
                        glassShards.get(i2).y -= flyingSpeed * 10 * delta;
                        if (glassShards.get(i2).y < -HEIGHT / 2f - glassShards.get(i2).radius) {
                            glassShards.removeIndex(i2);
                        }
                    }
                    renderer.end();
                    renderer.begin(outline ? ShapeRenderer.ShapeType.Line : ShapeRenderer.ShapeType.Filled);
                }
                
                musicWave.accumulate(samples, displaySamples, 0.01f, 16, 1.7f, (samples1, i) -> {
                    if (waterfall) {
                        if (samples1[i] > spawnThreshold * 100 && shardTimers[i] <= 0 && i % numOfHoles == 0) {
                            float colorHSV = -samples1[i] / 2048 * waterfallColorAmplitude + colorShift - waterfallColorShift;
                            if (invertColors) {
                                colorHSV = samples1[i] / 2048 * waterfallColorAmplitude + colorShift + waterfallColorShift;
                            }
                            glassShards.add(new GradientShape().buildGradientPolygon(samples1[i] / (2024 - maxRadius * 100) + baseRadius, gradientSteps, 90, -i * FFTStep + FFTStep / 2f, 0, faces, 0, new Color().fromHsv(colorHSV, 0.75f, 1), Color.CLEAR, 1 / (samplesNormalizedSmoothed[pos] + 0.5f)));
                            glassShards.add(new GradientShape().buildGradientPolygon(samples1[i] / (2024 - maxRadius * 100) + baseRadius, gradientSteps, 90, i * FFTStep + FFTStep / 2f, 0, faces, 0, new Color().fromHsv(colorHSV, 0.75f, 1), Color.CLEAR, 1 / (samplesNormalizedSmoothed[pos] + 0.5f)));
                            shardTimers[i] = minSpawnDelay * delta;
                        }
                        
                        shardTimers[i] -= delta;
                        
                    }
                    
                    renderer.setColor(new Color().fromHsv(MathUtils.clamp(samples1[i] / 2048 * colorAmplitude, 0, 130) + colorShift + colorShift2, 0.75f, 0.9f));
                    float height = samples1[i] / 1024 * fftHeight + 0.5f;
                    if (mode == Mode.BASIC) {
                        renderer.rect(-i * FFTStep, -height / 2, FFTStep, height);
                        renderer.rect(+i * FFTStep, -height / 2, FFTStep, height);
                    } else {
                        renderer.ellipse(i * FFTStep, -height / 2, FFTStep * 16, height, 0, 20);
                        renderer.ellipse(-i * FFTStep, -height / 2, FFTStep * 16, height, 0, 20);
                    }
                });
                break;
            
            case TRIANGLE:
                
                for (int i = 0; i < littleTriangles.size; i++) {
                    
                    float[] triangle = calculatePolygon(littleTriangles.get(i).x, littleTriangles.get(i).y, littleTriangles.get(i).z, -30, 3, 0);
                    
                    renderer.setColor(littleTrianglesColors.get(i));
                    renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
                    
                    float xSpeed = littleTrianglesSpeeds.get(i).x * samplesNormalizedSmoothed[pos] * triangleFlyingSpeed * delta * 30;
                    float ySpeed = littleTrianglesSpeeds.get(i).y * samplesNormalizedSmoothed[pos] * triangleFlyingSpeed * delta * 30;
                    
                    littleTriangles.set(i, littleTriangles.get(i).add(xSpeed, ySpeed, 37 * delta * (triangleFlyingSpeed / 49)));
                    
                    if (littleTriangles.get(i).z > 30) {
                        littleTriangles.removeIndex(i);
                        littleTrianglesSpeeds.removeIndex(i);
                        littleTrianglesColors.removeIndex(i);
                    }
                }
                
                for (int i = 0; i < samplesNormalizedRaw[pos] * 50; i++) {
                    
                    float x = getRandomInRange(-WIDTH, WIDTH);
                    float y = getRandomInRange(-HEIGHT, HEIGHT);
                    
                    float max = Math.max(Math.abs(x), Math.abs(y));
                    
                    littleTriangles.add(new Vector3(x, y, 0));
                    littleTrianglesSpeeds.add(new Vector2(x / max, y / max));
                    littleTrianglesColors.add(new Color().fromHsv(samplesNormalizedRaw[pos] * 120 - 60, 0.75f, 0.9f));
                }
                
                for (int i = 0; i < fftSize; i++) {
                    float[] triangle = calculatePolygon(0, 0, fftSize - i + 5, -30, 3, 0);
                    
                    renderer.setColor(new Color().fromHsv(displaySamples[i] / 256 * colorAmplitude + colorShift - colorShift2, 0.75f, 0.9f));
                    renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
                    displaySamples[i] += samples[i] / 16 * (i * 0.01 + 1);
                }
                
                renderFFTForTriangle(triangleStep, triangleFFTSize);
                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, 120));
                renderFFTForTriangle(triangleStep, triangleFFTSize);
                renderer.setTransformMatrix(new Matrix4().rotate(0, 0, 1, -120));
                renderFFTForTriangle(triangleStep, triangleFFTSize);
                
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
    
    public static CompositeSettings init() {
        
        CompositeSettings compositeSettings = new CompositeSettings(enumToArray(Palette.class), enumToArray(Mode.class));
        
        compositeSettings.addSetting("Triangle flying speed", 0, 200, 75, Type.FLOAT);
        compositeSettings.addSetting("Max fft height", 1, 4, 1, Type.FLOAT);
        compositeSettings.addSetting("Color shift", 0, 180, 0, Type.FLOAT);
        compositeSettings.addSetting("Color difference", 0, 180, 0, Type.FLOAT);
        compositeSettings.addSetting("Color amplitude", 1, 7, 1, Type.FLOAT);
        compositeSettings.addSetting("Outline", 0, 1, 0, Type.BOOLEAN);
        compositeSettings.addSetting("Waterfall", 0, 1, 0, Type.BOOLEAN);
        compositeSettings.addSetting("Number of holes", 1, 25, 11, Type.INT);
        compositeSettings.addSetting("Faces", 3, 15, 6, Type.INT);
        compositeSettings.addSetting("Base radius", 0, 10, 0, Type.FLOAT);
        compositeSettings.addSetting("Max radius", 0, 20.2f, 5.2f, Type.FLOAT);
        compositeSettings.addSetting("Flying Speed", 5, 50, 35, Type.FLOAT);
        compositeSettings.addSetting("Gradient steps", 1, 15, 5, Type.INT);
        compositeSettings.addSetting("Spawn threshold", 0, 60, 30.7f, Type.FLOAT);
        compositeSettings.addSetting("Min spawn delay", 0, 30, 10, Type.FLOAT);
        compositeSettings.addSetting("Waterfall color amplitude", 1, 17, 1, Type.FLOAT);
        compositeSettings.addSetting("Waterfall color shift", 0, 180, 0, Type.FLOAT);
        compositeSettings.addSetting("Invert colors", 0, 1, 1, Type.BOOLEAN);
        compositeSettings.addSetting("Display lyrics", 0, 1, 0, Type.BOOLEAN);
        compositeSettings.addSetting("Render", 0, 1, 0, Type.BOOLEAN);
        
        return compositeSettings;
    }
    
    public static String getName() {
        return "Frequency spectrum";
    }
    
    public static void setSettings(Array<SettingsEntry> settings, int mode, int palette) {
        FFTScreen.mode = Mode.values()[mode];
        FFTScreen.palette = Palette.values()[palette];
        triangleFlyingSpeed = getSettingByName(settings, "Triangle flying speed");
        fftHeight = getSettingByName(settings, "Max fft height");
        colorShift = getSettingByName(settings, "Color shift");
        colorShift2 = getSettingByName(settings, "Color difference");
        colorAmplitude = getSettingByName(settings, "Color amplitude") / 2f;
        outline = getSettingByName(settings, "Outline") > 0;
        waterfall = getSettingByName(settings, "Waterfall") > 0;
        numOfHoles = (int) getSettingByName(settings, "Number of holes");
        faces = (int) getSettingByName(settings, "Faces");
        baseRadius = getSettingByName(settings, "Base radius");
        maxRadius = getSettingByName(settings, "Max radius");
        flyingSpeed = getSettingByName(settings, "Flying Speed");
        gradientSteps = (int) getSettingByName(settings, "Gradient steps");
        spawnThreshold = getSettingByName(settings, "Spawn threshold");
        minSpawnDelay = getSettingByName(settings, "Min spawn delay");
        waterfallColorAmplitude = getSettingByName(settings, "Waterfall color amplitude");
        waterfallColorShift = getSettingByName(settings, "Waterfall color shift");
        invertColors = getSettingByName(settings, "Invert colors") > 0;
        displayLyrics = getSettingByName(settings, "Display lyrics") > 0;
        render = getSettingByName(settings, "Render") > 0;
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height, mode == Mode.TRIANGLE ? -90 : 0, mode != Mode.TRIANGLE);
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
