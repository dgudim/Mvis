package com.deo.mvis.visualisers;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.utils.SettingsEntry;
import com.deo.mvis.utils.Type;

import java.util.Locale;

public class MuffinScreen extends BaseVisualiser implements Screen {
    
    private final PerspectiveCamera cam;
    private final ModelBatch modelBatch;
    private final ModelBuilder modelBuilder;
    private final Array<Model> models;
    private final Array<Float> modelYPoses;
    private final Array<ModelInstance> instances;
    private final Array<Array<Integer>> cachedPositions;
    private final Environment environment;
    boolean preloaded;
    
    private float degrees;
    
    private Color fadeColor;
    private DirectionalLight prevLight;
    
    private static float visualiserQuality = 100f;
    
    private static MuffinScreen.Mode mode;
    private static MuffinScreen.Palette palette;
    
    private enum Palette {
        DEFAULT
    }
    
    private enum Mode {
        CUBE, MUFFIN, FLAT
    }
    
    public MuffinScreen(Game game) {
        super(game, LEFT_AND_RIGHT_RAW);
        
        cam = new PerspectiveCamera(67, WIDTH, HEIGHT);
        cam.position.set(10f, 10f, 10f);
        cam.lookAt(0, 0, 0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();
        viewport = new ScreenViewport(cam);
        
        models = new Array<>();
        instances = new Array<>();
        modelYPoses = new Array<>();
        cachedPositions = new Array<>();
        cachedPositions.setSize(51);
        
        modelBuilder = new ModelBuilder();
        modelBatch = new ModelBatch();
        environment = new Environment();
        
        utils.changeBloomEnabledState(mode == Mode.CUBE);
        
        initialiseScene();
    }
    
    @Override
    public void show() {
        super.show();
    }
    
    @Override
    public void render(float delta) {
        
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        
        int pos;
        if (render) {
            frame += step;
            recorderFrame++;
            pos = frame;
        } else {
            pos = (int) (music.getPosition() * sampleRate);
        }
        
        transform3d(pos);
        
        utils.bloomBegin(true, pos);
        modelBatch.begin(cam);
        
        for (int i = instances.size - 1; i >= 0; i--) {
            modelBatch.render(instances.get(i), environment);
        }
        
        modelBatch.end();
        utils.bloomRender();
        
        if (render) {
            utils.makeAScreenShot(recorderFrame);
            utils.displayData(recorderFrame, frame, camera.combined);
        }
        
        batch.begin();
        drawExitButton();
        batch.end();
        
        if (pos > step * 51 && !preloaded) {
            preloaded = true;
        }
        
    }
    
    private void transform3d(final int pos) {
        switch (mode) {
            case CUBE:
                degrees += samplesSmoothed[pos] * 2;
                
                instances.get(0).transform.setToScaling(samplesSmoothed[pos] * 0.8f + 1, samplesSmoothed[pos] * 0.8f + 1, samplesSmoothed[pos] * 0.8f + 1);
                instances.get(0).transform.rotate(new Vector3(1, 0, 0), degrees);
                instances.get(0).transform.rotate(new Vector3(0, 1, 0), degrees);
                instances.get(0).transform.rotate(new Vector3(0, 0, 1), degrees);
                
                fadeColor = new Color().fromHsv(160 - samplesSmoothed[pos] * 120, 1f, 1).add(0, 0, 0, 1);
                
                environment.remove(prevLight);
                prevLight = new DirectionalLight().set(fadeColor.r, fadeColor.g, fadeColor.b, -1f, -0.8f, -0.2f);
                environment.add(prevLight);
                
                break;
            case MUFFIN:
                for (int i = 0; i < 51; i++) {
                    transformInRadius(i, pos);
                }
                break;
            case FLAT:
                rubensTransform(pos);
                break;
        }
    }
    
    private void initialiseScene() {
        switch (mode) {
            case CUBE:
                
                environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
                prevLight = new DirectionalLight().set(0, 1, 1, -1f, -0.8f, -0.2f);
                environment.add(prevLight);
                
                Model model = modelBuilder.createSphere(5, 5, 5, 10, 10,
                        new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                
                ModelInstance instance = new ModelInstance(model);
                
                models.add(model);
                instances.add(instance);
                
                break;
            case MUFFIN:
            case FLAT:
                
                environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1, 1, 1, 1f));
                prevLight = new DirectionalLight().set(0, 1, 1, -1f, -0.8f, -0.2f);
                environment.add(prevLight);
                
                float visualiserQuality = MuffinScreen.visualiserQuality;
                if (mode == Mode.MUFFIN) {
                    visualiserQuality = 100;
                }
                
                for (int x = 0; x < 101 * visualiserQuality / 100f; x++) {
                    for (int z = 0; z < 101 * visualiserQuality / 100f; z++) {
                        Model model2 = modelBuilder.createBox(0.5f * 100 / visualiserQuality, 8f, 0.5f * 100 / visualiserQuality,
                                new Material(ColorAttribute.createDiffuse(Color.valueOf("#00000000")), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)),
                                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                        
                        ModelInstance instance2 = new ModelInstance(model2);
                        
                        instance2.transform.translate(-x * 0.5f * 100 / visualiserQuality, -17, -z * 0.5f * 100 / visualiserQuality);
                        
                        models.add(model2);
                        // TODO: 5/26/2021 probably dont need to create models, just instances
                        instances.add(instance2);
                        modelYPoses.add(0f);
                        
                    }
                }
                
                if (mode == Mode.MUFFIN) {
                    Model filler = modelBuilder.createBox(300, 15f, 300,
                            new Material(ColorAttribute.createDiffuse(Color.valueOf("#000000"))),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    ModelInstance fillerInstance = new ModelInstance(filler);
                    fillerInstance.transform.translate(0, -20.8f, 0);
                    models.add(filler);
                    instances.add(fillerInstance);
                }
                
                cam.position.set(0f, 9f, 0f);
                cam.lookAt(-7, 0, -7);
                cam.update();
                
                utils.setBloomIntensity(0.3f);
                
                break;
        }
    }
    
    private void transformInRadius(int radius, int Mpos) {
        if (!preloaded) {
            float prevPos = 0;
            Array<Integer> positions = new Array<>();
            for (float i = 0; i < 360; i += 0.5f * 100 / visualiserQuality) {
                float x = -MathUtils.cosDeg(i) * radius + 49.5f;
                float y = -MathUtils.sinDeg(i) * radius + 49.5f;
                float x2 = x;
                float y2 = y;
                
                x = (float) Math.floor(Math.abs(x));
                y = (float) Math.floor(Math.abs(y));
                if (x2 < 0) {
                    x *= -1;
                }
                if (y2 < 0) {
                    y *= -1;
                }
                
                int pos = 101 * (int) x + (int) y;
                
                try {
                    if (pos != prevPos) {
                        
                        transformGridAtPos(pos, Mpos, radius);
                        
                        positions.add(pos);
                        prevPos = pos;
                    }
                } catch (Exception e) {
                    //ignore
                }
            }
            cachedPositions.set(radius, positions);
        } else {
            Array<Integer> positions = cachedPositions.get(radius);
            for (int i = 0; i < positions.size; i++) {
                transformGridAtPos(positions.get(i), Mpos, radius);
            }
        }
    }
    
    private void transformGridAtPos(final int pos, final int Mpos, final int radius) {
        
        float fadeFactor = radius / 51f + 1;
        
        instances.get(pos).transform.translate(0, samplesSmoothed[Mpos - radius * step] * 4 / fadeFactor - modelYPoses.get(pos), 0);
        
        fadeColor = new Color().fromHsv(-samplesSmoothed[Mpos - radius * step] * 180 / fadeFactor, 0.75f, 0.85f).add(0, 0, 0, 1);
        instances.get(pos).materials.get(0).set(ColorAttribute.createDiffuse(fadeColor));
        
        modelYPoses.set(pos, samplesSmoothed[Mpos - radius * step] * 4 / fadeFactor);
    }
    
    private void rubensTransform(int pos) {
        try {
            for (int x = 0; x < 101 * visualiserQuality / 100f; x++) {
                for (int y = 0; y < 101 * visualiserQuality / 100f; y++) {
                    int arrayPos = (int) Math.ceil(101 * visualiserQuality / 100f) * x + y;
                    
                    float height = (rSamplesNormalised[pos - x * step / 512] + lSamplesNormalised[pos - y * step / 512]);
                    
                    float currentTranslation = modelYPoses.get(arrayPos);
                    float nextTranslation = (modelYPoses.get(arrayPos) + height) / 1.2f;
                    
                    instances.get(arrayPos).transform.translate(0, nextTranslation - currentTranslation, 0);
                    modelYPoses.set(arrayPos, nextTranslation);
                    
                    fadeColor = new Color().fromHsv(nextTranslation * 40, 0.75f, 0.85f).add(0, 0, 0, 1);
                    
                    instances.get(arrayPos).materials.get(0).set(ColorAttribute.createDiffuse(fadeColor));
                }
            }
        } catch (Exception e) {
            //ignore
        }
    }
    
    public static void init() {
    
        settings = new Array<>();
        settings.add(new SettingsEntry("Visualiser quality", 0, 100, 100, Type.INT));
        settings.add(new SettingsEntry("Render", 0, 1, 0, Type.BOOLEAN));
        
        paletteNames = new Array<>();
        for (int i = 0; i < MuffinScreen.Palette.values().length; i++) {
            paletteNames.add(MuffinScreen.Palette.values()[i].name().toLowerCase(Locale.ROOT).replace("_", ""));
        }
    
        typeNames = new Array<>();
        for (int i = 0; i < MuffinScreen.Mode.values().length; i++) {
            typeNames.add(MuffinScreen.Mode.values()[i].name().toLowerCase(Locale.ROOT).replace("_", ""));
        }
        
    }
    
    public static String getName() {
        return "3D";
    }
    
    public static void setSettings(int mode, int palette) {
        MuffinScreen.mode = MuffinScreen.Mode.values()[mode];
        MuffinScreen.palette = MuffinScreen.Palette.values()[palette];
        visualiserQuality = getSettingByName("Visualiser quality");
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
        modelBatch.dispose();
        for (int i = 0; i < models.size; i++) {
            models.get(i).dispose();
        }
        models.clear();
    }
}
