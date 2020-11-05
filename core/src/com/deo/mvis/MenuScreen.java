package com.deo.mvis;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.jtransforms.fft.FloatFFT_1D;
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.UIComposer;
import com.deo.mvis.visualisers.FFTScreen;
import com.deo.mvis.visualisers.MushroomScreen;
import com.deo.mvis.visualisers.OsciloscopeScreen;
import com.deo.mvis.visualisers.RingScreen;
import com.deo.mvis.visualisers.ThreeDScreen;

import java.util.Arrays;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

public class MenuScreen implements Screen {

    private Stage visualisers;
    private Table visualisersTable;
    private UIComposer uiComposer;
    private AssetManager assetManager;

    private ShapeRenderer renderer;

    private OrthographicCamera camera;
    private ScreenViewport viewport;

    private SpriteBatch batch;

    private BitmapFont font, font2;

    private MusicWave musicWave;
    private float[] displaySamples;
    private float[] averageSamples;
    private Music music;
    private FloatFFT_1D fft;

    private float triangleAngle = 0;
    private float triangleAnimation = 700;
    boolean musicStarted = false;

    MenuScreen(final Game game) {

        musicWave = new MusicWave(Gdx.files.internal("liquid.wav"));

        music = musicWave.getMusic();

        averageSamples = musicWave.smoothSamples(musicWave.getSamples(), 2, 32);

        music.setLooping(true);

        fft = new FloatFFT_1D(32);

        displaySamples = new float[32];
        Arrays.fill(displaySamples, 0);

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        camera = new OrthographicCamera(1600, 900);
        viewport = new ScreenViewport(camera);
        batch = new SpriteBatch();

        assetManager = new AssetManager();

        assetManager.load("menuButtons.atlas", TextureAtlas.class);
        assetManager.load("font2(old).fnt", BitmapFont.class);
        assetManager.load("font2.fnt", BitmapFont.class);

        while (!assetManager.isFinished()) {
            assetManager.update();
        }

        uiComposer = new UIComposer(assetManager);
        uiComposer.loadStyles("defaultLight");

        visualisersTable = new Table();

        visualisers = new Stage(viewport, batch);
        visualisersTable.setBounds(-WIDTH/2f, -HEIGHT/2f, WIDTH/2f - 10, HEIGHT/2f + 45);

        TextButton FftVisualiserButton = uiComposer.addTextButton("defaultLight", "Frequency Visualiser", 0.45f);
        FftVisualiserButton.setColor(Color.LIGHT_GRAY);

        TextButton RingVisualiserButton = uiComposer.addTextButton("defaultLight", "Ring Visualiser", 0.45f);
        RingVisualiserButton.setColor(Color.LIGHT_GRAY);

        TextButton FractalVisualiserButton = uiComposer.addTextButton("defaultLight", "Fractal Visualiser", 0.45f);
        FractalVisualiserButton.setColor(Color.LIGHT_GRAY);

        TextButton OscilloscopeVisualiserButton = uiComposer.addTextButton("defaultLight", "Mixed Visualiser", 0.45f);
        OscilloscopeVisualiserButton.setColor(Color.LIGHT_GRAY);

        TextButton ThreeDVisualiserButton = uiComposer.addTextButton("defaultLight", "3D Visualiser", 0.45f);
        ThreeDVisualiserButton.setColor(Color.LIGHT_GRAY);

        RingVisualiserButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new RingScreen());
                MenuScreen.this.dispose();
            }
        });

        FftVisualiserButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new FFTScreen());
                MenuScreen.this.dispose();
            }
        });

        ThreeDVisualiserButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new ThreeDScreen());
                MenuScreen.this.dispose();
            }
        });

        FractalVisualiserButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MushroomScreen());
                MenuScreen.this.dispose();
            }
        });

        OscilloscopeVisualiserButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new OsciloscopeScreen());
                MenuScreen.this.dispose();
            }
        });

        visualisersTable.add(FftVisualiserButton).width(370).height(100).pad(7);
        visualisersTable.add(RingVisualiserButton).width(370).height(100).pad(7).row();
        visualisersTable.add(FractalVisualiserButton).width(370).height(100).pad(7);
        visualisersTable.add(OscilloscopeVisualiserButton).width(370).height(100).pad(7).row();
        visualisersTable.add(ThreeDVisualiserButton).width(370).height(100).pad(7);

        visualisersTable.setPosition(-WIDTH/2f, -HEIGHT/2f);

        visualisers.addActor(visualisersTable);

        font = assetManager.get("font2.fnt");
        font.getData().scale(0.5f);

        font2 = assetManager.get("font2(old).fnt");
        font2.getData().scale(0.4f);

    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(visualisers);
    }

    @Override
    public void render(float delta) {

        int pos = (int) (music.getPosition() * 44100);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.7f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);

        float size = MathUtils.clamp(averageSamples[pos]*25+725 - triangleAnimation, 0, 725);
        float size2 = size + 25;

        float[] triangle = makeATriangle(size);
        float[] triangle2 = makeATriangle(size2);

        renderer.setColor(Color.valueOf("#558581"));
        renderer.triangle(triangle2[0], triangle2[1], triangle2[2], triangle2[3], triangle2[4], triangle2[5]);
        renderer.setColor(Color.valueOf("#88b2a1"));
        renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);

        renderer.setColor(0, 0, 0, 0.55f);
        float width = viewport.getScreenWidth();
        float height = viewport.getScreenHeight();
        renderer.rect(-width/2, -height/2, width, height);

        triangleAngle += 10*delta;

        if(triangleAnimation > 35){
            triangleAnimation /= 1.01f;
        }else if(!musicStarted){
            music.play();
            musicStarted = true;
        }

        renderer.end();

        visualisers.draw();
        visualisers.act(delta);

        batch.begin();

        //shadow
        font.setColor(Color.BLACK);
        font.draw(batch, "Welcome to Mvis V1.3", -WIDTH / 2f + 5, HEIGHT / 2f - 125, WIDTH, 1, false);

        //actual text
        font.setColor(Color.valueOf("#7799FF"));
        font.draw(batch, "Welcome to Mvis V1.3", -WIDTH / 2f, HEIGHT / 2f - 130, WIDTH, 1, false);

        //shadow
        font2.setColor(Color.BLACK);
        font2.draw(batch, "Available Visualisers:", -WIDTH/2f + 2, HEIGHT / 2f - 318, WIDTH/2f, 1, false);
        font2.draw(batch, "Visualiser Settings:", 2, HEIGHT / 2f - 318, WIDTH/2f, 1, false);

        //actual text
        font2.setColor(Color.valueOf("#77DD77"));
        font2.draw(batch, "Available Visualisers:", -WIDTH/2f, HEIGHT / 2f - 320, WIDTH/2f, 1, false);
        font2.draw(batch, "Visualiser Settings:", 0, HEIGHT / 2f - 320, WIDTH/2f, 1, false);

        batch.end();

        renderer.begin(ShapeRenderer.ShapeType.Filled);

        float[] samples = musicWave.getSamplesForFFT(pos, 32);
        fft.realForward(samples);

        float[] samples2 = new float[samples.length + 4];

        for (int t = 2; t < samples.length - 2; t++) {
            samples2[t] = samples[t - 2];
        }

        for (int t = 0; t < 2; t++) {
            for (int i = 2; i < samples2.length - 2; i++) {
                float neighbours = samples2[i - 2] + samples2[i + 2] + samples2[i - 1] + samples2[i + 1];
                samples2[i] = (Math.abs(neighbours) + Math.abs(samples2[i])) / 5f;
            }
        }

        for (int i = 2; i < samples2.length - 2; i++) {

            displaySamples[i-2] += samples2[i]/1.5f;

            renderer.setColor(new Color().fromHsv(displaySamples[i-2] / 2048, 0.75f, 0.9f));
            renderer.rect(i * 37.8f - samples.length / 2f * 37.8f - 62, 329, 11, displaySamples[i-2] / 512 + 0.5f);

            displaySamples[i-2] /= 1.3f;
        }


        float overHead = viewport.getScreenHeight() - HEIGHT;
        float overHeadHorizontal = viewport.getScreenWidth() - WIDTH;

        renderer.setColor(Color.BLACK);
        renderer.rect(-WIDTH/2f + 5 - overHeadHorizontal, HEIGHT/2f - 275, WIDTH + overHeadHorizontal*2, 20);
        renderer.rect(-5, -HEIGHT/2f + 5 - overHead, 20, HEIGHT - 280 + overHead);

        renderer.setColor(Color.CORAL);
        renderer.rect(-WIDTH/2f - overHeadHorizontal, HEIGHT/2f - 280, WIDTH + overHeadHorizontal*2, 20);
        renderer.rect(-10, -HEIGHT/2f - overHead, 20, HEIGHT - 280 + overHead);

        renderer.end();

        Gdx.gl20.glDisable(GL20.GL_BLEND);
    }

    private float[] makeATriangle(float size){
        float x1 = -MathUtils.cosDeg(triangleAngle) * size;
        float y1 = -MathUtils.sinDeg(triangleAngle) * size;

        float x2 = -MathUtils.cosDeg(120 + triangleAngle) * size;
        float y2 = -MathUtils.sinDeg(120 + triangleAngle) * size;

        float x3 = -MathUtils.cosDeg(240 + triangleAngle) * size;
        float y3 = -MathUtils.sinDeg(240 + triangleAngle) * size;
        return new float[]{x1, y1, x2, y2, x3, y3};
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(0, 0, 0);
        float tempScaleH = height / (float)HEIGHT;
        float tempScaleW = width / (float)WIDTH;
        float zoom = Math.min(tempScaleH, tempScaleW);
        camera.zoom = 1 / zoom;
        camera.update();
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
        batch.dispose();
        renderer.dispose();
        musicWave.dispose();
        assetManager.dispose();
        Gdx.input.setInputProcessor(null);
    }
}
