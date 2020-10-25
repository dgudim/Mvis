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
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deo.golly.postprocessing.PostProcessor;
import com.deo.golly.postprocessing.effects.Bloom;

public class ThreeDScreen implements Screen {

    private SpriteBatch batch;
    private BitmapFont font;
    private ShapeRenderer renderer;
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

    private PostProcessor blurProcessor;
    private Bloom bloom;
    private float degrees;

    private final int FPS = 30;
    private final int step = 44100 / FPS;
    private final boolean render = true;
    private int frame;
    private int recorderFrame;

    private Color fadeColor;
    private DirectionalLight prevLight;

    private final int SINGULAR = 0;
    private final int GRID = 1;
    private final int RUBENSTUBE = 2;

    private final int type = RUBENSTUBE;

    ThreeDScreen() {

        MusicWave musicWave = new MusicWave();
        music = musicWave.getMusic();

        rSamplesNormalised = musicWave.smoothSamples(musicWave.getRightChannelSamples(), 1, 32);
        lSamplesNormalised = musicWave.smoothSamples(musicWave.getLeftChannelSamples(), 1, 32);
        averageSamplesNormalised = musicWave.smoothSamples(musicWave.getSamples(), 2, 32);

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

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

        ShaderLoader.BasePath = "core/assets/shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(3);
        bloom.setBloomIntesity(1f);
        blurProcessor.addEffect(bloom);

        batch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("core/assets/font2(old).fnt"));

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

        int pos;
        if (render) {
            frame += step;
            recorderFrame++;
            pos = frame;
        } else {
            pos = (int) (music.getPosition() * 44100);
        }

        bloom.setBloomSaturation(Math.abs(averageSamplesNormalised[pos]) + 1);

        transform3d(pos);

        blurProcessor.capture();
        modelBatch.begin(cam);
        for (int i = instances.size - 1; i >= 0; i--) {
            modelBatch.render(instances.get(i), environment);
            modelBatch.flush();
        }
        modelBatch.end();
        blurProcessor.render();

        if (render) {
            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

            for (int i4 = 4; i4 < pixels.length; i4 += 4) {
                pixels[i4 - 1] = (byte) 255;
            }

            Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
            BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
            PixmapIO.writePNG(Gdx.files.external("GollyRender/pict" + recorderFrame + ".png"), pixmap);
            pixmap.dispose();

            batch.begin();
            font.draw(batch, String.format("% 2f", recorderFrame / (float) FPS) + "s", 100, 120);
            boolean normal = frame / (float) 44100 == recorderFrame / (float) FPS;
            font.draw(batch, frame + "fr " + recorderFrame + "fr " + normal, 100, 170);
            font.draw(batch, frame / (float) averageSamplesNormalised.length * 100 + "%", 100, 70);
            font.draw(batch, addAndComputeTime() + "h", 100, 220);
            batch.end();
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
            case(RUBENSTUBE):

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
            case(RUBENSTUBE):

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

                bloom.setBloomIntesity(0.3f);

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

    private float addAndComputeTime() {
        return Gdx.graphics.getDeltaTime() * (averageSamplesNormalised.length / (float) step - recorderFrame) / 3600;
    }

    private void rubensTransform(int pos){
        try{
            for (int x = 0; x < 101; x++) {
                for (int y = 0; y < 101; y++) {
                    int arrayPos = 101 * x + y;
                    float height = rSamplesNormalised[pos - x * step / 128] + lSamplesNormalised[pos - y * step / 128];
                    instances.get(arrayPos).transform.translate(0, height * 2 - modelYPoses.get(arrayPos), 0);
                    modelYPoses.set(arrayPos, height * 2);

                    fadeColor = new Color().fromHsv(height * 80, 0.75f, 0.85f).add(0, 0, 0, 1);
                    instances.get(arrayPos).materials.get(0).set(ColorAttribute.createDiffuse(fadeColor));
                }
            }
        }catch (Exception e){
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
