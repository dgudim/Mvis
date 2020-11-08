package com.deo.mvis;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.jtransforms.fft.FloatFFT_1D;
import com.deo.mvis.otherScreens.GameOfLife;
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.UIComposer;
import com.deo.mvis.visualisers.BaseVisualiser;
import com.deo.mvis.visualisers.FFTScreen;
import com.deo.mvis.visualisers.MuffinScreen;
import com.deo.mvis.visualisers.MushroomScreen;
import com.deo.mvis.visualisers.OsciloscopeScreen;
import com.deo.mvis.visualisers.RingScreen;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;
import static com.deo.mvis.utils.Utils.getBoolean;
import static com.deo.mvis.utils.Utils.getFloat;
import static com.deo.mvis.utils.Utils.getInteger;
import static com.deo.mvis.utils.Utils.putBoolean;
import static com.deo.mvis.utils.Utils.putInteger;

public class MenuScreen implements Screen {

    private Game game;
    private Stage stage;
    private Table visualisersTable;
    private Array<ScrollPane> settings;
    private ScrollPane scrollPane;
    private UIComposer uiComposer;
    private AssetManager assetManager;

    private ShapeRenderer renderer;

    private OrthographicCamera camera;
    private ScreenViewport viewport;

    private SpriteBatch batch;

    private BitmapFont font, font2, font_small;

    private MusicWave musicWave;
    private float[] displaySamples;
    private float[] averageSamples;
    private Music music;
    private FloatFFT_1D fft;

    private float triangleAngle = 0;
    private float triangleAnimation = 700;
    boolean musicStarted = false;

    Set<Class<? extends BaseVisualiser>> visualiserClasses;

    public MenuScreen(final Game game) {

        this.game = game;

        assetManager = new AssetManager();

        assetManager.load("menuButtons.atlas", TextureAtlas.class);
        assetManager.load("ui.atlas", TextureAtlas.class);
        assetManager.load("font2(old).fnt", BitmapFont.class);
        assetManager.load("font2.fnt", BitmapFont.class);

        while (!assetManager.isFinished()) {
            assetManager.update();
        }

        settings = new Array<>();

        musicWave = new MusicWave(Gdx.files.internal("liquid.wav"));

        music = musicWave.getMusic();

        averageSamples = musicWave.smoothSamples(musicWave.getSamples().clone(), 2, 32);

        music.setOnCompletionListener(new Music.OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                music.setPosition(0);
                music.play();
            }
        });

        fft = new FloatFFT_1D(32);

        displaySamples = new float[32];
        Arrays.fill(displaySamples, 0);

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        camera = new OrthographicCamera(1600, 900);
        viewport = new ScreenViewport(camera);
        batch = new SpriteBatch();

        font = assetManager.get("font2.fnt");
        font.getData().scale(0.5f);

        font2 = assetManager.get("font2(old).fnt");
        font2.getData().scale(0.4f);

        font_small = new BitmapFont(Gdx.files.internal("font2(old).fnt"), Gdx.files.internal("font2(old).png"), false);
        font_small.getData().scale(0.01f);

        uiComposer = new UIComposer(assetManager);
        uiComposer.loadStyles("defaultLight", "checkBoxDefault", "sliderDefaultSmall");

        visualisersTable = new Table();

        stage = new Stage(viewport, batch);

        visualiserClasses = new HashSet<>();

        visualiserClasses.add(OsciloscopeScreen.class);
        visualiserClasses.add(MuffinScreen.class);
        visualiserClasses.add(RingScreen.class);
        visualiserClasses.add(MushroomScreen.class);
        visualiserClasses.add(FFTScreen.class);
        visualiserClasses.add(GameOfLife.class);

        boolean row = false;
        int i = 0;
        for (final Class<? extends BaseVisualiser> aClass : visualiserClasses) {
            try {

                TextButton visualiserStartButton = uiComposer.addTextButton("defaultLight", (String) aClass.getMethod("getName").invoke(aClass), 0.45f);
                visualiserStartButton.setColor(Color.LIGHT_GRAY);
                if (row) {
                    visualisersTable.add(visualiserStartButton).width(370).height(100).pad(7).row();
                } else {
                    visualisersTable.add(visualiserStartButton).width(370).height(100).pad(7);
                }

                row = !row;
                aClass.getMethod("init").invoke(aClass);

                loadSettingsForVisualiser(aClass);

                final int finalI = i;
                visualiserStartButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        for (int d = 0; d < settings.size; d++) {
                            settings.get(d).setVisible(d == finalI);
                        }
                    }
                });

                i++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        scrollPane = new ScrollPane(visualisersTable);
        scrollPane.setBounds(-WIDTH / 2f, -HEIGHT / 2f, WIDTH / 2f - 10, HEIGHT / 2f + 45);

        stage.addActor(scrollPane);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
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

        float size = MathUtils.clamp(averageSamples[pos] * 25 + 725 - triangleAnimation, 0, 900);
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
        renderer.rect(-width / 2, -height / 2, width, height);

        triangleAngle += 10 * delta;

        if (triangleAnimation > 15) {
            triangleAnimation /= 1.01f;
        }

        if (!musicStarted && triangleAnimation < 70) {
            music.play();
            musicStarted = true;
        }

        renderer.end();

        stage.draw();
        stage.act(delta);

        batch.begin();

        //shadow
        font.setColor(Color.BLACK);
        font.draw(batch, "Welcome to Mvis V1.3", -WIDTH / 2f + 5, HEIGHT / 2f - 125, WIDTH, 1, false);

        //actual text
        font.setColor(Color.valueOf("#7799FF"));
        font.draw(batch, "Welcome to Mvis V1.3", -WIDTH / 2f, HEIGHT / 2f - 130, WIDTH, 1, false);

        //shadow
        font2.setColor(Color.BLACK);
        font2.draw(batch, "Available Visualisers:", -WIDTH / 2f + 2, HEIGHT / 2f - 318, WIDTH / 2f, 1, false);
        font2.draw(batch, "Visualiser Settings:", 2, HEIGHT / 2f - 318, WIDTH / 2f, 1, false);

        //actual text
        font2.setColor(Color.valueOf("#77DD77"));
        font2.draw(batch, "Available Visualisers:", -WIDTH / 2f, HEIGHT / 2f - 320, WIDTH / 2f, 1, false);
        font2.draw(batch, "Visualiser Settings:", 0, HEIGHT / 2f - 320, WIDTH / 2f, 1, false);

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

            displaySamples[i - 2] += samples2[i] / 1.5f;

            renderer.setColor(new Color().fromHsv(displaySamples[i - 2] / 2048, 0.75f, 0.9f));
            renderer.rect(i * 37.8f - samples.length / 2f * 37.8f - 62, 329, 11, displaySamples[i - 2] / 512 + 0.5f);

            displaySamples[i - 2] /= 1.3f;
        }


        float overHead = viewport.getScreenHeight() - HEIGHT;
        float overHeadHorizontal = viewport.getScreenWidth() - WIDTH;

        renderer.setColor(Color.BLACK);
        renderer.rect(-WIDTH / 2f + 5 - overHeadHorizontal, HEIGHT / 2f - 275, WIDTH + overHeadHorizontal * 2, 20);
        renderer.rect(-5, -HEIGHT / 2f + 5 - overHead, 20, HEIGHT - 280 + overHead);

        renderer.setColor(Color.CORAL);
        renderer.rect(-WIDTH / 2f - overHeadHorizontal, HEIGHT / 2f - 280, WIDTH + overHeadHorizontal * 2, 20);
        renderer.rect(-10, -HEIGHT / 2f - overHead, 20, HEIGHT - 280 + overHead);

        renderer.end();

        Gdx.gl20.glDisable(GL20.GL_BLEND);
    }

    private float[] makeATriangle(float size) {
        float x1 = -MathUtils.cosDeg(triangleAngle) * size;
        float y1 = -MathUtils.sinDeg(triangleAngle) * size;

        float x2 = -MathUtils.cosDeg(120 + triangleAngle) * size;
        float y2 = -MathUtils.sinDeg(120 + triangleAngle) * size;

        float x3 = -MathUtils.cosDeg(240 + triangleAngle) * size;
        float y3 = -MathUtils.sinDeg(240 + triangleAngle) * size;
        return new float[]{x1, y1, x2, y2, x3, y3};
    }

    private void loadSettingsForVisualiser(final Class visualiser) {

        String[] settingNames = new String[0];
        String[] settingTypes = new String[0];
        String[] paletteNames = new String[0];
        String[] typeNames = new String[0];
        float[] settingMaxValues = new float[0];
        float[] settingMinValues = new float[0];
        float[] defaultSettings = new float[0];

        String name = "noName";

        try {
            settingNames = (String[]) visualiser.getMethod("getSettings").invoke(BaseVisualiser.class);
            settingTypes = (String[]) visualiser.getMethod("getSettingTypes").invoke(BaseVisualiser.class);
            paletteNames = (String[]) visualiser.getMethod("getPaletteNames").invoke(BaseVisualiser.class);
            typeNames = (String[]) visualiser.getMethod("getTypeNames").invoke(BaseVisualiser.class);
            settingMaxValues = (float[]) visualiser.getMethod("getSettingMaxValues").invoke(BaseVisualiser.class);
            settingMinValues = (float[]) visualiser.getMethod("getSettingMinValues").invoke(BaseVisualiser.class);
            defaultSettings = (float[]) visualiser.getMethod("getDefaultSettings").invoke(BaseVisualiser.class);
            name = visualiser.getSimpleName();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final float[] newSettings = defaultSettings.clone();

        Pixmap pixmap = new Pixmap(100, 30, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        TextureRegionDrawable BarBackgroundBlank = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();

        Pixmap pixmap2 = new Pixmap(100, 30, Pixmap.Format.RGBA8888);
        pixmap2.setColor(Color.valueOf("#000000AA"));
        pixmap2.fill();
        TextureRegionDrawable BarBackgroundGrey = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap2)));
        pixmap2.dispose();

        Pixmap pixmap3 = new Pixmap(100, 30, Pixmap.Format.RGBA8888);
        pixmap3.setColor(Color.valueOf("#00000000"));
        pixmap3.fill();
        TextureRegionDrawable BarBackgroundEmpty = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap3)));
        pixmap3.dispose();

        Skin texturesForStyle = new Skin();
        texturesForStyle.addRegions((TextureAtlas) assetManager.get("ui.atlas"));

        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(font_small, Color.WHITE, BarBackgroundBlank,
                new ScrollPane.ScrollPaneStyle(BarBackgroundGrey, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty),
                new List.ListStyle(font_small, Color.CORAL, Color.SKY, BarBackgroundGrey));

        final SelectBox<String> paletteSelector = new SelectBox<>(selectBoxStyle);
        final SelectBox<String> typeSelector = new SelectBox<>(selectBoxStyle);
        final SelectBox<String> musicSelector = new SelectBox<>(selectBoxStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font_small;
        final Label musicFolderLabel = new Label("Place music into:\n"+Gdx.files.external("/").file().getAbsolutePath()+"/Mvis", labelStyle);

        Array<String> availableMusic = new Array<>();
        availableMusic.add("Up and away", "liquid cinema");
        final Array<FileHandle> availableMusicFiles = new Array<>();
        availableMusicFiles.add(Gdx.files.internal("away.wav"), Gdx.files.internal("liquid.wav"));

        File musicFolder = Gdx.files.external("Mvis").file();
        File musicFolder2 = Gdx.files.external("!Deltacore").file();
        try {
            for (File m : musicFolder2.listFiles()) {
                if (m.getName().endsWith(".wav")) {
                    availableMusic.add(m.getName().replace(".wav", ""));
                    availableMusicFiles.add(Gdx.files.external("!Deltacore/" + m.getName()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            for (File m : musicFolder.listFiles()) {
                if (m.getName().endsWith(".wav")) {
                    availableMusic.add(m.getName().replace(".wav", ""));
                    availableMusicFiles.add(Gdx.files.external("Mvis/" + m.getName()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        musicSelector.setItems(availableMusic);
        paletteSelector.setItems(paletteNames);
        typeSelector.setItems(typeNames);

        musicSelector.setMaxListCount(7);
        paletteSelector.setMaxListCount(7);
        typeSelector.setMaxListCount(7);

        typeSelector.setSelectedIndex(getInteger("type" + name));
        newSettings[0] = getInteger("type" + name);
        paletteSelector.setSelectedIndex(getInteger("palette" + name));
        newSettings[1] = getInteger("palette" + name);

        final String finalName = name;
        typeSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                newSettings[0] = typeSelector.getSelectedIndex();
                putInteger("type" + finalName, (int) newSettings[0]);
            }
        });

        paletteSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                newSettings[1] = paletteSelector.getSelectedIndex();
                putInteger("palette" + finalName, (int) newSettings[1]);
            }
        });

        Table settingsTable = new Table();
        settingsTable.align(Align.bottom);

        TextButton visualiserStartButton = uiComposer.addTextButton("defaultLight", "Launch visualiser", 0.45f);
        visualiserStartButton.setColor(Color.LIGHT_GRAY);

        visualiserStartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    visualiser.getMethod("setSettings", newSettings.getClass()).invoke(visualiser, newSettings);
                    visualiser.getMethod("setMusic", FileHandle.class).invoke(visualiser, availableMusicFiles.get(musicSelector.getSelectedIndex()));
                    game.setScreen((Screen) visualiser.getConstructor(Game.class).newInstance(game));
                    MenuScreen.this.dispose();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        ScrollPane scrollPane = new ScrollPane(settingsTable);
        scrollPane.setBounds(10, -HEIGHT / 2f, WIDTH / 2f - 10, HEIGHT / 2f + 45);
        scrollPane.setVisible(false);

        for (int i = 2; i < settingTypes.length; i++) {
            switch (settingTypes[i]) {
                case ("int"):
                case ("float"):

                    float step = 0.001f;
                    if (settingTypes[i].equals("int")) {
                        step = 1;
                    }

                    Table setting = uiComposer.addSlider("sliderDefaultSmall", settingMinValues[i], settingMaxValues[i],
                            step, settingNames[i] + ": ", "", name + "_" + i, settingTypes[i], scrollPane);

                    final Slider slider = (Slider) setting.getCells().get(0).getActor();

                    if (!getBoolean(name + "_" + i + "_changed")) {
                        slider.setValue(defaultSettings[i]);
                    } else {
                        newSettings[i] = slider.getValue();
                    }

                    final int finalI = i;
                    slider.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            newSettings[finalI] = slider.getValue();
                            putBoolean(finalName + "_" + finalI + "_changed", true);
                        }
                    });

                    settingsTable.add(setting).padBottom(5).padLeft(7).align(Align.left).row();
                    break;
                case ("boolean"):

                    final CheckBox setting2 = uiComposer.addCheckBox("checkBoxDefault", settingNames[i], name + "_" + i);

                    if (!getBoolean(name + "_" + i + "_changed")) {
                        setting2.setChecked(defaultSettings[i] > 0);
                    } else {
                        int set = 0;
                        if (setting2.isChecked()) {
                            set = 1;
                        }
                        newSettings[i] = set;
                    }

                    final int finalI1 = i;
                    setting2.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            int set = 0;
                            if (setting2.isChecked()) {
                                set = 1;
                            }
                            newSettings[finalI1] = set;
                            putBoolean(finalName + "_" + finalI1 + "_changed", true);
                        }
                    });

                    settingsTable.add(setting2).padBottom(5).padLeft(7).align(Align.left).row();

                    break;
            }
        }

        settingsTable.add(musicFolderLabel).width(WIDTH / 2f - 22).padBottom(5).row();
        settingsTable.add(musicSelector).width(WIDTH / 2f - 22).padBottom(5).row();
        settingsTable.add(typeSelector).width(WIDTH / 2f - 22).padBottom(5).row();
        settingsTable.add(paletteSelector).width(WIDTH / 2f - 22).padBottom(5).row();
        settingsTable.add(visualiserStartButton).width(330).height(75);

        settings.add(scrollPane);
        stage.addActor(scrollPane);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(0, 0, 0);
        float tempScaleH = height / (float) HEIGHT;
        float tempScaleW = width / (float) WIDTH;
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
        font_small.dispose();
        assetManager.dispose();
        if (Gdx.input.getInputProcessor().equals(stage)) {
            Gdx.input.setInputProcessor(null);
        }
    }
}
