package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.utils.SettingsArray;

import static com.deo.mvis.Launcher.HEIGHT;

public class MushroomScreen extends BaseVisualiser implements Screen {

    private Array<Array<Vector2>> branches;
    private Array<Vector3> colors;
    private static float fadeout = 0.08f;

    private final int SINGLE = 0;
    private final int DOUBLE = 1;
    private final int TRIPLE = 2;
    private final int DOUBLE_CHANNEL = 3;

    private static boolean exponential = false;
    private static float branchLength = 45;
    private static float maxAngle = 45;
    private static int maxIterations = 7;
    private static int baseIterations = 5;
    private static float baseAngle = 5;
    private static float maxSaturation = 3;

    private static int type;
    public static int palette;

    public MushroomScreen(Game game) {
        super(game, new boolean[]{false, false, true});

        branches = new Array<>();
        colors = new Array<>();

        utils.maxSaturation = maxSaturation;
        utils.setBloomIntensity(3);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fadeOut();
        float angle;
        int iterations;
        int pos;
        if (!render) {
            angle = samplesSmoothed[(int) (music.getPosition() * sampleRate)] * maxAngle + baseAngle;
            iterations = (int) (lSamplesNormalisedSmoothed[(int) (music.getPosition() * sampleRate)] * maxIterations) + baseIterations;
            pos = (int) (music.getPosition() * sampleRate);
        } else {
            angle = samplesSmoothed[frame] * maxAngle + baseAngle;
            iterations = (int) (lSamplesNormalisedSmoothed[frame] * maxIterations) + baseIterations;
            pos = frame;
            frame += step;
            recorderFrame++;
        }

        switch (type) {
            case (SINGLE):
                buildMushroom(-90, angle, branchLength, iterations, 0, -100);
                break;
            case (DOUBLE):
                buildMushroom(-90, angle, branchLength, iterations, 0, 0);
                buildMushroom(90, angle, branchLength, iterations, 0, 0);
                break;
            case (DOUBLE_CHANNEL):
                angle = rSamplesNormalisedSmoothed[(int) (music.getPosition() * sampleRate)] * maxAngle + baseAngle;
                iterations = (int) (rSamplesNormalisedSmoothed[(int) (music.getPosition() * sampleRate)] * maxIterations) + baseIterations;
                buildMushroom(-90, angle, branchLength, iterations, 0, 0);
                angle = lSamplesNormalisedSmoothed[(int) (music.getPosition() * sampleRate)] * maxAngle + baseAngle;
                iterations = (int) (lSamplesNormalisedSmoothed[(int) (music.getPosition() * sampleRate)] * maxIterations) + baseIterations;
                buildMushroom(90, angle, branchLength, iterations, 0, 0);
                break;
            case (TRIPLE):
                buildMushroom(150, angle, branchLength, iterations, 0, 0);
                buildMushroom(270, angle, branchLength, iterations, 0, 0);
                buildMushroom(390, angle, branchLength, iterations, 0, 0);
                break;
        }

        renderer.setProjectionMatrix(camera.combined);

        utils.bloomBegin(true, pos);

        renderer.begin();
        for (int i = 0; i < branches.size; i++) {
            renderer.setColor(colors.get(i).x, colors.get(i).y, MathUtils.clamp((branches.get(i).get(0).y + HEIGHT / 2f) / HEIGHT - 0.3f, 0, 1), 1);
            renderer.line(branches.get(i).get(0), branches.get(i).get(1));
        }
        renderer.end();

        utils.bloomRender();

        if (render) {
            utils.makeAScreenShot(recorderFrame);
            utils.displayData(recorderFrame, frame, camera.combined);
        }

        batch.begin();
        drawExitButton();
        batch.end();

    }

    private void fadeOut() {
        for (int i = 0; i < colors.size; i++) {
            Vector3 colorV = colors.get(i);
            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - fadeout / 1.5f, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - fadeout * 1.5f, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - fadeout, 0, 1);
                colors.set(i, colorV);
            } else {
                branches.removeIndex(i);
                colors.removeIndex(i);
            }
        }
    }

    void buildMushroom(float initialAngle, float offsetAngle, float branchLength, int iterations, float x, float y) {

        float angleRight = initialAngle + offsetAngle;
        float angleLeft = initialAngle - offsetAngle;

        float sinRight = MathUtils.sinDeg(angleRight) * branchLength;
        float cosRight = MathUtils.cosDeg(angleRight) * branchLength;

        float sinLeft = MathUtils.sinDeg(angleLeft) * branchLength;
        float cosLeft = MathUtils.cosDeg(angleLeft) * branchLength;

        float xRight = x - cosRight;
        float yRight = y - sinRight;

        float xLeft = x - cosLeft;
        float yLeft = y - sinLeft;

        Array<Vector2> branchRight = new Array<>();

        branchRight.add(new Vector2(x, y));
        branchRight.add(new Vector2(xRight, yRight));

        Array<Vector2> branchLeft = new Array<>();

        branchLeft.add(new Vector2(x, y));
        branchLeft.add(new Vector2(xLeft, yLeft));

        branches.add(branchRight, branchLeft);
        colors.add(new Vector3(1, 1, 0), new Vector3(1, 1, 0));

        if (iterations > 0 && branchLength > 0) {
            float offset = -0.9f;
            float bOffset = 0;
            if (exponential) {
                offset = 1;
                bOffset = 1.7f;
            }
            buildMushroom(angleRight, offsetAngle + offset, branchLength + bOffset, iterations - 1, xRight, yRight);
            buildMushroom(angleLeft, offsetAngle + offset, branchLength + bOffset, iterations - 1, xLeft, yLeft);
        }

    }

    public static void init() {
        paletteNames = new String[]{"Default"};
        typeNames = new String[]{"Single", "Double", "Triple", "Double channel"};

        addSetting("Type","int",0.0f,3.0f,0.0f);
        addSetting("Palette","int",0.0f,0.0f,0.0f);
        addSetting("Exponential","boolean",0.0f,1.0f,0.0f);
        addSetting("Branch length","float",1.0f,80.0f,45.0f);
        addSetting("Max iterations (+5)","int",1.0f,10.0f,7.0f);
        addSetting("Max angle","float",5.0f,90.0f,45.0f);
        addSetting("Base iterations","int",1.0f,7.0f,5.0f);
        addSetting("Base angle","float",0.0f,45.0f,5.0f);
        addSetting("Fadeout","float",5.0E-4f,0.1f,0.08f);
        addSetting("Max bloom saturation","float",0.0f,5.0f,3.0f);
        addSetting("Render","boolean",0.0f,1.0f,0.0f);
    }

    public static String getName() {
        return "Fractal tree";
    }

    public static void setSettings(SettingsArray newSettings){

        type = (int) newSettings.getSettingByName("Type");
        palette = (int) newSettings.getSettingByName("Palette");
        exponential = newSettings.getSettingByName("Exponential") > 0;

        branchLength = newSettings.getSettingByName("Branch length");
        maxIterations = (int) newSettings.getSettingByName("Max iterations (+5)");
        maxAngle = newSettings.getSettingByName("Max angle");

        baseIterations = (int) newSettings.getSettingByName("Base iterations");
        baseAngle = newSettings.getSettingByName("Base angle");

        fadeout = newSettings.getSettingByName("Fadeout");
        maxSaturation = newSettings.getSettingByName("Max bloom saturation");
        render = newSettings.getSettingByName("Render") > 0;
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
