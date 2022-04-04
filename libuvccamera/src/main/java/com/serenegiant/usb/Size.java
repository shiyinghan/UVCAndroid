package com.serenegiant.usb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Size implements Parcelable, Cloneable {
    /**
     * frame type: mjpeg, uncompressed etc.
     */
    public int type;
    public int width;
    public int height;
    public int fps;
    public List<Integer> fpsList;

    /**
     * constructor
     */
    public Size(final int _type, final int _width, final int _height, final int _fps, final List<Integer> _fpsList) {
        type = _type;
        width = _width;
        height = _height;
        fps = _fps;
        fpsList = _fpsList;
    }

    protected Size(Parcel in) {
        type = in.readInt();
        width = in.readInt();
        height = in.readInt();
        fps = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeInt(fps);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Size> CREATOR = new Creator<Size>() {
        @Override
        public Size createFromParcel(Parcel in) {
            return new Size(in);
        }

        @Override
        public Size[] newArray(int size) {
            return new Size[size];
        }
    };

    @Override
    public String toString() {
        return String.format(Locale.US, "Size(%dx%d@%d,type:%d)", width, height, fps, type);
    }

    @Override
    public Size clone() {
        Size size = null;
        try {
            size = (Size) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        if (size == null) {
            size = new Size(type, width, height, fps, new ArrayList<>(fpsList));
        } else {
            size.fpsList = new ArrayList<>(fpsList);
        }
        return size;
    }
}
