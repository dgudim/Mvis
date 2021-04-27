package com.deo.mvis.visualisers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.badlogic.gdx.math.MathUtils.clamp;
import static com.badlogic.gdx.math.MathUtils.random;
import static com.deo.mvis.utils.ColorPallets.interpolate;
import static com.deo.mvis.utils.ColorPallets.mixTwoColors;
import static com.deo.mvis.visualisers.SlimeScreen.HEIGHT;
import static com.deo.mvis.visualisers.SlimeScreen.WIDTH;
import static java.lang.Math.min;

public class SlimeScreen extends BaseVisualiser implements Screen {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;

    Agent[] agents;

    Pixmap gridPixmap;
    volatile float[][] gridPixmapColors;
    Texture gridTexture;
    final int numberOfAgents = 100000;
    static int currentPalette = 0;
    static int currentSimulationRule = 0;

    final boolean randomSpawn = false;

    public SlimeScreen(Game game) {
        super(game, DEFAULT);

        utils.setBloomSaturation(1);
        utils.setBloomIntensity(1.3f);
        utils.maxSaturation = 1.3f;

        agents = new Agent[numberOfAgents];
        for (int i = 0; i < agents.length; i++) {
            Agent newAgent = new Agent();
            if (randomSpawn) {
                newAgent.x = (float) (WIDTH * Math.random());
                newAgent.y = (float) (HEIGHT * Math.random());
            } else {
                newAgent.x = WIDTH / 2f;
                newAgent.y = HEIGHT / 2f;
            }
            newAgent.rotation = (float) (Math.random() * 360);
            agents[i] = newAgent;
        }
        gridPixmap = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888);
        gridPixmapColors = new float[WIDTH][HEIGHT];
        gridTexture = new Texture(gridPixmap);

        transparency = 0;
        music.play();

        changeSimulationRules();

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);

        int pos = (int) (music.getPosition() * sampleRate);
        for (Agent agent : agents) {
            float volume = samplesSmoothed[pos] * 100;
            if (volume > 500) {
                agent.rotation += (random() * 2 - 1)*180;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            render = !render;
            frame = 0;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            for (Agent agent : agents) {
                agent.x = WIDTH / 2f;
                agent.y = HEIGHT / 2f;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            double rand = Math.random();
            double rand1 = Math.random();
            double rand2 = Math.random();
            double rand3 = Math.random();
            double rand4 = Math.random();
            for (Agent agent : agents) {
                agent.speed = (float) (rand * 10 - 5);
                agent.turnSpeed = (float) (rand1 * 160 - 80);
                agent.sensorLength = (int) (rand2 * 40 - 20);
                agent.sensorLengthOffset = (int) (rand3 * 60 - 30);
                agent.sensorAngleOffset = (int) (rand4 * 160 - 80);
                System.out.println(agent.speed + ", " + agent.turnSpeed + ", " + agent.sensorLength + ", " + agent.sensorLengthOffset + ", " + agent.sensorAngleOffset);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            currentSimulationRule++;
            changeSimulationRules();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            currentSimulationRule--;
            changeSimulationRules();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            currentPalette++;
        }

        utils.bloomBegin(false, pos);
        batch.begin();
        gridTexture.draw(gridPixmap, 0, 0);
        batch.draw(gridTexture, 0, 0);
        batch.end();
        utils.bloomRender();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Agent agent : agents) {
                    agent.update(gridPixmapColors);
                }
            }
        }).start();
        fade();
        copyOver();
        if (render) {
            utils.makeAScreenShot(frame);
            frame++;
        }
    }

    public static void init() {
        paletteNames = new String[]{"Vivid", "Orange", "Fire", "Virus", "Black hole"};
        typeNames = new String[]{"Cells", "Cells2", "Cells3", "Big cells ", "Flow", "Mushrooms", "Flow2", "Mushrooms2", "Balls", "Neurons", "Neurons2", "Ripples", "Ripples2", "Wall", "Threads"};

        settings = new String[]{"Type", "Pallet", "Render"};
        settingTypes = new String[]{"int", "int", "boolean"};

        settingMaxValues = new float[]{typeNames.length - 1, paletteNames.length - 1, 1};
        settingMinValues = new float[]{0, 0, 0};

        defaultSettings = new float[]{0, 0, 0};
    }

    public static String getName() {
        return "Particle";
    }

    public static void setSettings(float[] newSettings) {
        currentSimulationRule = (int) newSettings[0];
        currentPalette = (int) newSettings[1];
        render = newSettings[2] > 0;
    }

    void fade() {
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int y = 1; y < HEIGHT - 1; y++) {

                float sum = 0;

                sum += gridPixmapColors[x][y];

                sum += gridPixmapColors[x - 1][y];
                sum += gridPixmapColors[x + 1][y];

                sum += gridPixmapColors[x - 1][y - 1];
                sum += gridPixmapColors[x + 1][y + 1];

                sum += gridPixmapColors[x][y + 1];
                sum += gridPixmapColors[x][y - 1];

                sum += gridPixmapColors[x - 1][y + 1];
                sum += gridPixmapColors[x + 1][y - 1];

                sum /= 9f;

                gridPixmapColors[x][y] = clamp(sum, 0, 1);
            }
        }

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                gridPixmapColors[x][y] = clamp(gridPixmapColors[x][y] - 0.035f, 0, 1);
            }
        }
    }

    void copyOver() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {

                switch (currentPalette) {
                    case (0):
                        float ratio1 = (gridPixmapColors[x][y] - 0.5f) * 2;
                        float ratio2 = (ratio1 - gridPixmapColors[x][y]) * 2;
                        Color c1 = new Color(mixTwoColors(Color.SLATE, Color.WHITE, ratio1));
                        gridPixmap.drawPixel(x, y, mixTwoColors(c1, Color.CORAL, ratio2));
                        break;
                    case (1):
                        gridPixmap.drawPixel(x, y, interpolate(gridPixmapColors[x][y], Color.BLACK, Color.ORANGE, Color.CYAN, Color.CORAL));
                        break;
                    case (2):
                        gridPixmap.drawPixel(x, y, interpolate(gridPixmapColors[x][y], Color.BLACK, Color.ORANGE, Color.RED, Color.GRAY));
                        break;
                    case (3):
                        gridPixmap.drawPixel(x, y, interpolate(gridPixmapColors[x][y], Color.BLACK, Color.LIME, Color.TEAL, Color.CLEAR));
                        break;
                    case (4):
                        gridPixmap.drawPixel(x, y,
                                mixTwoColors(
                                        new Color(
                                                interpolate(
                                                        gridPixmapColors[x][y],
                                                        Color.BLACK, Color.ROYAL,
                                                        Color.ROYAL, Color.CYAN,
                                                        Color.WHITE)),
                                        new Color(mixTwoColors(
                                                Color.LIGHT_GRAY,
                                                Color.BLACK, 1 - gridPixmapColors[x][y])), gridPixmapColors[x][y]));
                        break;
                    case (5):
                        gridPixmap.drawPixel(x, y, interpolate(gridPixmapColors[x][y], Color.BLACK, Color.SKY, Color.BLACK, Color.BLACK, Color.TEAL, Color.BLACK));
                        break;
                    default:
                        currentPalette = 0;
                        break;
                }
            }
        }
    }

    void changeSimulationRules() {
        switch (currentSimulationRule) {
            case (0):
                changeParamsForAllAgents(2f, 50, 8, 2, 50);
                break;
            case (1):
                changeParamsForAllAgents(2f, 50, 7, 2, 40);
                break;
            case (2):
                changeParamsForAllAgents(2f, 50, 9, 19, 50);
                break;
            case (3):
                changeParamsForAllAgents(3f, 92, 10, 102, 67);
                break;
            case (4):
                changeParamsForAllAgents(3f, 9, 10, 12, 67);
                break;
            case (5):
                changeParamsForAllAgents(1f, 9, 10, 12, 9);
                break;
            case (6):
                changeParamsForAllAgents(3f, 10, 30, 5, 45);
                break;
            case (7):
                changeParamsForAllAgents(3f, 10, 13, 15, -45);
                break;
            case (8):
                changeParamsForAllAgents(-3f, 14, 10, 15, -45);
                break;
            case (9):
                changeParamsForAllAgents(3f, 25, 17, 13, 15);
                break;
            case (10):
                changeParamsForAllAgents(3f, 50, 30, 13, 15);
                break;
            case (11):
                changeParamsForAllAgents(3f, 50, 12, 13, -15);
                break;
            case (12):
                changeParamsForAllAgents(-3f, -20, 12, -13, 15);
                break;
            case (13):
                changeParamsForAllAgents(3, 45, 3, 3, 3);
                break;
            case (14):
                changeParamsForAllAgents(3, 30, 3, 3, 45);
                break;
            default:
                currentSimulationRule = 0;
                changeSimulationRules();
                break;
        }
    }

    void changeParamsForAllAgents(float speed,
                                  float turnSpeed,
                                  int sensorLength,
                                  int sensorLengthOffset,
                                  int sensorAngleOffset) {
        for (Agent agent : agents) {
            agent.speed = speed;
            agent.turnSpeed = turnSpeed;
            agent.sensorLength = sensorLength;
            agent.sensorLengthOffset = sensorLengthOffset;
            agent.sensorAngleOffset = sensorAngleOffset;
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(WIDTH / 2f, HEIGHT / 2f, 0);
        float tempScaleH = height / (float) HEIGHT;
        float tempScaleW = width / (float) WIDTH;
        float zoom = min(tempScaleH, tempScaleW);
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
        super.dispose();
        gridTexture.dispose();
    }
}

class Agent {
    float x;
    float y;
    float rotation;

    float speed = 3f;
    float turnSpeed = 50;
    int sensorLength = 12;
    int sensorLengthOffset = 13;
    int sensorAngleOffset = -15;

    boolean variableSteering = false;
    int variableSteeringStrength = 1;
    int variableSteeringAngle = 360;

    void update(float[][] colors) {
        x -= MathUtils.cosDeg(rotation) * speed;
        y -= MathUtils.sinDeg(rotation) * speed;
        x = clamp(x, 0, WIDTH - 1);
        y = clamp(y, 0, HEIGHT - 1);
        if (x <= sensorLength || y <= sensorLength || y >= HEIGHT - 1 - sensorLength || x >= WIDTH - 1 - sensorLength) {
            rotation = 360 * random() - 180;
        }
        senseAndRotate(colors);
        colors[(int) x][(int) y] = 1;
    }

    void senseAndRotate(float[][] colors) {
        if (!variableSteering) {

            float valueForward = 0;
            float valueRight = 0;
            float valueLeft = 0;

            for (int i = 0; i < sensorLength; i++) {

                int xForward = (int) (x - MathUtils.cosDeg(rotation) * (sensorLengthOffset + i));
                int yForward = (int) (y - MathUtils.sinDeg(rotation) * (sensorLengthOffset + i));

                int xRight = (int) (x - MathUtils.cosDeg(rotation + sensorAngleOffset) * (sensorLengthOffset + i));
                int yRight = (int) (y - MathUtils.sinDeg(rotation + sensorAngleOffset) * (sensorLengthOffset + i));

                int xLeft = (int) (x - MathUtils.cosDeg(rotation - sensorAngleOffset) * (sensorLengthOffset + i));
                int yLeft = (int) (y - MathUtils.sinDeg(rotation - sensorAngleOffset) * (sensorLengthOffset + i));

                xForward = clamp(xForward, 0, WIDTH - 1);
                xRight = clamp(xRight, 0, WIDTH - 1);
                xLeft = clamp(xLeft, 0, WIDTH - 1);

                yForward = clamp(yForward, 0, HEIGHT - 1);
                yRight = clamp(yRight, 0, HEIGHT - 1);
                yLeft = clamp(yLeft, 0, HEIGHT - 1);

                valueForward += colors[xForward][yForward];
                valueRight += colors[xRight][yRight];
                valueLeft += colors[xLeft][yLeft];
            }

            float randomSteerStrength = random() + 0.01f;

            if (valueForward > valueLeft && valueForward > valueRight) {
                rotation += 0;
            } else if (valueForward < valueLeft && valueForward < valueRight) {
                rotation += (randomSteerStrength - 0.5) * 2 * turnSpeed;
            } else if (valueRight > valueLeft) {
                rotation += randomSteerStrength * turnSpeed;
            } else if (valueLeft > valueRight) {
                rotation -= randomSteerStrength * turnSpeed;
            }
        } else {

            int currentStrongestAngle = 0;
            float currentStrongestDirSum = 0;

            for (int i = -variableSteeringAngle; i < variableSteeringAngle; i++) {
                float sum = 0;
                for (int l = 0; l < sensorLength; l++) {
                    int senseX = (int) (x - MathUtils.cosDeg(rotation + i) * (sensorLengthOffset + l));
                    int senseY = (int) (y - MathUtils.sinDeg(rotation + i) * (sensorLengthOffset + l));
                    senseX = clamp(senseX, 0, WIDTH - 1);
                    senseY = clamp(senseY, 0, HEIGHT - 1);
                    sum += colors[senseX][senseY];
                }
                if (sum > currentStrongestDirSum) {
                    currentStrongestAngle = i;
                    currentStrongestDirSum = sum;
                } else if (sum == currentStrongestDirSum) {
                    if (random() > 0.49f) {
                        currentStrongestAngle = i;
                    }
                }
            }

            rotation = (rotation + currentStrongestAngle * variableSteeringStrength) / (variableSteeringStrength + 1);

        }

    }

}

