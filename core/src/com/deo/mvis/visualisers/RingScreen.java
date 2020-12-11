package com.deo.mvis.visualisers;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class RingScreen extends BaseVisualiser implements Screen {

    private Array<Float> radiuses, radiuses2;
    private Array<Vector2> positions;
    private Array<Vector3> colors;
    private float fadeout, ringGrowSpeed;

    public RingScreen() {

        camera = new OrthographicCamera(1600, 900);
        viewport = new ScreenViewport(camera);

        radiuses = new Array<>();
        radiuses2 = new Array<>();
        positions = new Array<>();
        colors = new Array<>();
        fadeout = 0.006f;
        ringGrowSpeed = 12f;

        musicWave.multiplySamples(samplesRaw, 3);

        if (!render) {
            music.play();
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

        fadeout();
        float radiusx, radiusy;
        int pos;
        if (!render) {
            radiusx = lSamplesNormalised[(int) (music.getPosition() * 44100)] * 720;
            radiusy = rSamplesNormalised[(int) (music.getPosition() * 44100)] * 720;
            pos = (int) (music.getPosition() * 44100);
        } else {
            radiusx = lSamplesNormalised[frame] * 720;
            radiusy = rSamplesNormalised[frame] * 720;
            pos = frame;
            frame += step;
            recorderFrame++;
        }

        positions.add(new Vector2(0, 0));
        colors.add(new Vector3(1, 1, 0));

        radiuses.add(radiusx);
        radiuses2.add(radiusy);

        renderer.setProjectionMatrix(camera.combined);
        utils.setBatchProjMat(camera.combined);

        utils.bloomBegin(true, pos);
        renderer.begin();
        for (int i = 0; i < positions.size; i++) {
            renderer.setColor(colors.get(i).x, colors.get(i).y, colors.get(i).z, 1);
            renderer.ellipse(positions.get(i).x - radiuses.get(i) / 2f, positions.get(i).y - radiuses2.get(i) / 2f, radiuses.get(i), radiuses2.get(i));
        }
        renderer.end();
        utils.bloomRender();

        if (render) {
            utils.makeAScreenShot(recorderFrame);
            utils.displayData(recorderFrame, frame);
        }

    }

    void fadeout() {
        for (int i = 0; i < positions.size; i++) {
            radiuses.set(i, radiuses.get(i) + ringGrowSpeed);
            radiuses2.set(i, radiuses2.get(i) + ringGrowSpeed);
            Vector3 colorV = colors.get(i);
            if (colorV.x + colorV.y + colorV.z >= fadeout) {
                colorV.x = MathUtils.clamp(colorV.x - fadeout / 1.5f, 0, 1);
                colorV.y = MathUtils.clamp(colorV.y - fadeout * 1.5f, 0, 1);
                colorV.z = MathUtils.clamp(colorV.z - fadeout, 0, 1);
                colors.set(i, colorV);
            } else {
                positions.removeIndex(i);
                radiuses.removeIndex(i);
                radiuses2.removeIndex(i);
                colors.removeIndex(i);
            }
        }
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
