package com.deo.mvis.otherScreens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.mvis.visualisers.BaseVisualiser;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

public class GameOfLife extends BaseVisualiser implements Screen {

    private final int fieldWidth = 800;
    private static final int fieldHeight = 450;
    private static int oneDRuleHeight = 100;
    private static boolean oneDRuleEnabled = true;

    private boolean[][] cells;
    private Vector3[][] colorMask;
    private int[][] colorMaskProgress;

    private Vector2 dimensions;

    private static int palette = 0;
    private static int type;

    private int drawSquareSize = 10;

    public GameOfLife(Game game) {
        super(game);

        cells = new boolean[fieldWidth][fieldHeight];
        colorMask = new Vector3[fieldWidth][fieldHeight];
        colorMaskProgress = new int[fieldWidth][fieldHeight];

        for (int x = 0; x < fieldWidth; x++) {
            for (int y = 0; y < fieldHeight; y++) {
                colorMask[x][y] = new Vector3(0, 0, 0);
            }
        }
        if (oneDRuleEnabled) {
            for (int x = 0; x < fieldWidth; x++) {
                cells[x][0] = MathUtils.randomBoolean();
            }
        }
        dimensions = new Vector2(2, 2);

    }

    public void update() {
        for (int x = 0; x < fieldWidth; x++) {
            for (int y = oneDRuleHeight; y < fieldHeight; y++) {
                if (cells[x][y]) {
                    if (isCrowded(x, y) || isAlone(x, y)) {
                        die(x, y);
                    }
                } else {
                    if (isAlive(x, y)) {
                        alive(x, y);
                    }
                }
            }
        }

        float limit;
        if (!render) {
            limit = (samplesSmoothed[(int) (music.getPosition() * 44100)]) * 5;
        } else {
            limit = (samplesSmoothed[frame]) * 5;
        }
        if (!oneDRuleEnabled) {
            limit = -1;
        }
        for (int i = 0; i < limit; i++) {
            for (int x = 0; x < fieldWidth; x++) {
                for (int y = oneDRuleHeight; y >= 0; y--) {
                    cells[x][y + 1] = cells[x][y];
                    cells[x][y] = false;
                }
            }
            for (int x = 0; x < fieldWidth; x++) {
                cells[x][0] = alive1D(x, 0);
            }
        }

        if (render) {
            frame += step;
            recorderFrame++;
            utils.makeAScreenShot(recorderFrame);
            utils.displayData(recorderFrame, frame, camera.combined);
        }

        batch.begin();
        drawExitButton();
        batch.end();

    }

    @Override
    public void show() {
        super.show();
    }

    private void alive(final int xPos, final int yPos) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                cells[xPos][yPos] = true;
            }
        });
    }

    private void die(final int xPos, final int yPos) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                cells[xPos][yPos] = false;
            }
        });
    }

    @Override
    public void render(float delta) {

        update();

        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);

        int pos;
        if (render) {
            pos = frame;
        } else {
            pos = (int) (music.getPosition() * 44100);
        }

        renderer.setProjectionMatrix(camera.combined);

        utils.bloomBegin(true, pos);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        {
            for (int x = 0; x < fieldWidth; x++) {
                for (int y = 0; y < fieldHeight; y++) {
                    if (colorMask[x][y].x + colorMask[x][y].y + colorMask[x][y].z > 0) {
                        renderer.setColor(colorMask[x][y].x, colorMask[x][y].y, colorMask[x][y].z, 1);
                        renderer.rect(x * dimensions.x - WIDTH / 2f, y * dimensions.y - HEIGHT / 2f, dimensions.x, dimensions.y);
                        renderer.setColor(Color.WHITE);
                        colorMask[x][y] = shiftColor(colorMask[x][y], colorMaskProgress[x][y]);
                        colorMaskProgress[x][y]++;
                    }
                    if (cells[x][y]) {
                        renderer.rect(x * dimensions.x - WIDTH / 2f, y * dimensions.y - HEIGHT / 2f, dimensions.x, dimensions.y);
                        colorMask[x][y].x = 0;
                        colorMask[x][y].y = 1;
                        colorMask[x][y].z = 1;
                        colorMaskProgress[x][y] = 0;
                    }
                }
            }
        }
        renderer.end();

        utils.bloomRender();

        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            palette++;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            palette--;
            if (palette == -1) {
                palette = 8;
            }
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            for (int i = 0; i < drawSquareSize; i++) {
                for (int i2 = 0; i2 < drawSquareSize; i2++) {
                    try {
                        Vector2 tCords = new Vector2(Gdx.input.getX(), Gdx.input.getY());
                        Vector2 newCords = viewport.unproject(tCords);
                        cells[(int) (newCords.x + fieldWidth / 2f - Math.floor(drawSquareSize / 2f) + i2)][(int) (newCords.y + fieldHeight / 2f - Math.floor(drawSquareSize / 2f) + i)] = true;
                    } catch (Exception e) {

                    }
                }
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            oneDRuleHeight--;
            oneDRuleHeight = MathUtils.clamp(oneDRuleHeight, 0, fieldHeight - 1);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            oneDRuleHeight++;
            oneDRuleHeight = MathUtils.clamp(oneDRuleHeight, 0, fieldHeight - 1);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            drawSquareSize = MathUtils.clamp(drawSquareSize - 1, 1, 1000);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            drawSquareSize++;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.P)) {
            utils.setBloomIntensity(utils.bloom.getBloomIntensity() + 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.O)) {
            utils.setBloomIntensity(utils.bloom.getBloomIntensity() - 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.L)) {
            utils.setBloomIntensity(utils.bloom.getBloomSaturation() + 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.K)) {
            utils.setBloomIntensity(utils.bloom.getBloomSaturation() - 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.I)) {
            utils.setBloomIntensity(utils.bloom.getBlurAmount() + 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.U)) {
            utils.setBloomIntensity(utils.bloom.getBlurAmount() - 0.01f);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            utils.setBloomIntensity(utils.bloom.getBlurPasses() + 1);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            utils.setBloomIntensity(utils.bloom.getBlurPasses() - 1);
        }

    }

    private boolean isAlive(int xPos, int yPos) {
        return getNeighbours(xPos, yPos) == 3;
    }

    private boolean isAlone(int xPos, int yPos) {
        return getNeighbours(xPos, yPos) < 2;
    }

    private boolean isCrowded(int xPos, int yPos) {
        return getNeighbours(xPos, yPos) > 3;
    }

    private int getNeighbours(int xPos, int yPos) {
        int num = 0; // number of neighbours

        num += getNeighbours(xPos, yPos, 1, 0);
        num += getNeighbours(xPos, yPos, -1, 0);

        num += getNeighbours(xPos, yPos, 0, 1);
        num += getNeighbours(xPos, yPos, 0, -1);

        num += getNeighbours(xPos, yPos, 1, 1);
        num += getNeighbours(xPos, yPos, 1, -1);

        num += getNeighbours(xPos, yPos, -1, 1);
        num += getNeighbours(xPos, yPos, -1, -1);

        return num;
    }

    private Vector3 getNeighbours1D(int xPos, int yPos) {
        return new Vector3().set(new float[]{getNeighbours(xPos, yPos, 0, 1), getNeighbours(xPos, yPos, 1, 1), getNeighbours(xPos, yPos, -1, 1)});
    }

    private boolean alive1D(int xPos, int yPos) {
        Vector3 neighbours = getNeighbours1D(xPos, yPos);
        if (neighbours.x == 0 && neighbours.y == 0 && neighbours.z == 0) {
            return true;
        }
        if (neighbours.x == 0 && neighbours.y == 0 && neighbours.z == 1) {
            return false;
        }
        if (neighbours.x == 0 && neighbours.y == 1 && neighbours.z == 0) {
            return false;
        }
        if (neighbours.x == 0 && neighbours.y == 1 && neighbours.z == 1) {
            return true;
        }
        if (neighbours.x == 1 && neighbours.y == 0 && neighbours.z == 0) {
            return false;
        }
        if (neighbours.x == 1 && neighbours.y == 0 && neighbours.z == 1) {
            return false;
        }
        if (neighbours.x == 1 && neighbours.y == 1 && neighbours.z == 0) {
            return false;
        }
        if (neighbours.x == 1 && neighbours.y == 1 && neighbours.z == 1) {
            return true;
        }
        return false;
    }

    private int getNeighbours(int xPos, int yPos, int xOffset, int yOffset) {
        try {
            if (cells[xPos + xOffset][yPos + yOffset]) return 1;
            return 0;
        } catch (IndexOutOfBoundsException ignore) {
        }
        return 0;
    }

    private Vector3 shiftColor(Vector3 prevColor, int progress) {
        switch (palette) {
            default:
                palette = 0;
                return prevColor;
            case (8):
                if (prevColor.x > 0.8f && prevColor.y > 0.8f && prevColor.z > 0.8f) {
                    prevColor.x -= 0.007;
                    prevColor.z -= 0.07;
                    prevColor.y -= 0.07;
                } else if (prevColor.x > 0.5f && prevColor.y > 0.5f && prevColor.z > 0.5f) {
                    prevColor.x -= 0.005;
                    prevColor.z -= 0.05;
                    prevColor.y -= 0.05;
                } else {
                    prevColor.x = (float) MathUtils.clamp(prevColor.x - 0.05, 0, 1);
                    prevColor.z -= 0.001;
                    prevColor.y -= 0.001;
                }
                return prevColor;
            case (1):
                prevColor.x = (float) MathUtils.clamp(prevColor.x - 0.02, 0, 1);
                prevColor.y = (float) MathUtils.clamp(prevColor.y - 0.01, 0, 1);
                prevColor.z = (float) MathUtils.clamp(prevColor.z - 0.01, 0, 1);
                return prevColor;
            case (2):
                prevColor.x = (float) MathUtils.clamp(prevColor.x - 0.01, 0, 1);
                prevColor.y = (float) MathUtils.clamp(prevColor.y - 0.05, 0, 1);
                prevColor.z = (float) MathUtils.clamp(prevColor.z - 0.01, 0, 1);
                prevColor.x = (prevColor.y + prevColor.z) / 2.1f;
                return prevColor;
            case (3):
                if (progress < 2000) {
                    prevColor.x = MathUtils.cosDeg(progress);
                    prevColor.y = MathUtils.sinDeg(progress);
                    prevColor.z = 0.7f;
                } else {
                    prevColor.x -= 0.005;
                    prevColor.z -= 0.005;
                    prevColor.y -= 0.005;
                }
                return prevColor;
            case (4):
                Color newColor = new Color();
                newColor = newColor.fromHsv(progress * 2, 1, 1).add(0, 0, 0, 1);
                if (progress < 180) {
                    prevColor.x = newColor.r;
                    prevColor.y = newColor.g;
                    prevColor.z = newColor.b;

                    if (prevColor.x > prevColor.y && prevColor.x > prevColor.z) {
                        prevColor.x /= 2f;
                    } else if (prevColor.y > prevColor.x && prevColor.y > prevColor.z) {
                        prevColor.y /= 2f;
                    } else {
                        prevColor.z /= 2f;
                    }

                } else {
                    prevColor.x -= 0.005;
                    prevColor.z -= 0.005;
                    prevColor.y -= 0.005;
                }
                return prevColor;
            case (5):
                Color newColor2 = new Color();
                newColor2 = newColor2.fromHsv(progress * 2, 0.5f, 1).add(0, 0, 0, 1);
                if (progress < 180) {
                    prevColor.x = newColor2.r;
                    prevColor.y = newColor2.g;
                    prevColor.z = newColor2.b;

                } else {
                    prevColor.x -= 0.005;
                    prevColor.z -= 0.005;
                    prevColor.y -= 0.005;
                }
                return prevColor;
            case (6):
                Color newColor3 = new Color();
                newColor3 = newColor3.fromHsv(progress * 2, 0.5f, 1).add(0, 0, 0, 1);
                if (progress < 90) {
                    prevColor.x = newColor3.r;
                    prevColor.y = newColor3.g;
                    prevColor.z = newColor3.b;

                } else {
                    prevColor.x -= 0.0005;
                    prevColor.z -= 0.0005;
                    prevColor.y -= 0.0005;
                }
                return prevColor;
            case (7):
                Color newColor4 = new Color();
                newColor4 = newColor4.fromHsv(progress * 2, progress / 180f, 1 - progress / 180f).add(0, 0, 0, 1);
                if (progress < 180) {
                    prevColor.x = newColor4.r;
                    prevColor.y = newColor4.g;
                    prevColor.z = newColor4.b;

                } else {
                    prevColor.x -= 0.0005;
                    prevColor.z -= 0.0005;
                    prevColor.y -= 0.0005;
                }
                return prevColor;
            case (0):
                Color newColor6 = new Color();
                newColor6 = newColor6.fromHsv(progress + 90, 0.5f, 0.5f).add(0, 0, 0, 1);
                if (progress < 180) {
                    prevColor.x = newColor6.r;
                    prevColor.y = newColor6.g;
                    prevColor.z = newColor6.b;
                } else {
                    prevColor.x -= 0.001;
                    prevColor.z -= 0.001;
                    prevColor.y -= 0.001;
                }
                return prevColor;
        }
    }

    public static void init() {
        paletteNames = new String[]{"Cyan-purple", "Cyan fadeout", "Purple fadeout", "Pink-green", "Rainbow water", "Pastel rainbow", "Long fadeout(pastel rainbow)", "winter", "Long cyan fadeout"};
        typeNames = new String[]{"Normal"};

        settings = new String[]{"Type", "Pallet", "Bottom enabled", "Bottom rule height"};
        settingTypes = new String[]{"int", "int", "boolean", "int"};

        settingMaxValues = new float[]{typeNames.length - 1, paletteNames.length - 1, 1, fieldHeight - 1};
        settingMinValues = new float[]{0, 0, 0, 0};

        defaultSettings = new float[]{0, 0, 1, oneDRuleHeight};
    }

    public static String getName() {
        return "Game of life";
    }

    public static void setSettings(float[] newSettings) {
        type = (int) newSettings[0];
        palette = (int) newSettings[1];
        oneDRuleEnabled = newSettings[2] > 0;
        oneDRuleHeight = (int) newSettings[3];
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height, 0, true);
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
    }
}
