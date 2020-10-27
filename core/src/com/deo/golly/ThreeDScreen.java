package com.deo.golly;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class ThreeDScreen implements Screen {

    private PerspectiveCamera cam;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private Array<Model> models;
    private Array<Float> modelYPoses;
    private Array<ModelInstance> instances;
    private Array<Array<Integer>> cachedPositions;
    private Environment environment;
    boolean preloaded;

    private Music music;

    private float[] rSamplesNormalised;
    private float[] lSamplesNormalised;
    private float[] averageSamplesNormalised;

    private float degrees;

    private final int FPS = 30;
    private final int step = 44100 / FPS;
    private final boolean render = false;
    private int frame;
    private int recorderFrame;

    private Color fadeColor;
    private DirectionalLight prevLight;

    private final int SINGULAR = 0;
    private final int GRID = 1;
    private final int RUBENSTUBE = 2;

    private final int type = RUBENSTUBE;

    private Utils utils;

    ThreeDScreen() {

        MusicWave musicWave = new MusicWave();
        music = musicWave.getMusic();

        rSamplesNormalised = musicWave.smoothSamples(musicWave.getRightChannelSamples(), 1, 32);
        lSamplesNormalised = musicWave.smoothSamples(musicWave.getLeftChannelSamples(), 1, 32);
        averageSamplesNormalised = musicWave.smoothSamples(musicWave.getSamples(), 2, 32);

        cam = new PerspectiveCamera(67, MainScreen.WIDTH, MainScreen.HEIGHT);
        cam.position.set(10f, 10f, 10f);
        cam.lookAt(0, 0, 0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        models = new Array<>();
        instances = new Array<>();
        modelYPoses = new Array<>();
        cachedPositions = new Array<>();
        cachedPositions.setSize(51);

        modelBuilder = new ModelBuilder();

        modelBatch = new ModelBatch();

        environment = new Environment();

        utils = new Utils(FPS, step, averageSamplesNormalised, 3, 1, 1, type == SINGULAR);

        initialiseScene();

        if (!render) {
            music.play();
        }

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
            if(type == GRID && render){
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
                degrees += averageSamplesNormalised[pos] * 2;

                instances.get(0).transform.setToScaling(averageSamplesNormalised[pos] * 0.8f + 1, averageSamplesNormalised[pos] * 0.8f + 1, averageSamplesNormalised[pos] * 0.8f + 1);
                instances.get(0).transform.rotate(new Vector3(1, 0, 0), degrees);
                instances.get(0).transform.rotate(new Vector3(0, 1, 0), degrees);
                instances.get(0).transform.rotate(new Vector3(0, 0, 1), degrees);

                fadeColor = new Color().fromHsv(160 - averageSamplesNormalised[pos] * 120, 1f, 1).add(0, 0, 0, 1);

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

        instances.get(pos).transform.translate(0, averageSamplesNormalised[Mpos - radius * step] * 4 / fadeFactor - modelYPoses.get(pos), 0);

        fadeColor = new Color().fromHsv(-averageSamplesNormalised[Mpos - radius * step] * 180 / fadeFactor, 0.75f, 0.85f).add(0, 0, 0, 1);
        instances.get(pos).materials.get(0).set(ColorAttribute.createDiffuse(fadeColor));

        modelYPoses.set(pos, averageSamplesNormalised[Mpos - radius * step] * 4 / fadeFactor);
    }

    private void rubensTransform(int pos) {
        try {
            for (int x = 0; x < 101; x++) {
                for (int y = 0; y < 101; y++) {
                    int arrayPos = 101 * x + y;

                    float prevHeight = 0;
                    for (int i = 1; i < FPS * 1.5; i++) {
                        prevHeight += rSamplesNormalised[pos - x * (step - i) / 128] / (i/2f);
                        prevHeight += lSamplesNormalised[pos - y * (step - i) / 128] / (i/2f);
                    }

                    float nextHeight = 0;
                    for (int i = 1; i < FPS * 1.5; i++) {
                        nextHeight += rSamplesNormalised[pos - x * (step + i) / 128] / (i/2f);
                        nextHeight += lSamplesNormalised[pos - y * (step + i) / 128] / (i/2f);
                    }

                    float height = (rSamplesNormalised[pos - x * step / 128] + lSamplesNormalised[pos - y * step / 128])*2 + prevHeight + nextHeight;

                    height /= (FPS/1.3f);

                    instances.get(arrayPos).transform.translate(0, height * 2 - modelYPoses.get(arrayPos), 0);
                    modelYPoses.set(arrayPos, height * 2);

                    fadeColor = new Color().fromHsv(height * 80, 0.75f, 0.85f).add(0, 0, 0, 1);
                    instances.get(arrayPos).materials.get(0).set(ColorAttribute.createDiffuse(fadeColor));
                }
            }
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    public void resize(int width, int height) {

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
    }
}
