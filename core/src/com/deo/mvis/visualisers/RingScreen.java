package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.mvis.utils.CompositeSettings;
import com.deo.mvis.utils.SettingsEntry;
import com.deo.mvis.utils.Type;

public class RingScreen extends BaseVisualiser implements Screen {
    
    private final Array<Float> radiuses;
    private final Array<Float> radiuses2;
    private final Array<Vector2> positions;
    private final Array<Vector3> colors;
    private static float fadeout = 0.006f;
    private static float ringGrowSpeed = 12f;
    
    private static RingScreen.Mode mode;
    private static RingScreen.Palette palette;
    
    private enum Palette {
        DEFAULT
    }
    
    private enum Mode {
        DEFAULT
    }
    
    public RingScreen(Game game) {
        super(game, LEFT_AND_RIGHT_RAW);
        
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
            pos = (int) (music.getPosition() * sampleRate);
        } else {
            pos = frame;
            frame += sampleStep;
            recorderFrame++;
        }
        
        radiusx = lSamplesNormalised[pos] * 720;
        radiusy = rSamplesNormalised[pos] * 720;
        
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
    
    public static CompositeSettings init() {
        
        CompositeSettings compositeSettings = new CompositeSettings(enumToArray(Palette.class), enumToArray(Mode.class));
        
        compositeSettings.addSetting("Ring grow speed", 5, 25, ringGrowSpeed, Type.FLOAT);
        compositeSettings.addSetting("Fadeout", 0.0005f, 0.05f, fadeout, Type.FLOAT);
        compositeSettings.addSetting("Render", 0, 1, 0, Type.BOOLEAN);
        
        return compositeSettings;
    }
    
    public static String getName() {
        return "Ring";
    }
    
    public static void setSettings(Array<SettingsEntry> settings, int mode, int palette) {
        RingScreen.mode = RingScreen.Mode.values()[mode];
        RingScreen.palette = RingScreen.Palette.values()[palette];
        
        ringGrowSpeed = getSettingByName(settings, "Ring grow speed");
        fadeout = getSettingByName(settings, "Fadeout");
        render = getSettingByName(settings, "Render") > 0;
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
