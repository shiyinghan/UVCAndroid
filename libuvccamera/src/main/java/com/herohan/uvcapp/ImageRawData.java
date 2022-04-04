package com.herohan.uvcapp;

class ImageRawData {
    private byte[] mData;
    private int mWidth;
    private int mHeight;

    public ImageRawData(byte[] data, int width, int height) {
        this.mData = data;
        this.mWidth = width;
        this.mHeight = height;
    }

    public byte[] getData() {
        return mData;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}
