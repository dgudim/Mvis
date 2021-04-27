package com.deo.mvis.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class ColorPallets {

    public static Color fadeBetweenTwoColors(Color color1, Color color2, float ratio){
        return new Color(mixTwoColors(color1, color2, ratio));
    }

    public static int interpolate(float step, Color... colors) {
        step = Math.max(Math.min(step, 1.0f), 0.0f);

        switch (colors.length) {
            case 0:
                throw new IllegalArgumentException("At least one color required.");

            case 1:
                return Color.argb8888(colors[0]);

            case 2:
                return mixTwoColors(colors[0], colors[1], step);

            default:

                int firstColorIndex = (int) (step * (colors.length - 1));

                if (firstColorIndex == colors.length - 1) {
                    return Color.argb8888(colors[colors.length - 1]);
                }

                // stepAtFirstColorIndex will be a bit smaller than step
                float stepAtFirstColorIndex = (float) firstColorIndex
                        / (colors.length - 1);

                // multiply to increase values to range between 0.0f and 1.0f
                float localStep = (step - stepAtFirstColorIndex)
                        * (colors.length - 1);

                return mixTwoColors(colors[firstColorIndex],
                        colors[firstColorIndex + 1], localStep);
        }

    }

    public static int mixTwoColors(Color color1, Color color2, float ratio) {
        return Color.rgba8888(color1.r * (1f - ratio) + color2.r * ratio, color1.g * (1f - ratio) + color2.g * ratio, color1.b * (1f - ratio) + color2.b * ratio, color1.a * (1f - ratio) + color2.a * ratio);
    }

}
