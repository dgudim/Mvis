package com.deo.mvis.utils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MusicWave {

    public static final int HEADER_SIZE = 44;

    private float[] samples;
    private float[] leftChannelSamples;
    private float[] rightChannelSamples;
    private Music music;

    public MusicWave(FileHandle musicFile) {

        try {

            InputStream wavStream = musicFile.read();
            WavInfo header = readHeader(wavStream);
            byte[] bytes = readWavPcm(header, wavStream);
            int channels = 1;
            if (header.isStereo) {
                channels = 2;
            }

            float[][] twoChannelSamples = getUnscaledAmplitude(bytes, channels);

            float[] averageChannelAmplitude = new float[twoChannelSamples[0].length];
            leftChannelSamples = new float[twoChannelSamples[0].length];
            rightChannelSamples = new float[twoChannelSamples[1].length];

            for (int i = 0; i < averageChannelAmplitude.length; i++) {
                averageChannelAmplitude[i] = Math.abs(twoChannelSamples[0][i] + twoChannelSamples[1][i]) / 2.0f;
                leftChannelSamples[i] = twoChannelSamples[0][i];
                rightChannelSamples[i] = twoChannelSamples[1][i];
            }

            samples = averageChannelAmplitude;

            music = Gdx.audio.newMusic(musicFile);

        } catch (IOException e) {
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

    public float[] getSamples() {
        return samples;
    }

    public float[] getLeftChannelSamples() {
        return leftChannelSamples;
    }

    public float[] getRightChannelSamples() {
        return rightChannelSamples;
    }

    public Music getMusic() {
        return music;
    }

    public float[] normaliseSamples(boolean cutoff, boolean absolute, float[] samples) {
        float maxValue = 0;
        for (float sample : samples) {
            if (Math.abs(sample) > maxValue) {
                maxValue = Math.abs(sample);
            }
        }
        for (int i = 0; i < samples.length; i++) {
            samples[i] = samples[i] / maxValue;
            if (cutoff) {
                samples[i] = Math.max(samples[i], 0);
            }
            if (absolute) {
                samples[i] = Math.abs(samples[i]);
            }
        }
        return samples;
    }

    public float[] smoothSamples(float[] samples, int smoothingFactor, int smoothingSampleRange) {
        for (int i = 0; i < smoothingFactor; i++) {
            for (int i2 = smoothingSampleRange; i2 < samples.length - smoothingSampleRange; i2++) {
                float sum = samples[i2]; // middle sample
                for (int i3 = 1; i3 < smoothingSampleRange; i3++) {
                    sum = sum + samples[i2 + i3] + samples[i2 - i3];
                    // samples to the left and to the right
                }
                samples[i2] = sum / (float) (smoothingSampleRange + 1); //smooth out the sample
            }
        }
        return normaliseSamples(false, false, samples);
    }

    public float[] multiplySamples(float[] samples, float factor) {
        for (int i = 0; i < samples.length; i++) {
            samples[i] *= factor;
        }
        return samples;
    }

    public float[] getSamplesForFFT(int pos, int i) {
        float[] newSamples = new float[i];
        for (int i2 = 0; i2 < i; i2++) {
            newSamples[i2] = samples[pos + i2];
        }
        return newSamples;
    }

    protected void checkFormat(boolean assertion, String message) throws IOException {
        if (!assertion) {
            throw new IOException(message);
        }
    }

    public static class WavInfo {
        int sampleRate;
        int bits;
        int dataSize;

        boolean isStereo;

        public WavInfo(int rate, int bits, boolean isStereo, int dataSize) {
            this.sampleRate = rate;
            this.bits = bits;
            this.isStereo = isStereo;
            this.dataSize = dataSize;
        }
    }

    private WavInfo readHeader(InputStream wavStream) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());

        buffer.rewind();
        buffer.position(buffer.position() + 20);

        int format = buffer.getShort();

        checkFormat(format == 1, "Unsupported encoding: " + format); // 1 means
        // Linear
        // PCM
        int channels = buffer.getShort();

        checkFormat(channels == 1 || channels == 2, "Unsupported channels: "
                + channels);

        int rate = buffer.getInt();

        checkFormat(rate <= 48000 && rate >= 11025, "Unsupported rate: " + rate);

        buffer.position(buffer.position() + 6);

        int bits = buffer.getShort();
        //checkFormat(bits == 16, "Unsupported bits: " + bits);

        int dataSize = 0;

        while (buffer.getInt() != 0x61746164) { // "data" marker
            int size = buffer.getInt();
            wavStream.skip(size);

            buffer.rewind();
            wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
            buffer.rewind();
        }

        dataSize = buffer.getInt();

        checkFormat(dataSize > 0, "wrong datasize: " + dataSize);

        return new WavInfo(rate, bits, channels == 2, dataSize);
    }

    private byte[] readWavPcm(WavInfo info, InputStream stream) throws IOException {

        byte[] data = new byte[info.dataSize];
        stream.read(data, 0, data.length);

        return data;
    }

    public void dispose(){
        music.dispose();
        samples = null;
        leftChannelSamples = null;
        rightChannelSamples = null;
        System.gc();
    }

}
