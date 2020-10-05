package com.deo.golly;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class MushroomScreen implements Screen {

    private ShapeRenderer renderer;
    private Array<Array<Vector2>> branches;
    private Array<Vector3> colors;

    MushroomScreen() {
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        branches = new Array<>();
        colors = new Array<>();

        buildMushroom(-90, 15, 40, 4, 800, 250);
    }

    void buildMushroom(float initialAngle, float offsetAngle, float branchLength, int iterations, float x, float y) {

        float angleRight = initialAngle + offsetAngle;
        float angleLeft = initialAngle - offsetAngle;

        float sinRight = (float) (MathUtils.sinDeg(angleRight) * branchLength);
        float cosRight = (float) (MathUtils.cosDeg(angleRight) * branchLength);

        float sinLeft = (float) (MathUtils.sinDeg(angleLeft) * branchLength);
        float cosLeft = (float) (MathUtils.cosDeg(angleLeft) * branchLength);

        float xRight = x - cosRight;
        float yRight = y - sinRight;

        float xLeft = x - cosLeft;
        float yLeft = y - sinLeft;

        Array<Vector2> branchRight = new Array<>();

        branchRight.add(new Vector2(x, y));
        branchRight.add(new Vector2(xRight, yRight));

        Array<Vector2> branchLeft = new Array<>();

        branchLeft.add(new Vector2(x, y));
        branchLeft.add(new Vector2(xLeft, yLeft));

        branches.add(branchRight, branchLeft);
        colors.add(new Vector3(1, 1, 0), new Vector3(1, 1, 0));

        if (iterations > 0 && branchLength > 0) {
            buildMushroom(angleRight, offsetAngle, branchLength, iterations - 1, xRight, yRight);
            buildMushroom(angleLeft, offsetAngle, branchLength, iterations - 1, xLeft, yLeft);
        }

    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {
        renderer.begin();
        for (int i = 0; i < branches.size; i++) {
            renderer.setColor(i / (float) branches.size, 1, 0, 1);
            renderer.line(branches.get(i).get(0), branches.get(i).get(1));
        }
        renderer.end();
    }

    private void fadeOut(){

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
