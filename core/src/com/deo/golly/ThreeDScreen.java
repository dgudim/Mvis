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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
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
    private Model model;
    private ModelInstance instance;
    private Environment environment;

    private Music music;

    private float[] rSamplesNormalised;
    private float[] lSamplesNormalised;
    private float[] averageSamplesNormalised;

    private PostProcessor blurProcessor;
    private Bloom bloom;
    private float degrees;

    private final int FPS = 30;
    private final int step = 44100 / FPS;
    private final boolean render = false;
    private int frame;
    private int recorderFrame;

    private Color fadeColor;
    private DirectionalLight prevLight;

    ThreeDScreen() {

        MusicWave musicWave = new MusicWave();
        music = musicWave.getMusic();

        rSamplesNormalised = musicWave.normaliseSamples(false, true, musicWave.getRightChannelSamples());
        lSamplesNormalised = musicWave.normaliseSamples(false, true, musicWave.getLeftChannelSamples());

        averageSamplesNormalised = musicWave.smoothSamples(musicWave.getSamples(), 2, 32);

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        cam = new PerspectiveCamera(67, MainScreen.WIDTH, MainScreen.HEIGHT);
        cam.position.set(10f, 10f, 10f);
        cam.lookAt(0, 0, 0);
        cam.near = 1f;
        cam.far = 300f;
        cam.update();

        modelBuilder = new ModelBuilder();

        model = modelBuilder.createBox(5, 5, 5,
                new Material(ColorAttribute.createDiffuse(Color.WHITE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        instance = new ModelInstance(model);

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
        prevLight = new DirectionalLight().set(0, 1, 1, -1f, -0.8f, -0.2f);
        environment.add(prevLight);

        ShaderLoader.BasePath = "core/assets/shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(3);
        bloom.setBloomIntesity(1f);
        blurProcessor.addEffect(bloom);

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
        if(render){
            frame += step;
            recorderFrame++;
            pos = frame;
        }else{
            pos = (int) (music.getPosition() * 44100);
        }

        bloom.setBloomSaturation(Math.abs(averageSamplesNormalised[pos]) + 1);

        degrees += averageSamplesNormalised[pos] * 2;

        instance.transform.setToScaling(averageSamplesNormalised[pos] * 0.8f + 1, averageSamplesNormalised[pos] * 0.8f + 1, averageSamplesNormalised[pos] * 0.8f + 1);
        instance.transform.rotate(new Vector3(1, 0, 0), degrees);
        instance.transform.rotate(new Vector3(0, 1, 0), degrees);
        instance.transform.rotate(new Vector3(0, 0, 1), degrees);

        fadeColor = new Color().fromHsv(120 - averageSamplesNormalised[pos] * 120, 1f, 1).add(0, 0, 0, 1);

        environment.remove(prevLight);
        prevLight = new DirectionalLight().set(fadeColor.r, fadeColor.g, fadeColor.b, -1f, -0.8f, -0.2f);
        environment.add(prevLight);

        blurProcessor.capture();
        modelBatch.begin(cam);
        modelBatch.render(instance, environment);
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
            batch.end();
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
