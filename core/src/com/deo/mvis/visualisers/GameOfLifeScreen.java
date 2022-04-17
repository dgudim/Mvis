package com.deo.mvis.visualisers;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.mvis.utils.CompositeSettings;
import com.deo.mvis.utils.SettingsEntry;
import com.deo.mvis.utils.Type;

public class GameOfLifeScreen extends BaseVisualiser implements Screen {
    
    private final static float cellSize = 1;
    private final int fieldWidth = (int) (WIDTH / cellSize);
    private final static int fieldHeight = (int) (HEIGHT / cellSize);
    
    private static int oneDRuleHeight = 100;
    
    private final int NUM_THREADS = 4;
    int chunkSize = fieldWidth / NUM_THREADS;
    Thread[] threads;
    
    private final boolean[][] cells;
    private final boolean[][] next_cells;
    private final Vector3[][] colorMask;
    private final int[][] colorMaskProgress;
    
    private static GameOfLifeScreen.Mode mode;
    private static GameOfLifeScreen.Palette palette;
    
    private enum Palette {
        CYAN_PURPLE, CYAN_FADEOUT, PURPLE_FADEOUT, PINK_GREEN, RAINBOW_WATER, PASTEL_RAINBOW, LONG_FADEOUT_PASTEL_RAINBOW, WINTER, LONG_FADEOUT_CYAN
    }
    
    private enum Mode {
        BASIC
    }
    
    public GameOfLifeScreen(Game game) {
        super(game, DEFAULT);
        
        cells = new boolean[fieldWidth][fieldHeight];
        next_cells = new boolean[fieldWidth][fieldHeight];
        
        colorMask = new Vector3[fieldWidth][fieldHeight];
        colorMaskProgress = new int[fieldWidth][fieldHeight];
    
        threads = new Thread[NUM_THREADS];
        
        for (int x = 0; x < fieldWidth; x++) {
            for (int y = 0; y < fieldHeight; y++) {
                colorMask[x][y] = new Vector3(0, 0, 0);
            }
        }
        for (int x = 0; x < fieldWidth; x++) {
            cells[x][0] = MathUtils.randomBoolean();
        }
    }
    
    public void updateMainField(int threadIndex) {
        for (int x = threadIndex * chunkSize; x < (threadIndex + 1) * chunkSize; x++) {
            for (int y = oneDRuleHeight; y < fieldHeight; y++) {
                if (cells[x][y]) {
                    if (isCrowded(x, y) || isAlone(x, y)) {
                        kill(x, y);
                    } else {
                        next_cells[x][y] = cells[x][y];
                    }
                } else {
                    if (isRevivable(x, y)) {
                        revive(x, y);
                    } else {
                        next_cells[x][y] = cells[x][y];
                    }
                }
            }
        }
    }
    
    public void updateBottomField() {
        int limit = (int) ((render ? samplesSmoothed[frame] : samplesSmoothed[(int) (music.getPosition() * sampleRate)]) * 15 + 1);
        
        for (int x = 0; x < fieldWidth; x++) {
            for (int y = oneDRuleHeight - 1; y >= 0; y--) {
                cells[x][y + limit] = cells[x][y];
                cells[x][y] = false;
            }
        }
        
        for (int y = limit - 1; y >= 0; y--) {
            for (int x = 0; x < fieldWidth; x++) {
                cells[x][y] = alive1D(x, y);
            }
        }
        
        for (int x = 0; x < fieldWidth; x++) {
            System.arraycopy(cells[x], 0, next_cells[x], 0, oneDRuleHeight + limit);
        }
    }
    
    public void update() {
        
        updateBottomField();
        for (int i = 0; i < NUM_THREADS; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> updateMainField(finalI));
            threads[i].start();
        }
        waitUntilAllThreadsFinish();
        
        for (int x = 0; x < fieldWidth; x++) {
            for (int y = 0; y < fieldHeight; y++) {
                cells[x][y] = next_cells[x][y];
                next_cells[x][y] = false;
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
    
    private void revive(final int xPos, final int yPos) {
        next_cells[xPos][yPos] = true;
    }
    
    private void kill(final int xPos, final int yPos) {
        next_cells[xPos][yPos] = false;
    }
    
    @Override
    public void render(float delta) {
        
        update();
        
        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);
        
        renderer.setProjectionMatrix(camera.combined);
        
        utils.bloomBegin(true, render ? frame : (int) (music.getPosition() * sampleRate));
        
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        {
            for (int x = 0; x < fieldWidth; x++) {
                for (int y = 0; y < fieldHeight; y++) {
                    if (colorMask[x][y].x + colorMask[x][y].y + colorMask[x][y].z > 0) {
                        renderer.setColor(colorMask[x][y].x, colorMask[x][y].y, colorMask[x][y].z, 1);
                        renderer.rect(x * cellSize - WIDTH / 2f, y * cellSize - HEIGHT / 2f, cellSize, cellSize);
                        renderer.setColor(Color.WHITE);
                        colorMask[x][y] = shiftColor(colorMask[x][y], colorMaskProgress[x][y]);
                        colorMaskProgress[x][y]++;
                    }
                    if (cells[x][y]) {
                        renderer.rect(x * cellSize - WIDTH / 2f, y * cellSize - HEIGHT / 2f, cellSize, cellSize);
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
    }
    
    private boolean isRevivable(int xPos, int yPos) {
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
        return neighbours.x == 1 && neighbours.y == 1 && neighbours.z == 1;
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
        Color tempColor = new Color();
        switch (palette) {
            case LONG_FADEOUT_CYAN:
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
            case CYAN_FADEOUT:
                prevColor.x = (float) MathUtils.clamp(prevColor.x - 0.02, 0, 1);
                prevColor.y = (float) MathUtils.clamp(prevColor.y - 0.01, 0, 1);
                prevColor.z = (float) MathUtils.clamp(prevColor.z - 0.01, 0, 1);
                return prevColor;
            case PURPLE_FADEOUT:
                prevColor.x = (float) MathUtils.clamp(prevColor.x - 0.01, 0, 1);
                prevColor.y = (float) MathUtils.clamp(prevColor.y - 0.05, 0, 1);
                prevColor.z = (float) MathUtils.clamp(prevColor.z - 0.01, 0, 1);
                prevColor.x = (prevColor.y + prevColor.z) / 2.1f;
                return prevColor;
            case PINK_GREEN:
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
            case RAINBOW_WATER:
                tempColor = tempColor.fromHsv(progress * 2, 1, 1).add(0, 0, 0, 1);
                if (progress < 180) {
                    prevColor.x = tempColor.r;
                    prevColor.y = tempColor.g;
                    prevColor.z = tempColor.b;
                    
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
            case PASTEL_RAINBOW:
                tempColor = tempColor.fromHsv(progress * 2, 0.5f, 1).add(0, 0, 0, 1);
                if (progress < 180) {
                    prevColor.x = tempColor.r;
                    prevColor.y = tempColor.g;
                    prevColor.z = tempColor.b;
                    
                } else {
                    prevColor.x -= 0.005;
                    prevColor.z -= 0.005;
                    prevColor.y -= 0.005;
                }
                return prevColor;
            case LONG_FADEOUT_PASTEL_RAINBOW:
                tempColor = tempColor.fromHsv(progress * 2, 0.5f, 1).add(0, 0, 0, 1);
                if (progress < 90) {
                    prevColor.x = tempColor.r;
                    prevColor.y = tempColor.g;
                    prevColor.z = tempColor.b;
                    
                } else {
                    prevColor.x -= 0.0005;
                    prevColor.z -= 0.0005;
                    prevColor.y -= 0.0005;
                }
                return prevColor;
            case WINTER:
                tempColor = tempColor.fromHsv(progress * 2, progress / 180f, 1 - progress / 180f).add(0, 0, 0, 1);
                if (progress < 180) {
                    prevColor.x = tempColor.r;
                    prevColor.y = tempColor.g;
                    prevColor.z = tempColor.b;
                    
                } else {
                    prevColor.x -= 0.0005;
                    prevColor.z -= 0.0005;
                    prevColor.y -= 0.0005;
                }
                return prevColor;
            case CYAN_PURPLE:
                tempColor = tempColor.fromHsv(progress + 90, 0.5f, 0.5f).add(0, 0, 0, 1);
                if (progress < 180) {
                    prevColor.x = tempColor.r;
                    prevColor.y = tempColor.g;
                    prevColor.z = tempColor.b;
                } else {
                    prevColor.x -= 0.001;
                    prevColor.z -= 0.001;
                    prevColor.y -= 0.001;
                }
                return prevColor;
            default:
                return prevColor;
        }
    }
    
    public void waitUntilAllThreadsFinish(){
        for (int i = 0; i < NUM_THREADS; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static CompositeSettings init() {
        CompositeSettings compositeSettings = new CompositeSettings(enumToArray(Palette.class), enumToArray(Mode.class));
        
        compositeSettings.addSetting("Bottom rule height", 1, fieldHeight - 50, oneDRuleHeight, Type.INT);
        compositeSettings.addSetting("Render", 0, 1, 0, Type.BOOLEAN);
        
        return compositeSettings;
    }
    
    public static String getName() {
        return "Game of life";
    }
    
    public static void setSettings(Array<SettingsEntry> settings, int mode, int palette) {
        GameOfLifeScreen.mode = GameOfLifeScreen.Mode.values()[mode];
        GameOfLifeScreen.palette = GameOfLifeScreen.Palette.values()[palette];
        oneDRuleHeight = (int) getSettingByName(settings, "Bottom rule height");
        render = getSettingByName(settings, "Render") > 0;
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
        waitUntilAllThreadsFinish();
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].interrupt();
        }
        super.dispose();
    }
}
