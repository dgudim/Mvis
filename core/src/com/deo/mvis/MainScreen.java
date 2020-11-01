package com.deo.mvis;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.deo.mvis.postprocessing.PostProcessor;
import com.deo.mvis.postprocessing.effects.Bloom;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;

public class MainScreen implements Screen {

    public static final int WIDTH = 1600;
    public static final int HEIGHT = 900;

    private final int fieldWidth = 800;
    private final int fieldHeight = 450;
    private int oneDRuleHeight = 100;
    private boolean oneDRuleEnabled = true;

    private ShapeRenderer renderer;

    private boolean[][] cells;
    private Vector3[][] colorMask;
    private int[][] colorMaskProgress;

    private Vector2 dimensions;

    private int currentPallete;

    private int drawSquareSize = 10;

    private PostProcessor blurProcessor;
    private Bloom bloom;

    private int frame;

    private float[] samples;
    private Music music2;
    private float maxValue;

    private int musicProgress;
    private float step;
    private final int FPS = 30;

    private boolean realtime = true;

    MainScreen(){
        ShaderLoader.BasePath = "core/assets/shaders/";
        blurProcessor = new PostProcessor(false, false, Gdx.app.getType() == Application.ApplicationType.Desktop);
        bloom = new Bloom((int) (Gdx.graphics.getWidth() * 0.25f), (int) (Gdx.graphics.getHeight() * 0.25f));
        bloom.setBlurPasses(3);
        bloom.setBloomSaturation(1f);
        bloom.setBloomIntesity(0.6f);
        blurProcessor.addEffect(bloom);

        currentPallete = 6;
        step = 44100 / FPS;

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        cells = new boolean[fieldWidth][fieldHeight];
        colorMask = new Vector3[fieldWidth][fieldHeight];
        colorMaskProgress = new int[fieldWidth][fieldHeight];

        for (int x = 0; x < fieldWidth; x++) {
            for (int y = 0; y < fieldHeight; y++) {
                colorMask[x][y] = new Vector3(0, 0, 0);
            }
        }
        if(oneDRuleEnabled) {
            for (int x = 0; x < fieldWidth; x++) {
                cells[x][0] = MathUtils.randomBoolean();
            }
        }
        dimensions = new Vector2(2, 2);

        MusicWave musicWave = new MusicWave();

        samples = musicWave.getSamples();
        music2 = musicWave.getMusic();

        for(int i = 0; i<samples.length; i++){
            if(samples[i]>maxValue){
                maxValue = samples[i];
            }
        }

        if(realtime) {
            music2.play();
        }
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
        if(realtime){
            limit = (samples[(int)(music2.getPosition()*44100)]/maxValue)*5;
        }else{
            limit = (samples[musicProgress]/maxValue)*5;
        }
        if(!oneDRuleEnabled){
            limit = -1;
        }
        for(int i = 0; i < limit; i++) {
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

        if(!realtime) {
            musicProgress += step;

            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

            for (int i = 4; i < pixels.length; i += 4) {
                pixels[i - 1] = (byte) 255;
            }

            frame++;

            Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
            BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
            PixmapIO.writePNG(Gdx.files.external("GollyRender/pict" + frame + ".png"), pixmap);
            pixmap.dispose();
            if (frame == 9000) {
                Gdx.app.exit();
            }
        }

    }

    @Override
    public void show() {

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

        Gdx.gl.glClearColor(0, 0, 0, 0);

        update();

        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);

        if(realtime){
            bloom.setBloomSaturation(samples[(int)(music2.getPosition()*44100)]/maxValue*5);
        }else {
            bloom.setBloomSaturation(samples[musicProgress] / maxValue * 5);
        }
        blurProcessor.capture();

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        {
            for (int x = 0; x < fieldWidth; x++) {
                for (int y = 0; y < fieldHeight; y++) {
                    if (colorMask[x][y].x + colorMask[x][y].y + colorMask[x][y].z > 0) {
                        renderer.setColor(colorMask[x][y].x, colorMask[x][y].y, colorMask[x][y].z, 1);
                        renderer.rect(x * dimensions.x, y * dimensions.y, dimensions.x, dimensions.y);
                        renderer.setColor(Color.WHITE);
                        colorMask[x][y] = shiftColor(colorMask[x][y], colorMaskProgress[x][y]);
                        colorMaskProgress[x][y]++;
                    }
                    if (cells[x][y]) {
                        renderer.rect(x * dimensions.x, y * dimensions.y, dimensions.x, dimensions.y);
                        colorMask[x][y].x = 0;
                        colorMask[x][y].y = 1;
                        colorMask[x][y].z = 1;
                        colorMaskProgress[x][y] = 0;
                    }
                }
            }
        }
        renderer.end();

        blurProcessor.render();

        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            currentPallete++;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            currentPallete--;
            if (currentPallete == -1) {
                currentPallete = 8;
            }
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            for (int i = 0; i < drawSquareSize; i++) {
                for (int i2 = 0; i2 < drawSquareSize; i2++) {
                    try {
                        cells[(int) (Gdx.input.getX() - Math.floor(drawSquareSize / 2f) + i2)][(int) (fieldHeight - Gdx.input.getY() - Math.floor(drawSquareSize / 2f) + i)] = true;
                    } catch (Exception e) {

                    }
                }
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            oneDRuleHeight--;
            oneDRuleHeight = MathUtils.clamp(oneDRuleHeight, 0, fieldHeight-1);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            oneDRuleHeight++;
            oneDRuleHeight = MathUtils.clamp(oneDRuleHeight, 0, fieldHeight-1);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            drawSquareSize = MathUtils.clamp(drawSquareSize - 1, 1, 1000);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            drawSquareSize++;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.P)) {
            bloom.setBloomIntesity(bloom.getBloomIntensity() + 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.O)) {
            bloom.setBloomIntesity(bloom.getBloomIntensity() - 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.L)) {
            bloom.setBloomSaturation(bloom.getBloomSaturation() + 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.K)) {
            bloom.setBloomSaturation(bloom.getBloomSaturation() - 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.I)) {
            bloom.setBlurAmount(bloom.getBlurAmount() + 0.01f);
        }

        if (Gdx.input.isKeyPressed(Input.Keys.U)) {
            bloom.setBlurAmount(bloom.getBlurAmount() - 0.01f);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            bloom.setBlurPasses(bloom.getBlurPasses() + 1);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            bloom.setBlurPasses(bloom.getBlurPasses() - 1);
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
        switch (currentPallete) {
            default:
                currentPallete = 0;
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
        renderer.dispose();
        blurProcessor.dispose();
        music2.dispose();
    }
}
