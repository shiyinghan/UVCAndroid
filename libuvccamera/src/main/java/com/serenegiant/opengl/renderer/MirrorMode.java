package com.serenegiant.opengl.renderer;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({MirrorMode.MIRROR_NORMAL, MirrorMode.MIRROR_HORIZONTAL,
        MirrorMode.MIRROR_VERTICAL, MirrorMode.MIRROR_BOTH})
@Retention(RetentionPolicy.SOURCE)
public @interface MirrorMode {
    int MIRROR_NORMAL = 0;
    int MIRROR_HORIZONTAL = 1;
    int MIRROR_VERTICAL = 2;
    int MIRROR_BOTH = 3;
    int MIRROR_NUM = 4;
}
