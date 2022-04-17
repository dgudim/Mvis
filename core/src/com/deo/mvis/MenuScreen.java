package com.deo.mvis;

import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;
import static com.deo.mvis.utils.Type.INT;
import static com.deo.mvis.utils.Utils.getBoolean;
import static com.deo.mvis.utils.Utils.getInteger;
import static com.deo.mvis.utils.Utils.putBoolean;
import static com.deo.mvis.utils.Utils.putFloat;
import static com.deo.mvis.utils.Utils.putInteger;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.jtransforms.fft.FloatFFT_1D;
import com.deo.mvis.utils.CompositeSettings;
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.SettingsEntry;
import com.deo.mvis.utils.UIComposer;
import com.deo.mvis.visualisers.AttractorScreen;
import com.deo.mvis.visualisers.BaseVisualiser;
import com.deo.mvis.visualisers.FFTScreen;
import com.deo.mvis.visualisers.FourierScreen;
import com.deo.mvis.visualisers.GameOfLifeScreen;
import com.deo.mvis.visualisers.MuffinScreen;
import com.deo.mvis.visualisers.MushroomScreen;
import com.deo.mvis.visualisers.OsciloscopeScreen;
import com.deo.mvis.visualisers.RingScreen;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

//TODO make a scratch-like visualiser programming system
//TODO make a space background
//TODO linear frequency visualiser and a string visualiser

public class MenuScreen implements Screen {
    
    private final Game game;
    private final Stage stage;
    private final Array<ScrollPane> settingsPanes;
    private final UIComposer uiComposer;
    private final AssetManager assetManager;
    
    private final ShapeRenderer renderer;
    
    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    
    private final SpriteBatch batch;
    
    private final BitmapFont font;
    private final BitmapFont font2;
    private final BitmapFont font_small;
    
    private MusicWave musicWave;
    private float[] displaySamples, currentSamples, currentSamplesSmoothed;
    private final Music music;
    private FloatFFT_1D fft;
    
    private float triangleAngle = 0;
    private float triangleAnimation = 700;
    boolean musicStarted = false;
    boolean menuVisualisation;
    
    Set<Class<? extends BaseVisualiser>> visualiserClasses;
    
    public MenuScreen(final Game game) {
        
        menuVisualisation = !getBoolean("menuVisD");
        
        FileHandle destinationFolder = Gdx.files.external("Mvis/");
        
        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs();
        }
        
        FileHandle destinationMusic = Gdx.files.external("Mvis/liquid.wav");
        
        if (!destinationMusic.exists()) {
            Gdx.files.internal("away.wav").copyTo(destinationFolder);
            Gdx.files.internal("liquid.wav").copyTo(destinationFolder);
        }
        
        this.game = game;
        
        assetManager = new AssetManager();
        
        assetManager.load("menuButtons.atlas", TextureAtlas.class);
        assetManager.load("ui.atlas", TextureAtlas.class);
        assetManager.load("font2(old).fnt", BitmapFont.class);
        assetManager.load("font2.fnt", BitmapFont.class);
        
        while (!assetManager.isFinished()) {
            assetManager.update();
        }
        
        settingsPanes = new Array<>();
        
        if (menuVisualisation) {
            musicWave = new MusicWave(destinationMusic, false);
            music = musicWave.getMusic();
            
            fft = new FloatFFT_1D(32);
            
            displaySamples = new float[32];
            currentSamplesSmoothed = new float[36];
        } else {
            music = Gdx.audio.newMusic(destinationMusic);
        }
        
        music.setOnCompletionListener(new Music.OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                music.setPosition(0);
                music.play();
            }
        });
        
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        
        camera = new OrthographicCamera(WIDTH, HEIGHT);
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
        
        Table visualisersTable = new Table();
        
        stage = new Stage(viewport, batch);
        
        visualiserClasses = new HashSet<>();
        
        visualiserClasses.add(OsciloscopeScreen.class);
        visualiserClasses.add(MuffinScreen.class);
        visualiserClasses.add(RingScreen.class);
        visualiserClasses.add(MushroomScreen.class);
        visualiserClasses.add(FFTScreen.class);
        visualiserClasses.add(GameOfLifeScreen.class);
        visualiserClasses.add(FourierScreen.class);
        visualiserClasses.add(AttractorScreen.class);
        
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
                
                loadSettingsForVisualiser(aClass);
                
                final int finalI = i;
                visualiserStartButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        for (int d = 0; d < settingsPanes.size; d++) {
                            settingsPanes.get(d).setVisible(d == finalI);
                        }
                    }
                });
                
                i++;
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        final TextButton menuVisButton = uiComposer.addTextButton("defaultLight", "menu vis on/off", 0.4f);
        
        if (getBoolean("menuVisD")) {
            menuVisButton.setColor(Color.RED);
            menuVisButton.setText("menu vis off");
        } else {
            menuVisButton.setColor(Color.WHITE);
            menuVisButton.setText("menu vis on");
        }
        
        menuVisButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                putBoolean("menuVisD", !getBoolean("menuVisD"));
                if (getBoolean("menuVisD")) {
                    menuVisButton.setColor(Color.RED);
                    menuVisButton.setText("menu vis off");
                } else {
                    menuVisButton.setColor(Color.WHITE);
                    menuVisButton.setText("menu vis on");
                }
                game.setScreen(new MenuScreen(game));
                dispose();
            }
        });
        
        if (row) {
            visualisersTable.add(menuVisButton).width(370).height(100).pad(7).row();
        } else {
            visualisersTable.add(menuVisButton).width(370).height(100).pad(7);
        }
        
        ScrollPane scrollPane = new ScrollPane(visualisersTable);
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
        
        if (menuVisualisation) {
            currentSamples = musicWave.getSamplesForFFT(pos, 32, musicWave.getSamples());
            fft.realForward(currentSamples);
    
            Arrays.fill(currentSamplesSmoothed, 0);
            
            System.arraycopy(currentSamples, 0, currentSamplesSmoothed, 2, currentSamples.length - 4);
            
            for (int t = 0; t < 2; t++) {
                for (int i = 2; i < currentSamplesSmoothed.length - 2; i++) {
                    float neighbours = currentSamplesSmoothed[i - 2] + currentSamplesSmoothed[i + 2] + currentSamplesSmoothed[i - 1] + currentSamplesSmoothed[i + 1];
                    currentSamplesSmoothed[i] = (Math.abs(neighbours) + Math.abs(currentSamplesSmoothed[i])) / 5f * (1 + i / 100f);
                }
            }
            
            for (int i = 2; i < currentSamplesSmoothed.length - 2; i++) {
                displaySamples[i - 2] /= 1.3f;
                displaySamples[i - 2] += currentSamplesSmoothed[i] / 1.5f;
            }
            
            float[] triangle;
            for (int i = 2; i < currentSamplesSmoothed.length - 2; i++) {
                triangle = makeATriangle(displaySamples[i - 2] / 1000 + 725 - triangleAnimation);
                renderer.setColor(new Color().fromHsv(i * 5 + 100, 0.6f, 0.9f).add(0, 0, 0, 0.3f));
                renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
            }
        } else {
            float[] triangle;
            for (int i = 0; i < 4; i++) {
                triangle = makeATriangle(i * 20 + 725 - triangleAnimation);
                renderer.setColor(new Color().fromHsv(i * 50 + 100, 0.6f, 0.9f).add(0, 0, 0, 0.3f));
                renderer.triangle(triangle[0], triangle[1], triangle[2], triangle[3], triangle[4], triangle[5]);
            }
        }
        
        renderer.setColor(0, 0, 0, 0.55f);
        float width = viewport.getScreenWidth() * camera.zoom;
        float height = viewport.getScreenHeight() * camera.zoom;
        renderer.rect(-width / 2, -height / 2, width, height);
        
        triangleAngle += 10 * delta;
        
        if (triangleAnimation > 15) {
            triangleAnimation = triangleAnimation * (1 - delta);
        }
        
        if (!musicStarted && triangleAnimation < 70) {
            if (menuVisualisation) {
                music.play();
            }
            musicStarted = true;
        }
        
        renderer.end();
        
        stage.draw();
        stage.act(delta);
        
        batch.begin();
        
        //shadow
        font.setColor(Color.BLACK);
        font.draw(batch, "Welcome to Mvis V1.9", -WIDTH / 2f + 5, HEIGHT / 2f - 125, WIDTH, 1, false);
        
        //actual text
        font.setColor(Color.valueOf("#7799FF"));
        font.draw(batch, "Welcome to Mvis V1.9", -WIDTH / 2f, HEIGHT / 2f - 130, WIDTH, 1, false);
        
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
        
        if (menuVisualisation) {
            for (int i = 2; i < currentSamplesSmoothed.length - 2; i++) {
                renderer.setColor(new Color().fromHsv(displaySamples[i - 2] / 2048, 0.75f, 0.9f));
                renderer.rect(i * 37.8f - currentSamples.length / 2f * 37.8f - 62, 420, 11, displaySamples[i - 2] / 512 + 0.5f);
            }
        }
        
        float overHead = viewport.getScreenHeight() * camera.zoom - HEIGHT;
        float overHeadHorizontal = viewport.getScreenWidth() * camera.zoom - WIDTH;
        
        renderer.setColor(Color.BLACK);
        renderer.rect(-WIDTH / 2f + 5 - overHeadHorizontal, HEIGHT / 2f - 275, WIDTH + overHeadHorizontal * 2, 20);
        renderer.rect(-5, -HEIGHT / 2f + 5 - overHead, 20, HEIGHT - 280 + overHead);
        renderer.rect(-WIDTH / 2f + 5 - overHeadHorizontal, HEIGHT / 2f - 400, WIDTH + overHeadHorizontal * 2, 20);
        
        renderer.setColor(Color.CORAL);
        renderer.rect(-WIDTH / 2f - overHeadHorizontal, HEIGHT / 2f - 280, WIDTH + overHeadHorizontal * 2, 20);
        renderer.rect(-10, -HEIGHT / 2f - overHead, 20, HEIGHT - 280 + overHead);
        renderer.rect(-WIDTH / 2f - overHeadHorizontal, HEIGHT / 2f - 405, WIDTH + overHeadHorizontal * 2, 20);
        
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
    
    private void loadSettingsForVisualiser(final Class<? extends BaseVisualiser> visualiser) {
        
        Array<SettingsEntry> settingsEntries = new Array<>();
        Array<String> paletteNames = new Array<>();
        Array<String> modeNames = new Array<>();
        
        final int[] palette = {0};
        final int[] type = {0};
        
        String name = "noName";
        
        try {
            CompositeSettings compositeSettings = (CompositeSettings) visualiser.getMethod("init").invoke(visualiser);
            settingsEntries = compositeSettings.getMainSettings();
            paletteNames = compositeSettings.getPaletteNames();
            modeNames = compositeSettings.getModeNames();
            name = visualiser.getSimpleName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
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
        texturesForStyle.addRegions(assetManager.get("ui.atlas"));
        
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(font_small, Color.WHITE, BarBackgroundBlank,
                new ScrollPane.ScrollPaneStyle(BarBackgroundGrey, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty),
                new List.ListStyle(font_small, Color.CORAL, Color.SKY, BarBackgroundGrey));
        
        final SelectBox<String> paletteSelector = new SelectBox<>(selectBoxStyle);
        final SelectBox<String> typeSelector = new SelectBox<>(selectBoxStyle);
        final SelectBox<String> musicSelector = new SelectBox<>(selectBoxStyle);
        final SelectBox<String> presetSelector = new SelectBox<>(selectBoxStyle);
        
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font_small;
        final Label musicFolderLabel = new Label("Place music into:\n" + Gdx.files.external("/").file().getAbsolutePath().replace("Users", "Users/") + "/Mvis", labelStyle);
        
        Array<String> availableMusic = new Array<>();
        final Array<FileHandle> availableMusicFiles = new Array<>();
        
        final Array<String> availablePresets = new Array<>();
        final Array<FileHandle> availablePresetFiles = new Array<>();
        
        File musicFolder = Gdx.files.external("Mvis").file();
        File presetsFolder = Gdx.files.external("Mvis/" + name).file();
        
        try {
            for (File m : Objects.requireNonNull(musicFolder.listFiles())) {
                if (m.getName().endsWith(".wav")) {
                    availableMusic.add(m.getName().replace(".wav", ""));
                    availableMusicFiles.add(Gdx.files.external("Mvis/" + m.getName()));
                }
            }
        } catch (Exception e) {
            System.out.println("No Mvis folder");
        }
        
        try {
            for (File m : Objects.requireNonNull(presetsFolder.listFiles())) {
                availablePresets.add(m.getName());
                availablePresetFiles.add(Gdx.files.external("Mvis/" + name + "/" + m.getName()));
            }
        } catch (Exception e) {
            System.out.println("No presets");
        }
        
        final String finalName = name;
        if (!Gdx.files.external("Mvis/" + finalName + "/default").exists()) {
            StringBuilder exportedSettings = new StringBuilder();
            exportedSettings.append(settingsEntries.get(0).getDefault());
            for (int i = 1; i < settingsEntries.size; i++) {
                exportedSettings.append(",");
                exportedSettings.append(settingsEntries.get(i).getDefault());
            }
            FileHandle destinationFolder = Gdx.files.external("Mvis/" + finalName + "/");
            
            if (!destinationFolder.exists()) {
                destinationFolder.mkdirs();
            }
            
            FileHandle destinationFile = Gdx.files.external("Mvis/" + finalName + "/default");
            destinationFile.writeString(exportedSettings.toString(), false);
            availablePresets.add("default");
            availablePresetFiles.add(destinationFile);
        }
        
        musicSelector.setItems(availableMusic);
        paletteSelector.setItems(paletteNames);
        typeSelector.setItems(modeNames);
        presetSelector.setItems(availablePresets);
        
        musicSelector.setMaxListCount(5);
        typeSelector.setMaxListCount(6);
        paletteSelector.setMaxListCount(7);
        presetSelector.setMaxListCount(8);
        
        int selectedMusic = MathUtils.clamp(getInteger("music" + name), 0, availableMusic.size - 1);
        
        musicSelector.setSelectedIndex(selectedMusic);
        typeSelector.setSelectedIndex(getInteger("type" + name));
        type[0] = getInteger("type" + name);
        paletteSelector.setSelectedIndex(getInteger("palette" + name));
        palette[0] = getInteger("palette" + name);
        
        typeSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                type[0] = typeSelector.getSelectedIndex();
                putInteger("type" + finalName, type[0]);
            }
        });
        
        paletteSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                palette[0] = paletteSelector.getSelectedIndex();
                putInteger("palette" + finalName, palette[0]);
            }
        });
        
        musicSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                putInteger("music" + finalName, musicSelector.getSelectedIndex());
            }
        });
        
        final Table settingsTable = new Table();
        settingsTable.align(Align.bottom);
        
        TextButton visualiserStartButton = uiComposer.addTextButton("defaultLight", "Launch visualiser", 0.45f);
        visualiserStartButton.setColor(Color.LIGHT_GRAY);
        
        TextButton addPresetButton = uiComposer.addTextButton("defaultLight", "Save as a preset", 0.45f);
        addPresetButton.setColor(Color.LIGHT_GRAY);
        
        TextButton loadPresetButton = uiComposer.addTextButton("defaultLight", "Load preset", 0.45f);
        loadPresetButton.setColor(Color.LIGHT_GRAY);
        
        TextButton settingsResetButton = uiComposer.addTextButton("defaultLight", "Reset to defaults", 0.45f);
        settingsResetButton.setColor(Color.LIGHT_GRAY);
        
        final Array<SettingsEntry> finalSettingsEntries = settingsEntries;
        loadPresetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                FileHandle destinationPreset = availablePresetFiles.get(presetSelector.getSelectedIndex());
                String[] settings = destinationPreset.readString().split(",");
                float[] fSettings = new float[settings.length];
                for (int i = 0; i < settings.length; i++) {
                    fSettings[i] = Float.parseFloat(settings[i]);
                }
                for (int i = 0; i < finalSettingsEntries.size; i++) {
                    switch (finalSettingsEntries.get(i).getType()) {
                        case INT:
                        case FLOAT:
                            putFloat(finalName + "_" + i, fSettings[i]);
                            Table sliderTable = (Table) settingsTable.getCells().get(i).getActor();
                            Slider slider = (Slider) sliderTable.getCells().get(0).getActor();
                            slider.setValue(fSettings[i]);
                            break;
                        case BOOLEAN:
                            putBoolean(finalName + "_" + i, fSettings[i] > 0);
                            CheckBox checkBox = (CheckBox) settingsTable.getCells().get(i).getActor();
                            checkBox.setChecked(fSettings[i] > 0);
                            break;
                    }
                }
            }
        });
        
        
        addPresetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                float key = 0;
                StringBuilder exportedSettings = new StringBuilder();
                exportedSettings.append(finalSettingsEntries.get(0).getCurrent());
                for (int i = 1; i < finalSettingsEntries.size; i++) {
                    exportedSettings.append(",");
                    exportedSettings.append(finalSettingsEntries.get(i).getCurrent());
                    key += finalSettingsEntries.get(i).getCurrent() % i;
                }
                FileHandle destinationFolder = Gdx.files.external("Mvis/" + finalName + "/");
                
                if (!destinationFolder.exists()) {
                    destinationFolder.mkdirs();
                }
                
                FileHandle destinationFile = Gdx.files.external("Mvis/" + finalName + "/" + key);
                destinationFile.writeString(exportedSettings.toString(), false);
                
                if (!availablePresets.contains(key + "", false)) {
                    availablePresets.add(key + "");
                    availablePresetFiles.add(destinationFile);
                    presetSelector.setItems(availablePresets);
                }
            }
        });
        
        presetSelector.addListener(new ActorGestureListener() {
            @Override
            public boolean longPress(Actor actor, float x, float y) {
                CustomTextInputListener listener = new CustomTextInputListener() {
                    @Override
                    public void input(String text) {
                        FileHandle fileToRename = availablePresetFiles.get(presetSelector.getSelectedIndex());
                        FileHandle newFile = Gdx.files.external("Mvis/" + finalName + "/" + text);
                        StringBuilder name = new StringBuilder(text);
                        while (newFile.exists()) {
                            name.append("(duplicate)");
                            newFile = Gdx.files.external("Mvis/" + finalName + "/" + name);
                        }
                        newFile.writeString(fileToRename.readString(), false);
                        fileToRename.delete();
                        availablePresets.set(presetSelector.getSelectedIndex(), name.toString());
                        availablePresetFiles.set(presetSelector.getSelectedIndex(), newFile);
                        presetSelector.setItems(availablePresets);
                    }
                };
                Gdx.input.getTextInput(listener, "Rename?", presetSelector.getSelected(), "");
                return true;
            }
        });
        
        visualiserStartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    Gdx.input.setInputProcessor(null);
                    music.stop();
                    visualiser.getMethod("setSettings", Array.class, int.class, int.class).invoke(visualiser, finalSettingsEntries, type[0], palette[0]);
                    visualiser.getMethod("setMusic", FileHandle.class).invoke(visualiser, availableMusicFiles.get(musicSelector.getSelectedIndex()));
                    game.setScreen((Screen) visualiser.getConstructor(Game.class).newInstance(game));
                    dispose();
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String fullStackTrace = sw.toString();
                    Gdx.files.external("Mvis/ERRORS.txt").writeString(fullStackTrace + "\n\n\n", true);
                    Gdx.input.setInputProcessor(stage);
                    music.play();
                    e.printStackTrace();
                }
            }
        });
        
        settingsResetButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    
                    for (int i = 0; i < finalSettingsEntries.size; i++) {
                        switch (finalSettingsEntries.get(i).getType()) {
                            case INT:
                            case FLOAT:
                                putFloat(finalName + "_" + i, finalSettingsEntries.get(i).getDefault());
                                Table sliderTable = (Table) settingsTable.getCells().get(i - 2).getActor();
                                Slider slider = (Slider) sliderTable.getCells().get(0).getActor();
                                slider.setValue(finalSettingsEntries.get(i).getDefault());
                                break;
                            case BOOLEAN:
                                putBoolean(finalName + "_" + i, finalSettingsEntries.get(i).getDefault() > 0);
                                CheckBox checkBox = (CheckBox) settingsTable.getCells().get(i - 2).getActor();
                                checkBox.setChecked(finalSettingsEntries.get(i).getDefault() > 0);
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        ScrollPane scrollPane = new ScrollPane(settingsTable);
        scrollPane.setBounds(10, -HEIGHT / 2f, WIDTH / 2f - 10, HEIGHT / 2f + 45);
        scrollPane.setVisible(false);
        SettingsEntry currentSettingsEntry;
        for (int i = 0; i < finalSettingsEntries.size; i++) {
            currentSettingsEntry = finalSettingsEntries.get(i);
            switch (currentSettingsEntry.getType()) {
                case INT:
                case FLOAT:
                    
                    float step = 0.001f;
                    if (currentSettingsEntry.getType() == INT) {
                        step = 1;
                    }
                    
                    if (!getBoolean(name + "_" + i + "_changed")) {
                        putBoolean(name + "_" + i + "_changed", true);
                        putFloat(name + "_" + i, currentSettingsEntry.getDefault());
                    }
                    
                    Table setting = uiComposer.addSlider("sliderDefaultSmall", currentSettingsEntry.getMin(), currentSettingsEntry.getMax(),
                            step, currentSettingsEntry.getName() + ": ", "", name + "_" + i, currentSettingsEntry.getType(), scrollPane);
                    
                    final Slider slider = (Slider) setting.getCells().get(0).getActor();
                    
                    currentSettingsEntry.setValue(slider.getValue());
                    
                    final int finalI = i;
                    slider.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            finalSettingsEntries.get(finalI).setValue(slider.getValue());
                            putBoolean(finalName + "_" + finalI + "_changed", true);
                        }
                    });
                    
                    settingsTable.add(setting).padBottom(5).padLeft(7).align(Align.left).row();
                    break;
                case BOOLEAN:
                    
                    if (!getBoolean(name + "_" + i + "_changed")) {
                        putBoolean(name + "_" + i + "_changed", true);
                        putBoolean(name + "_" + i, currentSettingsEntry.getDefault() > 0);
                    }
                    
                    final CheckBox setting2 = uiComposer.addCheckBox("checkBoxDefault", currentSettingsEntry.getName(), name + "_" + i);
                    
                    int set = 0;
                    if (setting2.isChecked()) {
                        set = 1;
                    }
                    currentSettingsEntry.setValue(set);
                    
                    final int finalI1 = i;
                    setting2.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            int set = 0;
                            if (setting2.isChecked()) {
                                set = 1;
                            }
                            finalSettingsEntries.get(finalI1).setValue(set);
                            putBoolean(finalName + "_" + finalI1 + "_changed", true);
                        }
                    });
                    
                    settingsTable.add(setting2).padBottom(5).padLeft(7).align(Align.left).row();
                    
                    break;
            }
        }
        
        settingsTable.add(musicFolderLabel).width(WIDTH / 2f - 22).padBottom(5).padLeft(7).align(Align.left).row();
        settingsTable.add(musicSelector).width(WIDTH / 2f - 22).padBottom(5).padLeft(7).align(Align.left).row();
        settingsTable.add(typeSelector).width(WIDTH / 2f - 22).padBottom(5).padLeft(7).align(Align.left).row();
        settingsTable.add(paletteSelector).width(WIDTH / 2f - 22).padBottom(5).padLeft(7).align(Align.left).row();
        
        Table middleTable = new Table();
        middleTable.add(visualiserStartButton).width(330).height(75).padRight(5).padBottom(5).align(Align.left);
        middleTable.add(settingsResetButton).width(330).height(75).padLeft(5).padBottom(5).align(Align.left);
        
        Table middleTable2 = new Table();
        middleTable2.add(addPresetButton).width(330).height(75).padRight(5).padBottom(5).align(Align.left);
        middleTable2.add(loadPresetButton).width(330).height(75).padLeft(5).padBottom(5).align(Align.left);
        
        settingsTable.add(middleTable).row();
        settingsTable.add(middleTable2).row();
        settingsTable.add(presetSelector).width(WIDTH / 2f - 22).padBottom(5).padLeft(7).align(Align.left);
        
        settingsPanes.add(scrollPane);
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
        font_small.dispose();
        assetManager.dispose();
        if (Gdx.input.getInputProcessor().equals(stage)) {
            Gdx.input.setInputProcessor(null);
        }
        if (menuVisualisation) {
            musicWave.dispose();
        } else {
            music.dispose();
        }
    }
}

class CustomTextInputListener implements Input.TextInputListener {
    @Override
    public void input(String text) {
    }
    
    @Override
    public void canceled() {
    }
}
