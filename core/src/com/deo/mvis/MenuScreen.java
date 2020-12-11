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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.jtransforms.fft.FloatFFT_1D;
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.UIComposer;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

public class MenuScreen implements Screen {

    private Game game;
    private Stage visualisers;
    private UIComposer uiComposer;
    private AssetManager assetManager;

    private ShapeRenderer renderer;

    private OrthographicCamera camera;
    private ScreenViewport viewport;

    private SpriteBatch batch;

    private BitmapFont font;

    private MusicWave musicWave;
    private Music music;
    private FloatFFT_1D fft;

    MenuScreen(Game game) {

        musicWave = new MusicWave(Gdx.files.internal("liquid.wav"));

        music = musicWave.getMusic();

        music.setLooping(true);
        music.play();

        fft = new FloatFFT_1D(32);

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        camera = new OrthographicCamera(1600, 900);
        viewport = new ScreenViewport(camera);
        batch = new SpriteBatch();

        this.game = game;
        assetManager = new AssetManager();

        assetManager.load("menuButtons.atlas", TextureAtlas.class);
        assetManager.load("font2(old).fnt", BitmapFont.class);
        assetManager.load("font2.fnt", BitmapFont.class);

        while (!assetManager.isFinished()) {
            assetManager.update();
        }

        uiComposer = new UIComposer(assetManager);
        uiComposer.loadStyles("defaultLight");

        visualisers = new Stage(viewport, batch);
        Table button = uiComposer.addTextButton("defaultLight", "Frequency Visualiser", 0.35f);
        button.setPosition(20, 20);
        button.setWidth(210);
        visualisers.addActor(button);

        font = assetManager.get("font2.fnt");
        font.getData().markupEnabled = true;
        font.getData().scale(0.5f);

    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(visualisers);
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        renderer.begin(ShapeRenderer.ShapeType.Filled);

        renderer.end();

        visualisers.draw();
        visualisers.act(delta);

        batch.begin();
        font.setColor(Color.valueOf("#7799FF"));
        font.draw(batch, "Welcome to Mvis V1.3", -WIDTH / 2f, HEIGHT / 2f - 130, WIDTH, 1, false);
        batch.end();

        renderer.begin(ShapeRenderer.ShapeType.Filled);

        float[] samples = musicWave.getSamplesForFFT((int) (music.getPosition() * 44100), 32);
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
            renderer.setColor(new Color().fromHsv(samples2[i] / 2048, 0.75f, 0.9f));
            renderer.rect(i * 37.8f - samples.length / 2f * 37.8f - 62, 329, 11, samples2[i] / 512 + 0.5f);
        }

        renderer.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(0, 0, 0);
        float tempScaleH = height / 900f;
        float tempScaleW = width / 1600f;
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

    }
}
