package com.deo.mvis.otherScreens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.deo.mvis.Launcher.HEIGHT;
import static com.deo.mvis.Launcher.WIDTH;
import static com.deo.mvis.utils.Utils.getRandomInRange;

public class FourierScreen implements Screen {

    private float[] radiuses;
    private float[] speeds;

    private float[] currentAngles;
    private ShapeRenderer renderer;

    private final float step = 0.25f;
    private final float scale = 0.4f;
    private final float speed = 6;

    private Array<Vector2> points;

    private boolean isDrawing;

    private Color currentColor;

    FourierScreen() {
        //radiuses = new float[]{10, 5, 5, 6, 7, 12, 34, 5};
        //speeds = new float[]{10, 5, 4, 7, 8, 15, 5, 8};
        radiuses = new float[15];
        speeds = new float[15];

        for(int i = 0; i<getRandomInRange(1, 10); i++){
            radiuses[i] = getRandomInRange(2, 20);
            speeds[i] = getRandomInRange(-20, 20);
            if(speeds[i] == 0){
                speeds[i] = 3;
            }
        }

        currentAngles = new float[radiuses.length];

        points = new Array<>();

        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);

        isDrawing = true;

        currentColor = new Color().fromHsv(getRandomInRange(1, 360), 0.5f, 1).add(0, 0, 0, 1);

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL_COLOR_BUFFER_BIT);

        renderer.begin();

        if(isDrawing) {
            for (int passes = 0; passes < (1 / step) * speed; passes++) {

                float prevX = WIDTH / 2f;
                float prevY = HEIGHT / 2f;
                float currentX = 0;
                float currentY = 0;

                for (int i = 0; i < radiuses.length; i++) {
                    currentX = prevX - MathUtils.sinDeg(currentAngles[i]) * radiuses[i] * 10 * scale;
                    currentY = prevY - MathUtils.cosDeg(currentAngles[i]) * radiuses[i] * 10 * scale;
                    renderer.line(prevX, prevY, currentX, currentY);
                    prevX = currentX;
                    prevY = currentY;
                    currentAngles[i] += speeds[i] * delta * 10 * step;
                }

                points.add(new Vector2().set(currentX, currentY));
            }
        }

        renderer.setColor(currentColor);
        for (int i = 0; i < points.size-1; i++) {
            renderer.line(points.get(i).x, points.get(i).y, points.get(i+1).x, points.get(i+1).y);
        }
        renderer.setColor(Color.WHITE);

        renderer.end();

        if(Gdx.input.isKeyJustPressed(Input.Keys.S)){
            isDrawing = false;
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.C)){
            points.clear();
            for(int i = 0; i<getRandomInRange(1, 10); i++){
                radiuses[i] = getRandomInRange(2, 20);
                speeds[i] = getRandomInRange(-20, 20);
                if(speeds[i] == 0){
                    speeds[i] = 3;
                }
            }
            for(int i = 0; i < 15; i++){
                currentAngles[i] = 0;
            }
            isDrawing = true;
            currentColor = new Color().fromHsv(getRandomInRange(1, 360), 0.5f, 1).add(0, 0, 0, 1);
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
