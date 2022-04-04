package com.serenegiant.usb;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;
import java.util.Locale;

public class Format  implements Parcelable {

    public int index;
    /**
     * format type: mjpeg, uncompressed etc.
     */
    public int type;
    public List<Descriptor> frameDescriptors;

    public Format(int index, int type, List<Descriptor> frameDescriptors) {
        this.index = index;
        this.type = type;
        this.frameDescriptors = frameDescriptors;
    }

    protected Format(Parcel in) {
        index = in.readInt();
        type = in.readInt();
        frameDescriptors = in.createTypedArrayList(Descriptor.CREATOR);
    }

    public static final Creator<Format> CREATOR = new Creator<Format>() {
        @Override
        public Format createFromParcel(Parcel in) {
            return new Format(in);
        }

        @Override
        public Format[] newArray(int size) {
            return new Format[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(index);
        dest.writeInt(type);
        dest.writeTypedList(frameDescriptors);
    }

    public static class Descriptor implements Parcelable {

        public int index;
        /**
         * frame type: mjpeg, uncompressed etc.
         */
        public int type;
        public int width;
        public int height;
        public int fps;
        public int frameInterval;
        public List<Interval> intervals;

        /**
         * constructor
         */
        public Descriptor(final int _index, final int _type, final int _width, final int _height, final int _fps, final int _frameInterval, final List<Interval> _intervals) {
            index = _index;
            type = _type;
            width = _width;
            height = _height;
            fps = _fps;
            frameInterval = _frameInterval;
            intervals = _intervals;
        }

        protected Descriptor(Parcel in) {
            index = in.readInt();
            type = in.readInt();
            width = in.readInt();
            height = in.readInt();
            fps = in.readInt();
            frameInterval = in.readInt();
            intervals = in.createTypedArrayList(Interval.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(index);
            dest.writeInt(type);
            dest.writeInt(width);
            dest.writeInt(height);
            dest.writeInt(fps);
            dest.writeInt(frameInterval);
            dest.writeTypedList(intervals);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Descriptor> CREATOR = new Creator<Descriptor>() {
            @Override
            public Descriptor createFromParcel(Parcel in) {
                return new Descriptor(in);
            }

            @Override
            public Descriptor[] newArray(int size) {
                return new Descriptor[size];
            }
        };

        @Override
        public String toString() {
            return String.format(Locale.US, "Size(%dx%d@%d,type:%d,index:%d)", width, height, fps, type, index);
        }
    }

    public static class Interval implements Parcelable {
        public int index;
        public int value;
        public int fps;

        public Interval(int index, int value, int fps) {
            this.index = index;
            this.value = value;
            this.fps = fps;
        }

        protected Interval(Parcel in) {
            index = in.readInt();
            value = in.readInt();
            fps = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(index);
            dest.writeInt(value);
            dest.writeInt(fps);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Interval> CREATOR = new Creator<Interval>() {
            @Override
            public Interval createFromParcel(Parcel in) {
                return new Interval(in);
            }

            @Override
            public Interval[] newArray(int size) {
                return new Interval[size];
            }
        };
    }
}
