package com.example.ivars.cubecontrol;

import java.nio.ByteBuffer;

/**
 * Created by Ivars on 2017.06.28..
 */

public class LedScene {
    private int mSceneTimeInMillis;
    private byte[] mLevels = new byte[8];
    private int mSceneNumber;

    public LedScene(int[][] ledScene, int sceneLenght, int sceneNumber) {
        mSceneNumber = sceneNumber;
        int byteCounter = 0;
        int shiftCounter = 7;
        mSceneTimeInMillis = sceneLenght;
        for (int x = 0; x < 4; x++) {
            //loop through levels
            for (int y = 0; y < 16; y++) {
                mLevels[byteCounter] |= ledScene[x][y] << shiftCounter;
                shiftCounter--;
                if (shiftCounter < 0) {
                    shiftCounter = 7;
                    byteCounter++;
                }
            }
        }
    }

    //constructor for when creating UI from saved file
    public LedScene(byte[] sceneArray, int sceneNumber) {
        mSceneNumber = sceneNumber;
        //here we receive the array of bytes
        //1st 4 bytes are time then 8 scene bytes
        int bitShiftCount = 24;
        mSceneTimeInMillis = 0;
        for (int i = 0; i < 4; i++) {
            mSceneTimeInMillis += ((sceneArray[i] & 0xff) << bitShiftCount);
            bitShiftCount -= 8;
            //Log.e("Millis: ", String.valueOf(mSceneTimeInMillis));
        }

        for (int j = 4; j < 12; j++) {
            mLevels[j - 4] = sceneArray[j];
        }
    }

    public int getSceneTimeInMillis() {
        return mSceneTimeInMillis;
    }

    public byte[] getLevels() {
        return mLevels;
    }

    public int getSceneNumber() {
        return mSceneNumber;
    }

    @Override
    public String toString() {
        String ledSceneString;
        ledSceneString = String.valueOf(mSceneTimeInMillis);
        for (int i = 0; i < 8; i++) {
            ledSceneString += " " + mLevels[i];
        }
        return ledSceneString;
    }

    public byte[] getBytes() {
        byte[] data = new byte[12];
        byte[] temp = ByteBuffer.allocate(4).putInt(mSceneTimeInMillis).array();
        for (int i = 0; i < 4; i++) {

            data[i] = temp[i];
            //Log.e("getBytes", String.valueOf(data[i]));
        }
        for (int j = 4; j < 12; j++) {
            data[j] = mLevels[j - 4];
            //Log.e("getBytes", String.valueOf(data[j]));
        }
        return data;
    }
}
