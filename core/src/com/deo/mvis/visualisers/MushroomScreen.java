package com.deo.mvis.visualisers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.mvis.utils.MusicWave;
import com.deo.mvis.utils.Utils;

public class MushroomScreen extends BaseVisualiser implements Screen {

    private Array<Array<Vector2>> branches;
    private Array<Vector3> colors;
    private float fadeout;

    private final int SINGLE = 0;
    private final int DOUBLE = 1;
    private final int TRIPLE = 2;
    private final int DOUBLE_CHANNEL = 3;
    private boolean EXPONENTIAL = false;

    private final int type = SINGLE;

    public MushroomScreen() {

        branches = new Array<>();
        colors = new Array<>();
        fadeout = 0.05f;

        utils.maxSaturation = 3;
        utils.setBloomIntensity(3);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fadeOut();
        float angle;
        int iterations;
        int pos;
        if (!render) {
            angle = rSamplesNormalised[(int) (music.getPosition() * 44100)] * 45;
            iterations = (int) (lSamplesNormalised[(int) (music.getPosition() * 44100)] * 10) + 5;
            pos = (int) (music.getPosition() * 44100);
        } else {
            angle = rSamplesNormalised[frame] * 45;
            iterations = (int) (lSamplesNormalised[frame] * 10) + 5;
            pos = frame;
            frame += step;
            recorderFrame++;
        }

        switch (type) {
            case (SINGLE):
                buildMushroom(-90, angle, 40, iterations, 800, 290);
                break;
            case (DOUBLE):
                buildMushroom(-90, angle, 40, iterations, 800, 450);
                buildMushroom(90, angle, 40, iterations, 800, 450);
                break;
            case (DOUBLE_CHANNEL):
                angle = rSamplesNormalised[(int) (music.getPosition() * 44100)] * 45;
                iterations = (int) (rSamplesNormalised[(int) (music.getPosition() * 44100)] * 10) + 5;
                buildMushroom(-90, angle, 40, iterations, 800, 450);
                angle = lSamplesNormalised[(int) (music.getPosition() * 44100)] * 45;
                iterations = (int) (lSamplesNormalised[(int) (music.getPosition() * 44100)] * 10) + 5;
                buildMushroom(90, angle, 40, iterations, 800, 450);
                break;
            case (TRIPLE):
                buildMushroom(150, angle, 40, iterations, 800, 450);
                buildMushroom(270, angle, 40, iterations, 800, 450);
                buildMushroom(390, angle, 40, iterations, 800, 450);
                break;
        }

        utils.bloomBegin(true, pos);

        renderer.begin();
        for (int i = 0; i < branches.size; i++) {
            renderer.setColor(colors.get(i).x, colors.get(i).y, (branches.get(i).get(0).y - 100) / 900f, 1);
            renderer.line(branches.get(i).get(0), branches.get(i).get(1));
        }
        renderer.end();

        utils.bloomRender();

        if (render) {
            utils.makeAScreenShot(recorderFrame);
            utils.displayData(recorderFrame, frame);
        }
    }

    private void fadeOut() {
        for (int i = 0; i < colors.size; i++) {
            Vector3 colorV = colors.get(i);
            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - fadeout / 1.5f, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - fadeout * 1.5f, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - fadeout, 0, 1);
                colors.set(i, colorV);
            } else {
                branches.removeIndex(i);
                colors.removeIndex(i);
            }
        }
    }

    void buildMushroom(float initialAngle, float offsetAngle, float branchLength, int iterations, float x, float y) {

        float angleRight = initialAngle + offsetAngle;
        float angleLeft = initialAngle - offsetAngle;

        float sinRight = MathUtils.sinDeg(angleRight) * branchLength;
        float cosRight = MathUtils.cosDeg(angleRight) * branchLength;

        float sinLeft = MathUtils.sinDeg(angleLeft) * branchLength;
        float cosLeft = MathUtils.cosDeg(angleLeft) * branchLength;

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
            float offset = -0.9f;
            float bOffset = 0;
            if (EXPONENTIAL) {
                offset = 1;
                bOffset = 1.7f;
            }
            buildMushroom(angleRight, offsetAngle + offset, branchLength + bOffset, iterations - 1, xRight, yRight);
            buildMushroom(angleLeft, offsetAngle + offset, branchLength + bOffset, iterations - 1, xLeft, yLeft);
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
        super.dispose();
    }
}
