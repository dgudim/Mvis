package com.deo.mvis;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

public class ColorPallets {

    public static Color fadeBetweenTwoColors(Color color1, Color color2, float i){
        i = MathUtils.clamp(i, 0, 1);

        float r = color1.r*(1-i) + color2.r*i;
        float g = color1.g*(1-i) + color2.g*i;
        float b = color1.b*(1-i) + color2.b*i;
        float a = color1.a*(1-i) + color2.a*i;

        return new Color().set(r, g, b, a);
    }

    public static Color fadeBetweenThreeColors(Color color1, Color color2, Color color3, float i){
        i = MathUtils.clamp(i, -1, 1);
        if(i>=0){
            return fadeBetweenTwoColors(color2, color3, i);
        }else{
            return fadeBetweenTwoColors(color2, color1, -i);
        }
    }

}
