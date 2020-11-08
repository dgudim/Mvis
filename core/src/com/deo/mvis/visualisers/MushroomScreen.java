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

import static com.deo.mvis.Launcher.HEIGHT;

public class MushroomScreen extends BaseVisualiser implements Screen {

    private Array<Array<Vector2>> branches;
    private Array<Vector3> colors;
    private static float fadeout;

    private final int SINGLE = 0;
    private final int DOUBLE = 1;
    private final int TRIPLE = 2;
    private final int DOUBLE_CHANNEL = 3;

    private static boolean exponential = false;
    private static float branchLength = 40;
    private static float maxAngle = 45;
    private static int maxIterations = 10;

    private static int type;
    public static int palette;

    public MushroomScreen(Game game) {
        super(game);

        branches = new Array<>();
        colors = new Array<>();
        fadeout = 0.05f;

        utils.maxSaturation = 3;
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
            angle = rSamplesNormalised[(int) (music.getPosition() * 44100)] * maxAngle;
            iterations = (int) (lSamplesNormalised[(int) (music.getPosition() * 44100)] * maxIterations) + 5;
            pos = (int) (music.getPosition() * 44100);
        } else {
            angle = rSamplesNormalised[frame] * maxAngle;
            iterations = (int) (lSamplesNormalised[frame] * maxIterations) + 5;
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
                angle = rSamplesNormalised[(int) (music.getPosition() * 44100)] * 45;
                iterations = (int) (rSamplesNormalised[(int) (music.getPosition() * 44100)] * maxIterations) + 5;
                buildMushroom(-90, angle, branchLength, iterations, 0, 0);
                angle = lSamplesNormalised[(int) (music.getPosition() * 44100)] * 45;
                iterations = (int) (lSamplesNormalised[(int) (music.getPosition() * 44100)] * maxIterations) + 5;
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
            renderer.setColor(colors.get(i).x, colors.get(i).y, (branches.get(i).get(0).y - 100) / 900f, 1);
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

        settings = new String[]{"Type", "Pallet", "Exponential", "Branch length", "Max iterations (+5)", "Max angle"};
        settingTypes = new String[]{"int", "int", "boolean", "float", "int", "float"};

        settingMaxValues = new float[]{typeNames.length - 1, paletteNames.length - 1, 1, 80, 25, 90};
        settingMinValues = new float[]{0, 0, 0, 1, 1, 5};

        defaultSettings = new float[]{0, 0, 0, 40, 10, 45};
    }

    public static String getName() {
        return "Fractal tree";
    }

    public static void setSettings(float[] newSettings) {
        type = (int) newSettings[0];
        palette = (int) newSettings[1];
        exponential = newSettings[2] > 0;
        branchLength = newSettings[3];
        maxIterations = (int) newSettings[4];
        maxAngle = newSettings[5];
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
