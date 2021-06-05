package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.badlogic.gdx.math.MathUtils.clamp;
import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.random;
import static java.lang.StrictMath.sin;

public class AttractorScreen extends BaseVisualiser implements Screen {
    
    private final PerspectiveCamera cam;
    
    private final ModelBatch modelBatch;
    private final Environment environment;
    
    private static int currentPalette;
    private static int currentSimulationRule;
    private static int pointCount;
    private static float timestep;
    
    float spread = 3;
    
    private final Array<Point> points;
    
    CameraInputController cameraInputController;
    
    Color[][] availablePalettes = new Color[][]{
            {Color.valueOf("#cc700099"), Color.ORANGE, Color.CORAL, Color.RED},
            {Color.valueOf("#44132199"), Color.valueOf("#662341"), Color.valueOf("#ffe240"), Color.FIREBRICK},
            {Color.valueOf("#cc700099"), Color.ORANGE, Color.RED, Color.GRAY},
            {Color.valueOf("12691299"), Color.LIME, Color.SKY, Color.CYAN},
            {Color.valueOf("#00334499"), Color.TEAL, Color.SKY, Color.CYAN}};
    
    public AttractorScreen(Game game) {
        super(game, DEFAULT);
        
        utils.setBloomSaturation(1);
        utils.setBloomIntensity(1.3f);
        utils.maxSaturation = 1.3f;
        
        points = new Array<>();
        for (int i = 0; i < pointCount; i++) {
            points.add(new Point(new Vector3((float) (random() * spread), (float) (random() * spread), (float) (random() * spread)), availablePalettes[currentPalette],
                    currentSimulationRule, timestep/3f));
        }
        
        cam = new PerspectiveCamera(67, WIDTH, HEIGHT);
        cam.position.set(10f, 10f, 10f);
        cam.lookAt(0, 0, 0);
        cam.near = 0f;
        cam.far = 500f;
        cam.update();
        viewport = new ScreenViewport(cam);
        
        modelBatch = new ModelBatch();
        
        environment = new Environment();
        
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
        
        cameraInputController = new CameraInputController(cam);
    
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(cameraInputController);
        multiplexer.addProcessor(stage);
        
        Gdx.input.setInputProcessor(multiplexer);
    }
    
    @Override
    public void show() {
        Gdx.gl.glLineWidth(2.1f);
    }
    
    @Override
    public void render(float delta) {
        
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        
        cameraInputController.update();
        
        int pos;
        if (render) {
            pos = frame;
            frame += step;
            recorderFrame++;
        } else {
            pos = (int) (music.getPosition() * sampleRate);
        }
        
        float volume = samplesSmoothed[pos];
    
        utils.bloomBegin(true, pos);
        modelBatch.begin(cam);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        
        for (int i = 0; i < points.size; i++) {
            points.get(i).render(environment, modelBatch);
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
        
        for (int i = 0; i < points.size; i++) {
            points.get(i).advance(volume);
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            for (int i = 0; i < points.size; i++) {
                points.get(i).resetTrail();
            }
        }
    }
    
    public static void init() {
        paletteNames = new String[]{"Vivid", "Orange", "Fire", "Virus", "Black hole"};
        typeNames = new String[]{"Attractor1", "Attractor2", "Attractor3", "Attractor4", "Attractor5", "Attractor6"};
        
        settings = new String[]{"Type", "Pallet", "Number of points", "TimeStep", "Render"};
        settingTypes = new String[]{"int", "int", "int", "float", "boolean"};
        
        settingMaxValues = new float[]{typeNames.length - 1, paletteNames.length - 1, 450, 0.5f, 1};
        settingMinValues = new float[]{0, 0, 10, 0.0001f, 0};
        
        defaultSettings = new float[]{0, 0, 70, 0.01f, 0};
    }
    
    public static String getName() {
        return "Attractor";
    }
    
    public static void setSettings(float[] newSettings) {
        currentSimulationRule = (int) newSettings[0];
        currentPalette = (int) newSettings[1];
        pointCount = (int) newSettings[2];
        timestep = newSettings[3];
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
        modelBatch.dispose();
        for (int i = 0; i < points.size; i++) {
            points.get(i).dispose();
        }
        points.clear();
    }
}

class Point {
    
    private final ModelBuilder modelBuilder;
    
    Array<Vector3> points;
    Array<Model> models;
    Array<ModelInstance> instances;
    Array<Color> colors;
    int maxPoints = 70;
    float fadeStep = 1 / (float) maxPoints;
    float timestep;
    
    int attractorType;
    
    private final Color[] palette;
    
    Point(Vector3 startingPoint, Color[] palette, int attractorType, float timestep) {
        
        this.timestep = timestep;
        this.palette = palette;
        this.attractorType = attractorType;
        
        modelBuilder = new ModelBuilder();
        
        points = new Array<>();
        colors = new Array<>();
        models = new Array<>();
        instances = new Array<>();
        
        init(startingPoint);
    }
    
    void init(Vector3 startingPoint) {
        for (int i = 0; i < maxPoints; i++) {
            points.add(startingPoint);
            colors.add(palette[palette.length - 1]);
            Model bulk = new Model();
            ModelInstance bulkInstance = new ModelInstance(bulk);
            models.add(bulk);
            instances.add(bulkInstance);
        }
    }
    
    private void shift(float volume) {
        
        for (int n = 0; n < 2 + volume * 3; n++) {
            for (int i = 0; i < instances.size - 1; i++) {
                colors.set(i, new Color(colors.get(i + 1).r, colors.get(i + 1).g, colors.get(i + 1).b, clamp(colors.get(i + 1).a - fadeStep, 0, 1)));
            }
        }
        for (int i = 0; i < instances.size - 1; i++) {
            
            points.set(i, points.get(i + 1));
            models.set(i, models.get(i + 1));
            instances.set(i, instances.get(i + 1));
            
            try {
                instances.get(i).materials.get(0).set(
                        ColorAttribute.createDiffuse(colors.get(i)),
                        new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
            } catch (Exception e) {
                //ignore
            }
        }
    }
    
    void advance(float volume) {
        for (int i = 0; i < 1 + volume * 3; i++) {
            shift(volume);
            Vector3 prevPos = points.get(points.size - 1);
            Color color = new Color(interpolate(volume, 1, palette));
            colors.set(colors.size - 1, color);
            
            points.set(points.size - 1, calculateNextPosition(prevPos));
            
            addLine(points.get(points.size - 2), points.get(points.size - 1), color);
        }
        
        
    }
    
    void resetTrail() {
        dispose();
        instances.clear();
        colors.clear();
        Vector3 prevPos = points.get(points.size - 1);
        points.clear();
        init(prevPos);
    }
    
    Vector3 calculateNextPosition(Vector3 position) {
        
        float startX = position.x;
        float startY = position.y;
        float startZ = position.z;
        
        float dX;
        float dY;
        float dZ;
        
        switch (attractorType) {
            case (0):
                dX = (startZ - 0.7f) * startX - 3.5f * startY;
                dY = 3.5f * startX + (startZ - 0.7f) * startY;
                dZ = 0.6f + 0.95f * startZ - (startZ * startZ * startZ) / 3f - (startX * startX + startY * startY) * (1 + 0.25f * startZ) + 0.1f * startZ * startX * startX * startX;
                break;
            case (1):
                dX = (float) (sin(-0.759 * startY) - startZ * cos(2.449 * startX));
                dY = (float) (startZ * sin(1.253 * startX) - cos(1.5 * startY));
                dZ = (float) sin(startX);
                break;
            case (2):
                dX = 40 * (startY - startX) + 0.5f * startX * startZ;
                dY = 20 * startY - startX * startZ;
                dZ = 0.8333f * startZ + startX * startY - 0.65f * startX * startX;
                break;
            case (3):
                dX = startY;
                dY = -startX + startY * startZ;
                dZ = 1 - startY * startY;
                break;
            case (4):
                dX = -1.4f * startX - 4 * startY - 4 * startZ - startY * startY;
                dY = -1.4f * startY - 4 * startZ - 4 * startX - startZ * startZ;
                dZ = -1.4f * startZ - 4 * startX - 4 * startY - startX * startX;
                break;
            case (5):
            default:
                dX = startY;
                dY = (1 - startZ) * startX - 0.75f * startY;
                dZ = startX * startX - 0.45f * startZ;
                break;
        }
        
        dX *= timestep;
        dY *= timestep;
        dZ *= timestep;
        
        return new Vector3(startX + dX, startY + dY, startZ + dZ);
    }
    
    void render(Environment environment, ModelBatch batch) {
        for (int i = instances.size - 1; i >= 0; i--) {
            batch.render(instances.get(i), environment);
        }
    }
    
    void addLine(Vector3 beginning, Vector3 end, Color color) {
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("line", 1, 3, new Material(ColorAttribute.createDiffuse(color), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)));
        builder.line(beginning, end);
        Model model = modelBuilder.end();
        
        ModelInstance instance = new ModelInstance(model);
        
        models.set(models.size - 1, model);
        instances.set(instances.size - 1, instance);
        
    }
    
    static int interpolate(float step, float maxValue, Color... colors) {
        step = Math.max(Math.min(step / maxValue, 1.0f), 0.0f);
        
        switch (colors.length) {
            case 0:
                throw new IllegalArgumentException("At least one color required.");
            
            case 1:
                return Color.argb8888(colors[0]);
            
            case 2:
                return mixTwoColors(colors[0], colors[1], step);
            
            default:
                
                int firstColorIndex = (int) (step * (colors.length - 1));
                
                if (firstColorIndex == colors.length - 1) {
                    return Color.argb8888(colors[colors.length - 1]);
                }
                
                // stepAtFirstColorIndex will be a bit smaller than step
                float stepAtFirstColorIndex = (float) firstColorIndex
                        / (colors.length - 1);
                
                // multiply to increase values to range between 0.0f and 1.0f
                float localStep = (step - stepAtFirstColorIndex)
                        * (colors.length - 1);
                
                return mixTwoColors(colors[firstColorIndex],
                        colors[firstColorIndex + 1], localStep);
        }
        
    }
    
    static int mixTwoColors(Color color1, Color color2, float ratio) {
        return Color.rgba8888(color1.r * (1f - ratio) + color2.r * ratio, color1.g * (1f - ratio) + color2.g * ratio, color1.b * (1f - ratio) + color2.b * ratio, color1.a * (1f - ratio) + color2.a * ratio);
    }
    
    void dispose() {
        for (int i = 0; i < models.size; i++) {
            models.get(i).dispose();
        }
        models.clear();
    }
    
}

