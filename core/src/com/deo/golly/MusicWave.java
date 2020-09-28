package com.deo.golly;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class MusicWave {

    private float[] samples;
    private float[] leftChannelSamples;
    private float[] rightChannelSamples;
    private Music music;

    public MusicWave() {

        String path;

        path = "!DeltaCore/wav(resonance).wav";

        File file = Gdx.files.external(path).file();

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            int frameLength = (int) audioInputStream.getFrameLength();
            int frameSize = audioInputStream.getFormat().getFrameSize();
            byte[] bytes = new byte[frameLength * frameSize];
            try {
                audioInputStream.read(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }

            float[][] twoChannelSamples = getUnscaledAmplitude(bytes, audioInputStream.getFormat().getChannels());

            float[] averageChannelAmplitude = new float[twoChannelSamples[0].length];
            leftChannelSamples = new float[twoChannelSamples[0].length];
            rightChannelSamples = new float[twoChannelSamples[1].length];

            for(int i = 0; i<averageChannelAmplitude.length; i++){
                averageChannelAmplitude[i] = Math.abs(twoChannelSamples[0][i] + twoChannelSamples[1][i])/2.0f;
                leftChannelSamples[i] = twoChannelSamples[0][i];
                rightChannelSamples[i] = twoChannelSamples[1][i];
            }

            samples = averageChannelAmplitude;

            music = Gdx.audio.newMusic(Gdx.files.external(path));

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    private int getSixteenBitSample(int high, int low) {
        return (high << 8) + (low & 0x00ff);
    }

    private float[][] getUnscaledAmplitude(byte[] eightBitByteArray, int nbChannels) {

        float[][] toReturn = new float[nbChannels][eightBitByteArray.length / (2 * nbChannels)];
        int index = 0;

        for (int audioByte = 0; audioByte < eightBitByteArray.length; ) {
            for (int channel = 0; channel < nbChannels; channel++) {
                int low = eightBitByteArray[audioByte];
                audioByte++;
                int high = eightBitByteArray[audioByte];
                audioByte++;
                int sample = getSixteenBitSample(high, low);

                toReturn[channel][index] = sample;
            }
            index++;
        }

        return toReturn;
    }

    public float[] getSamples(){
        return samples;
    }

    public float[] getLeftChannelSamples() {
        return leftChannelSamples;
    }

    public float[] getRightChannelSamples() {
        return rightChannelSamples;
    }

    public Music getMusic(){
        return music;
    }


}
