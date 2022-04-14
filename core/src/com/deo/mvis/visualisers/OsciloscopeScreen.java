package com.deo.mvis.visualisers;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.mvis.utils.SettingsEntry;
import com.deo.mvis.utils.Type;

import java.util.Locale;

public class OsciloscopeScreen extends BaseVisualiser implements Screen {
    
    private final Array<Vector3> dots;
    private final Array<Vector3> colors;
    
    //settings
    private static int freqDisplaySampleMultiplier = 2;
    private static int freqDisplaySamples = 512;
    private static float fadeout = 0.005f;
    private static int polarAmplitude = 180;
    private float freqDisplayRenderAngle = 0;
    private final float angleStep = 360 / (float) freqDisplaySamples;
    private final int skipOver;
    
    private int prevPosition = 0;
    
    private final Color palletColor;
    private final Vector3 palletFadeoutPattern;
    
    private static float maxSaturation = 4;
    
    private static OsciloscopeScreen.Mode mode;
    private static OsciloscopeScreen.Palette palette;
    
    private enum Palette {
        LIME, FIRE, WATER
    }
    
    private enum Mode {
        OSCILLOSCOPE, POLAR, BUBBLE, POLAR_BUBBLE, SHAPES, SINUS, FREQUENCY_IN_CIRCLE
    }
    
    public OsciloscopeScreen(Game game) {
        super(game, ALL_SAMPLES_RAW);
        
        dots = new Array<>();
        colors = new Array<>();
        
        if (mode == Mode.BUBBLE || mode == Mode.SHAPES || mode == Mode.POLAR_BUBBLE || mode == Mode.SINUS) {
            skipOver = step;
            maxSaturation = 1;
        } else {
            skipOver = 1;
        }
        
        if (mode == Mode.SINUS) {
            fadeout *= 30;
            maxSaturation = 3;
            utils.setBloomIntensity(2f);
        }
        
        if (mode == Mode.FREQUENCY_IN_CIRCLE) {
            if (!render) {
                fadeout *= 5;
            } else {
                fadeout /= step / (float) freqDisplaySamples * 16;
            }
            utils.setBloomIntensity(1.3f);
        }
        
        if (mode == Mode.SHAPES) {
            fadeout *= 2;
        }
        
        utils.maxSaturation = maxSaturation;
        
        palletColor = new Color();
        palletFadeoutPattern = new Vector3();
        
        switch (palette) {
            case FIRE:
                palletColor.r = 1;
                palletColor.g = 1;
                palletFadeoutPattern.set(fadeout / 1.5f, fadeout * 1.5f, fadeout);
                break;
            case WATER:
                palletColor.r = 0.5f;
                palletColor.g = 1;
                palletColor.b = 1;
                palletFadeoutPattern.set(fadeout / 3f, fadeout * 6f, fadeout / 0.75f);
                break;
            case LIME:
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
            
            if (!(mode == Mode.OSCILLOSCOPE || mode == Mode.POLAR)) {
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
        
        switch (mode) {
            case OSCILLOSCOPE:
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
            case POLAR:
                if (!render) {
                    if (pos - prevPosition > 1500) {
                        prevPosition = pos - 1500;
                    }
                    for (int i = prevPosition; i < pos; i++) {
                        angle = -lSamplesNormalised[i] * polarAmplitude;
                        x = -MathUtils.cosDeg(angle) * rSamplesNormalised[i] * (HEIGHT / 2f - 100);
                        y = -MathUtils.sinDeg(angle) * rSamplesNormalised[i] * (HEIGHT / 2f - 100);
                        colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                        dots.add(new Vector3().set(x, y, 0));
                        fadeOut();
                    }
                    prevPosition = pos;
                } else {
                    angle = -lSamplesNormalised[pos] * polarAmplitude;
                    x = -MathUtils.cosDeg(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    y = -MathUtils.sinDeg(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(x, y, 0));
                    fadeOut();
                }
                break;
            case BUBBLE:
                for (int i = 0; i < repeat; i++) {
                    rad = Math.abs(Math.max(lSamplesNormalised[pos] * 300, rSamplesNormalised[pos] * 300));
                    rad = (float) (Math.random() * rad);
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(lSamplesNormalised[pos] * (HEIGHT / 2f - 100), rSamplesNormalised[pos] * (HEIGHT / 2f - 100), rad));
                }
                break;
            case POLAR_BUBBLE:
                for (int i = 0; i < repeat; i++) {
                    rad = Math.abs(Math.max(lSamplesNormalised[pos] * 300, rSamplesNormalised[pos] * 300));
                    rad = (float) (Math.random() * rad);
                    angle = -lSamplesNormalised[pos] * polarAmplitude;
                    x = -MathUtils.cosDeg(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    y = -MathUtils.sinDeg(angle) * rSamplesNormalised[pos] * (HEIGHT / 2f - 100);
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                    dots.add(new Vector3().set(x, y, rad));
                }
                break;
            case SHAPES:
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
            case SINUS:
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
            case FREQUENCY_IN_CIRCLE:
                if (!render) {
                    if (pos >= freqDisplaySamples) {
                        for (int i = 0; i < freqDisplaySamples; i += freqDisplaySampleMultiplier) {
                            freqDisplayRenderAngle += angleStep * freqDisplaySampleMultiplier;
                            x = -MathUtils.cosDeg(freqDisplayRenderAngle) * samplesSmoothed[pos - freqDisplaySamples / 2 + i] * (HEIGHT / 2f - 10);
                            y = -MathUtils.sinDeg(freqDisplayRenderAngle) * samplesSmoothed[pos - freqDisplaySamples / 2 + i] * (HEIGHT / 2f - 10);
                            dots.add(new Vector3().set(x, y, 0));
                            colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                        }
                    }
                    while (freqDisplayRenderAngle > 360) {
                        freqDisplayRenderAngle -= 360;
                    }
                } else {
                    freqDisplayRenderAngle += angleStep;
                    x = -MathUtils.cosDeg(freqDisplayRenderAngle) * samplesSmoothed[pos] * (HEIGHT / 2f - 10);
                    y = -MathUtils.sinDeg(freqDisplayRenderAngle) * samplesSmoothed[pos] * (HEIGHT / 2f - 10);
                    dots.add(new Vector3().set(x, y, 0));
                    colors.add(new Vector3(palletColor.r, palletColor.g, palletColor.b));
                }
        }
    }
    
    private void render() {
        // bubble renderer
        if (mode == Mode.POLAR || mode == Mode.POLAR_BUBBLE) {
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
                if (mode == Mode.BUBBLE || mode == Mode.POLAR_BUBBLE) {
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
    
        settings = new Array<>();
        settings.add(new SettingsEntry("Fadeout",0.0005f,0.05f,fadeout,Type.FLOAT));
        settings.add(new SettingsEntry("Frequency display samples",2048,16192,freqDisplaySamples,Type.INT));
        settings.add(new SettingsEntry("Frequency display sample multiplier",1,8,2,Type.INT));
        settings.add(new SettingsEntry("Polar visualiser amplitude",15,450, polarAmplitude,Type.FLOAT));
        settings.add(new SettingsEntry("Max bloom saturation",0,4,1,Type.FLOAT));
        settings.add(new SettingsEntry("Render",0,1,0, Type.BOOLEAN));
    
        paletteNames = new Array<>();
        for (int i = 0; i < OsciloscopeScreen.Palette.values().length; i++) {
            paletteNames.add(OsciloscopeScreen.Palette.values()[i].name().toLowerCase(Locale.ROOT).replace("_", ""));
        }
    
        typeNames = new Array<>();
        for (int i = 0; i < OsciloscopeScreen.Mode.values().length; i++) {
            typeNames.add(OsciloscopeScreen.Mode.values()[i].name().toLowerCase(Locale.ROOT).replace("_", ""));
        }
    }
    
    public static String getName() {
        return "Oscilloscope";
    }
    
    public static void setSettings(int mode, int palette) {
        OsciloscopeScreen.mode = OsciloscopeScreen.Mode.values()[mode];
        OsciloscopeScreen.palette = OsciloscopeScreen.Palette.values()[palette];
        
        fadeout = getSettingByName("Fadeout");
        freqDisplaySamples = (int) getSettingByName("Frequency display samples");
        freqDisplaySampleMultiplier = (int) getSettingByName("Frequency display sample multiplier") + 4;
        polarAmplitude = (int) getSettingByName("Polar visualiser amplitude");
        maxSaturation = getSettingByName("Max bloom saturation");
        render = getSettingByName("Render") > 0;
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
