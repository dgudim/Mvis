package com.deo.mvis.visualisers;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;
import static com.deo.mvis.utils.ColorPallets.fadeBetweenTwoColors;
import static com.deo.mvis.utils.GradientShape.calculatePolygon;
import static com.deo.mvis.utils.Utils.getRandomInRange;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.mvis.jtransforms.fft.FloatFFT_1D;
import com.deo.mvis.utils.SettingsEntry;
import com.deo.mvis.utils.Type;

import java.util.Locale;

public class FourierScreen extends BaseVisualiser implements Screen {
    
    private final float[] radiuses;
    private final float defaultRadius;
    
    private final float[] currentAngles;
    private final float[] currentAngles2;
    private final float[] displaySamples;
    private final ShapeRenderer renderer;
    
    private static int numberOfLinks = 15;
    
    private final FloatFFT_1D fft;
    
    private final Array<Vector2> points;
    private final Array<Vector2> points2;
    private final Array<Vector3> fireShapes;
    private final Array<Color> fireColors;
    private final Array<Color> colors;
    private final Array<Color> colors2;
    
    private final Array<Vector3> triangles;
    private final Array<Vector3> starGateTriangles;
    private final Array<Vector3> starGateTriangleSpeeds;
    private final Array<Color> starGateTriangleColors;
    
    private static FourierScreen.Mode mode;
    private static FourierScreen.Palette palette;
    
    private enum Palette {
        COFFEE, LIME, GREEN, CYAN;
    }
    
    private enum Mode {
        BASIC
    }
    
    private static boolean changeBranchLength = false;
    private static boolean additionalRotation = true;
    private static boolean enableTriangles = true;
    private static float orbitAmplitude = 3;
    private static int triangleCount = 36;
    private static float triangleStep = 360 / (float) triangleCount;
    private static boolean enableWallFire = true;
    private static boolean enableStarGateEffect = false;
    private static boolean enableBottomFFT = true;
    private static boolean enableFlyingBalls = true;
    private static boolean enableFlyingBallsTrails = true;
    private static boolean syncColorOffsetToTheMusic = false;
    private static float syncedColorAmplitude = 50;
    private static float baseColorOffset = 0;
    private static float colorOffset = 0;
    private static float starGateSizeModifier = 1;
    private static float fftSizeModifier = 1;
    private static float triangleSizeModifier = 1;
    private static float flyingBallsSizeModifier = 1;
    float offsetAngle;
    
    public FourierScreen(Game game) {
        super(game, FFT_AND_RAW);
        //radiuses = new float[]{10, 5, 5, 6, 7, 12, 34, 5};
        //speeds = new float[]{10, 5, 4, 7, 8, 15, 5, 8};
        
        switch (palette) {
            case COFFEE:
                colorOffset = 35;
                break;
            case LIME:
                colorOffset = 80;
                break;
            case GREEN:
                colorOffset = 110;
                break;
            case CYAN:
                colorOffset = 190;
                break;
        }
        
        baseColorOffset = colorOffset;
        
        radiuses = new float[numberOfLinks];
        
        float maxReduction = Math.max(Math.max(fftSizeModifier, starGateSizeModifier), triangleSizeModifier);
        for (int i = 0; i < numberOfLinks; i++) {
            radiuses[i] = (HEIGHT - 30 * (maxReduction + flyingBallsSizeModifier + 3)) / (numberOfLinks * 2f);
        }
        
        defaultRadius = radiuses[0];
        
        currentAngles = new float[radiuses.length];
        currentAngles2 = new float[radiuses.length];
        
        points = new Array<>();
        points2 = new Array<>();
        colors = new Array<>();
        colors2 = new Array<>();
        triangles = new Array<>();
        starGateTriangles = new Array<>();
        starGateTriangleSpeeds = new Array<>();
        starGateTriangleColors = new Array<>();
        fireColors = new Array<>();
        fireShapes = new Array<>();
        
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        
        fft = new FloatFFT_1D(525);
        displaySamples = new float[1050];
        
        utils.setBloomIntensity(1);
        utils.maxSaturation = 1;
    }
    
    @Override
    public void show() {
        super.show();
    }
    
    @Override
    public void render(float delta) {
        
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);
        
        int pos = (int) (music.getPosition() * sampleRate);
        if (syncColorOffsetToTheMusic) {
            colorOffset = baseColorOffset + samplesSmoothed[pos] * syncedColorAmplitude;
        }
        
        float[] samples = musicWave.getSamplesForFFT(pos, 525, samplesForFFT);
        fft.realForward(samples);
        float[] newSamples = new float[numberOfLinks];
        for (int i = 0; i < numberOfLinks; i++) {
            float newSample = 0;
            for (int i2 = 0; i2 < 512 / (float) numberOfLinks; i2++) {
                newSample += Math.abs(samples[512 / numberOfLinks * i + i2]);
            }
            newSample /= (512 / (float) numberOfLinks);
            newSamples[i] = newSample * (i * 0.01f + 1);
        }
        
        for (int i = 0; i < samples.length; i++) {
            samples[i] *= (i * 0.01 + 1);
        }
        
        renderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        
        utils.bloomBegin(true, pos);
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // write with overhead
        float[] samplesToDisplay = new float[samples.length * 2];
        for (int i = 0; i < samples.length + 3; i++) {
            if (i < 525) {
                samplesToDisplay[i] = samples[samples.length - 1 - i];
            } else {
                samplesToDisplay[i] = samples[i - 525];
            }
        }
        
        //smooth
        for (int t = 0; t < 5; t++) {
            for (int i = 2; i < samples.length + 3; i++) {
                float neighbours = Math.abs(samplesToDisplay[i - 2]) + Math.abs(samplesToDisplay[i + 2]) + Math.abs(samplesToDisplay[i - 1]) + Math.abs(samplesToDisplay[i + 1]);
                samplesToDisplay[i] = (neighbours + Math.abs(samplesToDisplay[i])) / 5f;
            }
        }
        
        float[] verticalSamples = new float[samples.length / 15];
        for (int i = 0; i < samples.length / 15; i++) {
            float sample = 0;
            for (int i2 = 0; i2 < 15; i2++) {
                sample += samples[i * 15 + i2];
            }
            verticalSamples[i] = sample / 15f * (1 + 1.3f * i);
        }
        
        if (enableWallFire) {
            float verticalStep = HEIGHT / (float) verticalSamples.length + 0.2f;
            for (int i = 0; i < verticalSamples.length; i++) {
                fireColors.add(
                        fadeBetweenTwoColors(new Color().fromHsv(0.9f + colorOffset, 0.5882f, 1), new Color().fromHsv(0.1078f + colorOffset, 1, 1),
                                MathUtils.clamp(verticalSamples[0] / 500000f, 0, 1)));
                fireShapes.add(new Vector3().set(-WIDTH / 2f - verticalStep - 5, i * verticalStep - HEIGHT / 2f, verticalStep + 0.5f));
            }
        }
        if (enableStarGateEffect) {
            for (int i = 0; i < samplesSmoothed[pos] * 15; i++) {
                starGateTriangles.add(new Vector3().set(0, 0, 10));
                starGateTriangleSpeeds.add(new Vector3(getRandomInRange(-200, 200) / 50f * (samplesSmoothed[pos] + 1), getRandomInRange(-200, 200) / 50f * (samplesSmoothed[pos] + 1), getRandomInRange(-180, 180)));
                starGateTriangleColors.add(new Color().fromHsv(samplesRaw[pos] * 120 - 50 + colorOffset, 0.75f, 0.9f));
            }
        }
        if (enableTriangles) {
            for (int i = 0; i < triangleCount; i++) {
                triangles.add(new Vector3().set(
                        -MathUtils.sinDeg(offsetAngle * 7 + i * triangleStep) * (HEIGHT / 2f - 30 * (samplesSmoothed[pos] * orbitAmplitude + triangleSizeModifier)),
                        -MathUtils.cosDeg(offsetAngle * 7 + i * triangleStep) * (HEIGHT / 2f - 30 * (samplesSmoothed[pos] * orbitAmplitude + triangleSizeModifier)),
                        30));
            }
        }
        
        //shift to the right
        for (int i = samples.length + 3; i > 3; i--) {
            samplesToDisplay[i] = samplesToDisplay[i - 3];
        }
        for (int i = 0; i < 3; i++) {
            samplesToDisplay[i] = 5;
        }
        
        //mirror
        for (int i = 0; i < samplesToDisplay.length; i++) {
            if (i >= 525) {
                samplesToDisplay[i] = Math.abs(samplesToDisplay[1050 - i]);
            }
        }
        
        if (enableWallFire) {
            drawWallFire(delta);
        }
        
        if (enableStarGateEffect) {
            drawStarGateTriangles(delta, pos);
        }
        
        if (changeBranchLength) {
            radiuses[0] = defaultRadius * (1 + samplesSmoothed[pos] * 0.5f);
        }
        
        if (additionalRotation && music.isPlaying()) {
            offsetAngle += delta * 10 * (samplesSmoothed[pos] + 0.5f);
        }
        
        if (enableFlyingBalls) {
            drawFlyingBalls(newSamples, delta, pos);
        }
        if (enableTriangles || enableFlyingBallsTrails) {
            renderer.setColor(new Color().fromHsv(samplesSmoothed[pos] * 45 + colorOffset, 0.75f, 1));
            for (int i = 0; i < triangles.size; i++) {
                renderer.circle(triangles.get(i).x, triangles.get(i).y, triangles.get(i).z, 3);
                triangles.get(i).z -= delta * 100f;
                if (triangles.get(i).z <= 0) {
                    triangles.removeIndex(i);
                }
            }
        }
        
        renderer.end();
        utils.bloomRender();
        
        if (enableBottomFFT) {
            drawBottomFFT(samplesToDisplay, pos);
        }
        
        batch.begin();
        drawExitButton();
        batch.end();
        
    }
    
    private void drawBottomFFT(float[] samplesToDisplay, int pos) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        
        float ffStep = 360 / 1050f;
        for (int i = 0; i < 1050; i++) {
            
            float aAngle = 0;
            if (i > 525) {
                aAngle = 5 * (i - 525) / 300f;
            } else if (i < 525) {
                aAngle = 5 * (i - 525) / 300f;
            }
            
            float xStart = -MathUtils.cosDeg(i * ffStep + offsetAngle * 1.3f) * (HEIGHT / 2f - 30 * (samplesSmoothed[pos] * orbitAmplitude + fftSizeModifier));
            float yStart = -MathUtils.sinDeg(i * ffStep + offsetAngle * 1.3f) * (HEIGHT / 2f - 30 * (samplesSmoothed[pos] * orbitAmplitude + fftSizeModifier));
            
            float xEnd = -MathUtils.cosDeg(i * ffStep + offsetAngle * 1.3f + aAngle) * (HEIGHT / 2f - 30 * (samplesSmoothed[pos] * orbitAmplitude + fftSizeModifier) + displaySamples[i] / 10000f);
            float yEnd = -MathUtils.sinDeg(i * ffStep + offsetAngle * 1.3f + aAngle) * (HEIGHT / 2f - 30 * (samplesSmoothed[pos] * orbitAmplitude + fftSizeModifier) + displaySamples[i] / 10000f);
            
            displaySamples[i] += samplesToDisplay[i];
            Color color = new Color().fromHsv(MathUtils.clamp(displaySamples[i] / 10100 + colorOffset, 0, 190), 0.9f, 1);
            color.a = 0.3f;
            renderer.setColor(color);
            renderer.rectLine(xStart, yStart, xEnd, yEnd, 3);
            displaySamples[i] /= 1.3f;
        }
        
        renderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
    
    private void drawFlyingBalls(float[] newSamples, float delta, int pos) {
        float prevX = 0;
        float prevY = 0;
        float currentX = 0;
        float currentY = 0;
        
        for (int i = 0; i < radiuses.length; i++) {
            currentX = prevX - MathUtils.sinDeg(currentAngles[i] - offsetAngle) * radiuses[i];
            currentY = prevY - MathUtils.cosDeg(currentAngles[i] - offsetAngle) * radiuses[i];
            prevX = currentX;
            prevY = currentY;
            currentAngles[i] -= newSamples[i] * delta / 251f;
            if (i < radiuses.length - 1 && enableFlyingBallsTrails) {
                triangles.add(new Vector3().set(currentX, currentY, 10));
            }
        }
        
        points.add(new Vector2().set(currentX, currentY));
        colors.add(new Color().fromHsv(samplesSmoothed[pos] * 45 + colorOffset, 0.75f, 1));
        
        prevX = 0;
        prevY = 0;
        
        for (int i = 0; i < radiuses.length; i++) {
            currentX = prevX - MathUtils.sinDeg(currentAngles2[i] + offsetAngle) * radiuses[i];
            currentY = prevY - MathUtils.cosDeg(currentAngles2[i] + offsetAngle) * radiuses[i];
            prevX = currentX;
            prevY = currentY;
            currentAngles2[i] += newSamples[i] * delta / 251f;
            if (i < radiuses.length - 1 && enableFlyingBallsTrails) {
                triangles.add(new Vector3().set(currentX, currentY, 10));
            }
        }
        
        points2.add(new Vector2().set(currentX, currentY));
        colors2.add(new Color().fromHsv(-samplesSmoothed[pos] * 45 + colorOffset, 0.75f, 1));
        
        float fadeout = delta / 2f;
        
        for (int i = 0; i < points.size - 1; i++) {
            renderer.rectLine(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y, 5, colors.get(i), colors.get(i + 1));
            colors.get(i).r = MathUtils.clamp(colors.get(i).r - fadeout, 0, 1);
            colors.get(i).g = MathUtils.clamp(colors.get(i).g - fadeout, 0, 1);
            colors.get(i).b = MathUtils.clamp(colors.get(i).b - fadeout, 0, 1);
            if (colors.get(i).r + colors.get(i).g + colors.get(i).b == 0) {
                colors.removeIndex(i);
                points.removeIndex(i);
            }
        }
        
        renderer.setColor(colors.get(colors.size - 1));
        renderer.circle(points.get(points.size - 1).x, points.get(points.size - 1).y, 10);
        
        for (int i = 0; i < points2.size - 1; i++) {
            renderer.rectLine(points2.get(i).x, points2.get(i).y, points2.get(i + 1).x, points2.get(i + 1).y, 5, colors2.get(i), colors2.get(i + 1));
            colors2.get(i).r = MathUtils.clamp(colors2.get(i).r - fadeout, 0, 1);
            colors2.get(i).g = MathUtils.clamp(colors2.get(i).g - fadeout, 0, 1);
            colors2.get(i).b = MathUtils.clamp(colors2.get(i).b - fadeout, 0, 1);
            if (colors2.get(i).r + colors2.get(i).g + colors2.get(i).b == 0) {
                colors2.removeIndex(i);
                points2.removeIndex(i);
            }
        }
        
        renderer.setColor(colors2.get(colors2.size - 1));
        renderer.circle(points2.get(points2.size - 1).x, points2.get(points2.size - 1).y, 10);
        
    }
    
    private void drawStarGateTriangles(float delta, int pos) {
        for (int i = 0; i < starGateTriangles.size; i++) {
            float[] triangle = calculatePolygon(starGateTriangles.get(i).x, starGateTriangles.get(i).y, starGateTriangles.get(i).z, starGateTriangleSpeeds.get(i).z, 3, 0);
            
            renderer.setColor(starGateTriangleColors.get(i));
            renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
            
            starGateTriangles.set(i, starGateTriangles.get(i).add(starGateTriangleSpeeds.get(i).x * delta * 100, starGateTriangleSpeeds.get(i).y * delta * 100, -delta * 3));
            
            float radius = starGateTriangles.get(i).x * starGateTriangles.get(i).x + starGateTriangles.get(i).y * starGateTriangles.get(i).y;
            radius = (float) Math.sqrt(radius);
            
            if (radius > HEIGHT / 2f - 30 * (samplesSmoothed[pos] * orbitAmplitude + starGateSizeModifier) || starGateTriangles.get(i).z <= 0) {
                starGateTriangles.removeIndex(i);
                starGateTriangleSpeeds.removeIndex(i);
                starGateTriangleColors.removeIndex(i);
            }
        }
    }
    
    private void drawWallFire(float delta) {
        for (int n = 0; n < 2; n++) {
            for (int i = 0; i < fireShapes.size; i++) {
                renderer.setColor(fireColors.get(i));
                renderer.circle(
                        fireShapes.get(i).x, fireShapes.get(i).y,
                        fireShapes.get(i).z, 5);
                
                fireShapes.get(i).set(fireShapes.get(i).x + delta * 500, fireShapes.get(i).y, fireShapes.get(i).z - delta * 5);
                fireColors.get(i).r = MathUtils.clamp(fireColors.get(i).r - delta / 2f * 1.7f, 0, 1);
                fireColors.get(i).g = MathUtils.clamp(fireColors.get(i).g - delta / 2f * 2, 0, 1);
                fireColors.get(i).b = MathUtils.clamp(fireColors.get(i).b - delta / 2f * 2, 0, 1);
                if (fireColors.get(i).r + fireColors.get(i).g + fireColors.get(i).b <= 0) {
                    fireColors.removeIndex(i);
                    fireShapes.removeIndex(i);
                }
            }
            renderer.setTransformMatrix(renderer.getTransformMatrix().rotate(0, 0, 1, 180));
        }
    }
    
    public static void init() {
    
        settings = new Array<>();
        settings.add(new SettingsEntry("Number of branches", 1, 5, 2, Type.INT));
        settings.add(new SettingsEntry("Vary branch length", 0, 1, 1, Type.BOOLEAN));
        settings.add(new SettingsEntry("Additional rotation", 0, 1, 1, Type.BOOLEAN));
        settings.add(new SettingsEntry("Enable triangles", 0, 1, 1, Type.BOOLEAN));
        settings.add(new SettingsEntry("Orbit amplitude", -3, 11, 3, Type.FLOAT));
        settings.add(new SettingsEntry("Triangle count", 0, 72, 36, Type.INT));
        settings.add(new SettingsEntry("Enable wall fire", 0, 1, 1, Type.BOOLEAN));
        settings.add(new SettingsEntry("Enable stargate", 0, 1, 0, Type.BOOLEAN));
        settings.add(new SettingsEntry("Enable flying balls", 0, 1, 1, Type.BOOLEAN));
        settings.add(new SettingsEntry("Enable fft", 0, 1, 1, Type.BOOLEAN));
        settings.add(new SettingsEntry("Enable trail triangles", 0, 1, 1, Type.BOOLEAN));
        settings.add(new SettingsEntry("Color offset", 0, 180, 0, Type.FLOAT));
        settings.add(new SettingsEntry("Sync color to the beats", 0, 1, 0, Type.BOOLEAN));
        settings.add(new SettingsEntry("Synced color amplitude", 5, 180, 15, Type.FLOAT));
        settings.add(new SettingsEntry("Stargate\n orbit reduction", 1, 5, 1, Type.FLOAT));
        settings.add(new SettingsEntry("Fft\n orbit reduction", 1, 5, 1, Type.FLOAT));
        settings.add(new SettingsEntry("Triangle\n orbit reduction", 1, 5, 1, Type.FLOAT));
        settings.add(new SettingsEntry("Flying balls\n orbit reduction", -5, 11, 0, Type.FLOAT));
        
        paletteNames = new Array<>();
        for (int i = 0; i < FourierScreen.Palette.values().length; i++) {
            paletteNames.add(FourierScreen.Palette.values()[i].name().toLowerCase(Locale.ROOT).replace("_", " "));
        }
        
        typeNames = new Array<>();
        for (int i = 0; i < FourierScreen.Mode.values().length; i++) {
            typeNames.add(FourierScreen.Mode.values()[i].name().toLowerCase(Locale.ROOT).replace("_", " "));
        }
    }
    
    public static String getName() {
        return "Fourier visualiser";
    }
    
    public static void setSettings(int mode, int palette) {
        FourierScreen.mode = FourierScreen.Mode.values()[mode];
        FourierScreen.palette = FourierScreen.Palette.values()[palette];
        switch ((int) getSettingByName("Number of branches")) {
            case (1):
                numberOfLinks = 1;
                break;
            case (2):
                numberOfLinks = 3;
                break;
            case (3):
                numberOfLinks = 5;
                break;
            case (4):
                numberOfLinks = 15;
                break;
            case (5):
                numberOfLinks = 25;
                break;
        }
        changeBranchLength = getSettingByName("Vary branch length") > 0;
        additionalRotation = getSettingByName("Additional rotation") > 0;
        enableTriangles = getSettingByName("Enable triangles") > 0;
        orbitAmplitude = getSettingByName("Orbit amplitude");
        triangleCount = (int) getSettingByName("Triangle count");
        triangleStep = 360 / (float)triangleCount;
        enableWallFire = getSettingByName("Enable wall fire") > 0;
        enableStarGateEffect = getSettingByName("Enable stargate") > 0;
        enableFlyingBalls = getSettingByName("Enable flying balls") > 0;
        enableBottomFFT = getSettingByName("Enable fft") > 0;
        enableFlyingBallsTrails = getSettingByName("Enable trail triangles") > 0;
        colorOffset = getSettingByName("Color offset");
        syncColorOffsetToTheMusic = getSettingByName("Sync color to the beats") > 0;
        syncedColorAmplitude = getSettingByName("Synced color amplitude");
        starGateSizeModifier = getSettingByName("Stargate\n orbit reduction");
        fftSizeModifier = getSettingByName("Fft\n orbit reduction");
        triangleSizeModifier = getSettingByName("Triangle\n orbit reduction");
        flyingBallsSizeModifier = getSettingByName("Flying balls\n orbit reduction");
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height, 0, true);
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
