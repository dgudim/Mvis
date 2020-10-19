package com.deo.golly;

import com.badlogic.gdx.Application;
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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.deo.golly.postprocessing.PostProcessor;
import com.deo.golly.postprocessing.effects.Bloom;

public class ThreeDScreen implements Screen {

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

    ThreeDScreen() {

        MusicWave musicWave = new MusicWave();
        music = musicWave.getMusic();

        rSamplesNormalised = musicWave.normaliseSamples(false, true, musicWave.getRightChannelSamples());
        lSamplesNormalised = musicWave.normaliseSamples(false, true, musicWave.getLeftChannelSamples());
        averageSamplesNormalised = musicWave.normaliseSamples(false, true, musicWave.getSamples());

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
                new Material(ColorAttribute.createReflection(Color.WHITE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

        for(int i = 0; i<model.nodes.size; i++){
            System.out.println(model.nodes.get(i));
        }

        //modelBuilder.createRect();

        instance = new ModelInstance(model);

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1f));
        environment.add(new DirectionalLight().set(0, 1, 1, -1f, -0.8f, -0.2f));

        ShaderLoader.BasePath = "core/assets/shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(3);
        bloom.setBloomIntesity(1f);
        blurProcessor.addEffect(bloom);

        music.play();
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        degrees += averageSamplesNormalised[(int) (music.getPosition()*44100)]*2;

        instance.transform.setToScaling(averageSamplesNormalised[(int) (music.getPosition()*44100)]*0.8f+1, averageSamplesNormalised[(int) (music.getPosition()*44100)]*0.8f+1, averageSamplesNormalised[(int) (music.getPosition()*44100)]*0.8f+1);
        instance.transform.rotate(new Vector3(1, 0, 0), degrees);
        instance.transform.rotate(new Vector3(0, 1, 0), degrees);
        instance.transform.rotate(new Vector3(0, 0, 1), degrees);

        blurProcessor.capture();
        modelBatch.begin(cam);
        modelBatch.render(instance, environment);
        modelBatch.end();
        blurProcessor.render();
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
