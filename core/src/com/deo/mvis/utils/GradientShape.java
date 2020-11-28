package com.deo.mvis.utils;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import static com.deo.mvis.utils.ColorPallets.fadeBetweenTwoColors;

public class GradientShape {

    Array<float[]> vertices;
    static int verticesPerGradient;
    static int gradientSteps;
    public float x;
    public float y;
    public float radius;
    Array<Color> colors;
    Array<Color> colorsFrom;
    Array<Color> colorsTo;

    public GradientShape() {
        vertices = new Array<>();
        colors = new Array<>();
        colorsFrom = new Array<>();
        colorsTo = new Array<>();
    }

    public void draw(ShapeRenderer shapeRenderer, float colorFadeout) {
        for (int i = 0; i < vertices.size; i++) {
            float[] newVertices = new float[verticesPerGradient];
            for (int v = 0; v < verticesPerGradient; v++) {
                if (v % 2 == 0) {
                    newVertices[v] = vertices.get(i)[v] + x;
                } else {
                    newVertices[v] = vertices.get(i)[v] + y;
                }
            }
            shapeRenderer.setColor(fadeBetweenTwoColors(colorsFrom.get(i), colorsTo.get(i), i / (float) gradientSteps * colorFadeout));
            shapeRenderer.polygon(newVertices);
        }
    }

    public GradientShape buildGradientPolygon(float radius, int gradientSteps, float rotationOffset, float x, float y, int faces, float angleStep, Color from, Color to, float colorFadeout) {
        radius = 11 * radius / 10f;
        GradientShape.gradientSteps = gradientSteps;
        this.radius = radius;
        float step = 1 / (float) gradientSteps;
        float radiusStep = radius * step;
        float currentRadius = radius;
        for (int i = 0; i < gradientSteps; i++) {
            vertices.add(calculatePolygon(x, y, currentRadius, rotationOffset, faces, angleStep));
            currentRadius -= radiusStep;
            colors.add(fadeBetweenTwoColors(from, to, i * step * colorFadeout));
            colorsFrom.add(from);
            colorsTo.add(to);
        }
        return this;
    }

    public static float[] calculatePolygon(float x, float y, float size, float angleOffset, int faces, float angleStep) {
        verticesPerGradient = faces*2;
        float[] vertices = new float[faces * 2];
        if (angleStep == 0) {
            angleStep = 360 / (float)faces;
        }
        for (int i = 0; i < faces; i++) {
            float x1 = -MathUtils.cosDeg(angleOffset + i * angleStep) * size + x;
            float y1 = -MathUtils.sinDeg(angleOffset + i * angleStep) * size + y;
            vertices[i * 2] = x1;
            vertices[i * 2 + 1] = y1;
        }
        return vertices;
    }

}
