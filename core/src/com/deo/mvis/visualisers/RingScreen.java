package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.deo.mvis.Launcher.HEIGHT;

public class RingScreen extends BaseVisualiser implements Screen {

    private Array<Float> radiuses, radiuses2;
    private Array<Vector2> positions;
    private Array<Vector3> colors;
    private static float fadeout = 0.006f;
    private static float ringGrowSpeed = 12f;

    private static int palette, type;

    public RingScreen(Game game) {
        super(game, new boolean[]{false, true, false});

        radiuses = new Array<>();
        radiuses2 = new Array<>();
        positions = new Array<>();
        colors = new Array<>();

        utils.maxSaturation = 2;

    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void render(float delta) {

        fadeout();
        float radiusx, radiusy;
        int pos;
        if (!render) {
            radiusx = lSamplesNormalised[(int) (music.getPosition() * sampleRate)] * 720;
            radiusy = rSamplesNormalised[(int) (music.getPosition() * sampleRate)] * 720;
            pos = (int) (music.getPosition() * sampleRate);
        } else {
            radiusx = lSamplesNormalised[frame] * 720;
            radiusy = rSamplesNormalised[frame] * 720;
            pos = frame;
            frame += step;
            recorderFrame++;
        }

        positions.add(new Vector2(0, 0));
        colors.add(new Vector3(1, 1, 0));

        radiuses.add(radiusx);
        radiuses2.add(radiusy);

        renderer.setProjectionMatrix(camera.combined);

        utils.bloomBegin(true, pos);
        renderer.begin();
        for (int i = 0; i < positions.size; i++) {
            renderer.setColor(colors.get(i).x, colors.get(i).y, colors.get(i).z, 1);
            renderer.ellipse(positions.get(i).x - radiuses.get(i) / 2f, positions.get(i).y - radiuses2.get(i) / 2f, radiuses.get(i), radiuses2.get(i));
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

    void fadeout() {
        for (int i = 0; i < positions.size; i++) {
            radiuses.set(i, radiuses.get(i) + ringGrowSpeed);
            radiuses2.set(i, radiuses2.get(i) + ringGrowSpeed);
            Vector3 colorV = colors.get(i);
            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - fadeout / 1.5f, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - fadeout * 1.5f, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - fadeout, 0, 1);
                colors.set(i, colorV);
            } else {
                positions.removeIndex(i);
                radiuses.removeIndex(i);
                radiuses2.removeIndex(i);
                colors.removeIndex(i);
            }
        }
    }

    public static void init() {
        paletteNames = new String[]{"Default"};
        typeNames = new String[]{"Default"};

        settings = new String[]{"Type", "Pallet", "Ring grow speed", "Fadeout", "Render"};
        settingTypes = new String[]{"int", "int", "float", "float", "boolean"};

        settingMaxValues = new float[]{typeNames.length-1, paletteNames.length-1, 25, 0.05f, 1};
        settingMinValues = new float[]{0, 0, 5, 0.0005f, 0};

        defaultSettings = new float[]{0, 0, ringGrowSpeed, fadeout, 0};
    }

    public static String getName(){
        return "Ring";
    }

    public static void setSettings(float[] newSettings) {
        type = (int) newSettings[0];
        palette = (int) newSettings[1] + 100;
        ringGrowSpeed = newSettings[2];
        fadeout = newSettings[3];
        render = newSettings[4] > 0;
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
