package com.deo.mvis.visualisers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
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
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.Utils;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

public class MuffinScreen extends BaseVisualiser implements Screen{

    private PerspectiveCamera cam;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private Array<Model> models;
    private Array<Float> modelYPoses;
    private Array<ModelInstance> instances;
    private Array<Array<Integer>> cachedPositions;
    private Environment environment;
    boolean preloaded;

    private float degrees;

    private Color fadeColor;
    private DirectionalLight prevLight;

    private final int SINGULAR = 0;
    private final int GRID = 1;
    private final int RUBENSTUBE = 2;

    private static int type;
    private static int palette;

    public MuffinScreen() {

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

        initialiseScene();
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);

        int pos;
        if (render) {
            frame += step;
            recorderFrame++;
            pos = frame;
        } else {
            pos = (int) (music.getPosition() * 44100);
        }

        transform3d(pos);

        utils.bloomBegin(true, pos);
        modelBatch.begin(cam);
        for (int i = instances.size - 1; i >= 0; i--) {
            modelBatch.render(instances.get(i), environment);
            if (type == GRID && render) {
                modelBatch.flush();
            }
        }
        modelBatch.end();
        utils.bloomRender();

        if (render) {
            utils.makeAScreenShot(recorderFrame);
            utils.displayData(recorderFrame, frame);
        }

        if (pos > step * 51 && !preloaded) {
            preloaded = true;
        }

    }

    private void transform3d(int pos) {
        switch (type) {
            case (SINGULAR):
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
            case (GRID):

                for (int i = 0; i < 51; i++) {
                    transformInRadius(i, pos);
                }

                break;
            case (RUBENSTUBE):

                rubensTransform(pos);

                break;
        }
    }

    private void initialiseScene() {
        switch (type) {
            case (SINGULAR):

                environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
                prevLight = new DirectionalLight().set(0, 1, 1, -1f, -0.8f, -0.2f);
                environment.add(prevLight);

                Model model = modelBuilder.createBox(5, 5, 5,
                        new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

                ModelInstance instance = new ModelInstance(model);

                models.add(model);
                instances.add(instance);

                break;
            case (GRID):
            case (RUBENSTUBE):

                environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 1, 1, 1, 1f));
                prevLight = new DirectionalLight().set(0, 1, 1, -1f, -0.8f, -0.2f);
                environment.add(prevLight);

                for (int x = 0; x < 101; x++) {
                    for (int z = 0; z < 101; z++) {
                        Model model2 = modelBuilder.createBox(0.5f, 8f, 0.5f,
                                new Material(ColorAttribute.createDiffuse(Color.valueOf("#00000000")), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)),
                                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

                        ModelInstance instance2 = new ModelInstance(model2);

                        instance2.transform.translate(-x * 0.5f, -17, -z * 0.5f);

                        models.add(model2);
                        instances.add(instance2);
                        modelYPoses.add(0f);

                    }
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
            for (float i = 0; i < 360; i += 0.01f) {
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
            for (int x = 0; x < 101; x++) {
                for (int y = 0; y < 101; y++) {
                    int arrayPos = 101 * x + y;

                    float height = (rSamplesNormalised[pos - x * step / 128] + lSamplesNormalised[pos - y * step / 128]) * 2;

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
        paletteNames = new String[]{"Default"};
        typeNames = new String[]{"Muffin", "Flat"};

        settings = new String[]{"Type", "Pallet"};
        settingTypes = new String[]{"int", "int"};
        settingMaxValues = new float[]{typeNames.length, paletteNames.length};
    }

    public static String getName(){
        return "3D";
    }

    public static void setSettings(float[] newSettings) {
        type = (int) newSettings[0];
        palette = (int) newSettings[1];
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        cam.update();
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
        for (int i = 0; i < models.size; i++) {
            models.get(i).dispose();
        }
        models.clear();
        utils.dispose();
    }
}
