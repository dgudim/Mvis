package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

public class OsciloscopeScreen extends BaseVisualiser implements Screen {

    private Array<Vector3> dots;
    private Array<Vector3> colors;

    //settings
    private static int freqDisplaySamples = 512;
    private static float fadeout = 0.005f;
    private static int radialAmplitude = 180;
    private float freqDisplayRenderAngle;
    private float angleStep = 360 / (float) freqDisplaySamples;
    private int skipOver;

    private int prevPosition = 0;

    public final int STANDARD = 0;
    public final int RADIAL = 1;
    public final int BUBBLE = 2;
    public final int RADIAL_BUBBLE = 3;
    public final int SHAPES = 4;
    public final int SINUS = 5;
    public static final int FREQUENCY = 6;

    public final int LIME = 100;
    public final int FIRE = 101;
    public final int CYAN = 102;

    private Color palletColor;
    private Vector3 palletFadeoutPattern;

    private static float maxSaturation = 4;

    private static int type = 0;
    private static int palette = 100;

    public OsciloscopeScreen(Game game) {
        super(game, new boolean[]{type == FREQUENCY, !(type == FREQUENCY), false});

        dots = new Array<>();
        colors = new Array<>();

        freqDisplayRenderAngle = 0;

        if (type == BUBBLE || type == SHAPES || type == RADIAL_BUBBLE || type == SINUS) {
            skipOver = step;
            maxSaturation = 1;
        } else {
            skipOver = 1;
        }

        if (type == SINUS) {
            fadeout *= 30;
            maxSaturation = 3;
            utils.setBloomIntensity(2f);
        }

        if (type == FREQUENCY) {
            if (!render) {
                fadeout *= 5;
            } else {
                fadeout /= step / (float) freqDisplaySamples * 16;
            }
            utils.setBloomIntensity(1.3f);
        }

        if (type == SHAPES) {
            fadeout *= 2;
        }

        utils.maxSaturation = maxSaturation;

        palletColor = new Color();
        palletFadeoutPattern = new Vector3();

        switch (palette) {
            case (FIRE):
                palletColor.r = 1;
                palletColor.g = 1;
                palletFadeoutPattern.set(fadeout / 1.5f, fadeout * 1.5f, fadeout);
                break;
            case (CYAN):
                palletColor.r = 0.5f;
                palletColor.g = 1;
                palletColor.b = 1;
                palletFadeoutPattern.set(fadeout / 3f, fadeout * 6f, fadeout / 0.75f);
                break;
            case (LIME):
                palletColor.r = 0.5f;
                palletColor.g = 1;
                palletColor.b = 0.3f;
                palletFadeoutPattern.set(fadeout, fadeout, fadeout / 8f);
                break;
        }

    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void render(float delta) {

        int pos;
        if (!render) {
            pos = (int) (music.getPosition() * sampleRate);
        } else {
            pos = frame;
        }

        renderer.setProjectionMatrix(camera.combined);

        utils.bloomBegin(true, pos);

        renderer.begin();

        if (!render) {

            addCoords((int) (music.getPosition() * sampleRate));
            render();

            if (!(type == STANDARD || type == RADIAL)) {
                fadeOut();
            }

        } else {

            for (int i = 0; i < step; i += skipOver) {
                addCoords(frame);
                frame += skipOver;
                fadeOut();
            }

            recorderFrame++;

            render();

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

    private void addCoords(int pos) {

        float x, y, angle, rad;

        int repeat = 1; //triple circle effect happens automatically because music.getPosition returns in milliseconds
        if (render) {
            repeat = 3;  //make triple circle effect
        }

        switch (type) {
            case (STANDARD):
                if (!render) {
                    if (pos - prevPosition > 1500) {
                        prevPosition = pos - 1500;
                    }
                    for (int i = prevPosition; i < pos; i++) {
                        colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                        dots.add(new Vector3().set(lSamplesNormalised[i] * (HEIGHT / 2f - 100), rSamplesNormalised[i] * (HEIGHT / 2f - 100), 0));
                        fadeOut();
                    }
                    prevPosition = pos;
                } else {
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(lSamplesNormalised[pos] * (HEIGHT / 2f - 100), rSamplesNormalised[pos] * (HEIGHT / 2f - 100), 0));
                    fadeOut();
                }
                break;
            case (RADIAL):
                if (!render) {
                    if (pos - prevPosition > 1500) {
                        prevPosition = pos - 1500;
                    }
                    for (int i = prevPosition; i < pos; i++) {
                        angle = -lSamplesNormalised[i] * radialAmplitude;
                        x = -MathUtils.cosDeg(angle) * rSamplesNormalised[i] * (HEIGHT / 2f - 100);
                        y = -MathUtils.sinDeg(angle) * rSamplesNormalised[i] * (HEIGHT / 2f - 100);
                        colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                        dots.add(new Vector3().set(x, y, 0));
                        fadeOut();
                    }
                    prevPosition = pos;
                } else {
                    angle = -lSamplesNormalised[pos] * radialAmplitude;
                    x = -MathUtils.cosDeg(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    y = -MathUtils.sinDeg(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(x, y, 0));
                    fadeOut();
                }
                break;
            case (BUBBLE):
                for (int i = 0; i < repeat; i++) {
                    rad = Math.abs(Math.max(lSamplesNormalised[pos] * 300, rSamplesNormalised[pos] * 300));
                    rad = (float) (Math.random() * rad);
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(lSamplesNormalised[pos] * (HEIGHT / 2f - 100), rSamplesNormalised[pos] * (HEIGHT / 2f - 100), rad));
                }
                break;
            case (RADIAL_BUBBLE):
                for (int i = 0; i < repeat; i++) {
                    rad = Math.abs(Math.max(lSamplesNormalised[pos] * 300, rSamplesNormalised[pos] * 300));
                    rad = (float) (Math.random() * rad);
                    angle = -lSamplesNormalised[pos] * radialAmplitude;
                    x = -MathUtils.cosDeg(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    y = -MathUtils.sinDeg(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(x, y, rad));
                }
                break;
            case (SHAPES):
                x = 0;
                y = 0;
                dots.add(new Vector3().set(x, y, 0));
                colors.add(new Vector3(0, 0, 0));
                angle = lSamplesNormalised[pos] * 90 / 450;
                for (int i = 0; i < 50; i++) {
                    x = x - (float) Math.cos(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    y = y - (float) Math.sin(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    angle = angle + Math.abs(lSamplesNormalised[pos]) + 1;
                    dots.add(new Vector3().set(x, y, 0));
                    colors.add(new Vector3(palletColor.r, i / 50f * palletColor.g, palletColor.b));
                }
                break;
            case (SINUS):
                for (float i = 0; i < 1600; i += 0.5f) {
                    if (i % 7 == 0) {
                        y = (float) (Math.sin(i / 32) * lSamplesNormalised[pos] * 150);
                    } else {
                        y = (float) (Math.cos(i / 32) * rSamplesNormalised[pos] * 150);
                    }
                    dots.add(new Vector3().set(i - 800, y, 0));
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                }
                dots.add(new Vector3().set(WIDTH / 2f, 0, 0));
                colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                dots.add(new Vector3().set(-WIDTH / 2f, 0, 0));
                colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                break;
            case (FREQUENCY):
                if (!render) {
                    if (pos >= freqDisplaySamples / 2) {
                        for (int i = 0; i < freqDisplaySamples; i++) {
                            freqDisplayRenderAngle += angleStep;
                            x = -MathUtils.cosDeg(freqDisplayRenderAngle) * samplesRaw[pos - freqDisplaySamples / 2 + i] * (HEIGHT / 2f - 10);
                            y = -MathUtils.sinDeg(freqDisplayRenderAngle) * samplesRaw[pos - freqDisplaySamples / 2 + i] * (HEIGHT / 2f - 10);
                            dots.add(new Vector3().set(x, y, 0));
                            colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                        }
                    }
                } else {
                    freqDisplayRenderAngle += angleStep;
                    x = -MathUtils.cosDeg(freqDisplayRenderAngle) * samplesRaw[pos] * (HEIGHT / 2f - 10);
                    y = -MathUtils.sinDeg(freqDisplayRenderAngle) * samplesRaw[pos] * (HEIGHT / 2f - 10);
                    dots.add(new Vector3().set(x, y, 0));
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                }
        }
    }

    private void render() {
        // bubble renderer
        if (type == BUBBLE || type == RADIAL_BUBBLE) {
            for (int i = 0; i < dots.size; i++) {
                Vector3 colorV = colors.get(i);
                renderer.setColor(colorV.x, colorV.y, colorV.z, 1);
                float x = dots.get(i).x;
                float y = dots.get(i).y;
                float radius = dots.get(i).z;
                renderer.circle(x, y, radius);
            }
        } else {
            // normal renderer
            for (int i = 1; i < dots.size; i++) {
                Vector3 colorV = colors.get(i);
                renderer.setColor(colorV.x, colorV.y, colorV.z, 1);
                float x = dots.get(i - 1).x;
                float y = dots.get(i - 1).y;
                float x2 = dots.get(i).x;
                float y2 = dots.get(i).y;
                renderer.line(x, y, x2, y2);
            }
        }
    }

    private void fadeOut() {
        // fade the colors
        for (int i = 0; i < dots.size; i++) {

            Vector3 colorV = colors.get(i);

            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - palletFadeoutPattern.x, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - palletFadeoutPattern.y, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - palletFadeoutPattern.z, 0, 1);
                if (type == BUBBLE || type == RADIAL_BUBBLE) {
                    // size fadeout
                    dots.get(i).z = MathUtils.clamp(dots.get(i).z - fadeout * 4, 0, 450);
                }
                colors.set(i, colorV);
            } else {
                dots.removeIndex(i);
                colors.removeIndex(i);
            }
        }
    }

    public static void init() {
        paletteNames = new String[]{"Lime", "Fire", "Water"};
        typeNames = new String[]{"Oscilloscope", "Radial", "Bubble", "Bubble2", "Shapes", "Sinus", "Frequency in circle"};

        settings = new String[]{"Type", "Pallet", "Fadeout", "Frequency display samples", "Radial visualiser amplitude", "Max bloom saturation", "Render"};
        settingTypes = new String[]{"int", "int", "float", "int", "float", "float", "boolean"};

        settingMaxValues = new float[]{typeNames.length - 1, paletteNames.length - 1, 0.05f, 1024, 450, 4, 1};
        settingMinValues = new float[]{0, 0, 0.0005f, 256, 15, 0, 0};

        defaultSettings = new float[]{0, 0, fadeout, freqDisplaySamples, radialAmplitude, 1, 0};
    }

    public static String getName() {
        return "Oscilloscope";
    }

    public static void setSettings(float[] newSettings) {
        type = (int) newSettings[0];
        palette = (int) (newSettings[1] + 100);
        fadeout = newSettings[2];
        freqDisplaySamples = (int) newSettings[3];
        radialAmplitude = (int) newSettings[4];
        maxSaturation = newSettings[5];
        render = newSettings[6] > 0;
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
