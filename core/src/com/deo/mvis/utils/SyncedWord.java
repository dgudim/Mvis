package com.deo.mvis.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

import static com.deo.mvis.Launcher.WIDTH;

public class SyncedWord {

    private final int timestamp;
    private final float x;
    private final float y;
    private final BitmapFont font;
    private final String text;
    private float alpha = 0;
    private final float fadeoutSpeed;
    private boolean displayed = false;
    public boolean dispose = false;
    private final float fadeInSpeed;
    private boolean hasFadedIn = false;

    public SyncedWord(float x, float y, int timestamp, int nextTimestamp, String text, BitmapFont font, float fadeInSpeed) {
        this.timestamp = timestamp;
        this.font = font;
        this.text = text;
        this.x = x;
        this.y = y;
        this.fadeInSpeed = fadeInSpeed;
        
        font.setUseIntegerPositions(false);

        fadeoutSpeed = MathUtils.clamp(1 / (float) (nextTimestamp - timestamp) * 1.3f, 0.00015f, 1000000);

        //System.out.println(timestamp+"   "+nextTimestamp+"   "+text);
    }

    public void drawAndUpdate(float currentSongPos, SpriteBatch batch, Color color) {
        //System.out.println(text + "   "+timestamp+"   "+currentSongPos*1000);
        if (!dispose) {
            if (timestamp - 1000 / fadeInSpeed <= currentSongPos * 1000 && !displayed) {
                displayed = true;
            }
            if (displayed && !hasFadedIn) {
                alpha = MathUtils.clamp(alpha + fadeInSpeed * Gdx.graphics.getDeltaTime(), 0, 1);
                hasFadedIn = displayed && alpha == 1;
            }
            if (displayed) {
                font.setColor(color.r * 0.5f, color.g * 0.5f, color.b * 0.5f, alpha);
                font.draw(batch, text, x + 2, y + 2 - (alpha - 1) * 10, WIDTH, 1, false);
                font.setColor(color.r, color.g, color.b, alpha);
                font.draw(batch, text, x, y - (1 - alpha) * 10, WIDTH, 1, false);
                if (hasFadedIn) {
                    alpha = MathUtils.clamp(alpha - fadeoutSpeed * 700 * Gdx.graphics.getDeltaTime(), 0, 1);
                }
                dispose = displayed && alpha == 0 && hasFadedIn;
            }
        }
    }

    public void dispose() {
        font.dispose();
    }

}
