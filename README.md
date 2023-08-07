[![Maven Central](https://img.shields.io/maven-central/v/com.herohan/UVCAndroid.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.herohan%22%20AND%20a:%22UVCAndroid%22)

UVCAndroid
=========

Library and sample to access UVC camera on non-rooted Android device

[中文文档： UVCAndroid，安卓UVC相机通用开发库](https://blog.csdn.net/hanshiying007/article/details/124118486)

How do I use it?
---

### Setup

##### Dependencies
```groovy
repositories {
  mavenCentral()
}

dependencies {
    implementation 'com.herohan:UVCAndroid:1.0.5'
}
```
R8 / ProGuard
-------------

If you are using R8 the shrinking and obfuscation rules are included automatically.

ProGuard users must manually add the below options.
```groovy
-keep class com.herohan.uvcapp.** { *; }
-keep class com.serenegiant.usb.** { *; }
-keepclassmembers class * implements com.serenegiant.usb.IButtonCallback {*;}
-keepclassmembers class * implements com.serenegiant.usb.IFrameCallback {*;}
-keepclassmembers class * implements com.serenegiant.usb.IStatusCallback {*;}
```

Requirements
--------------
Android 5.0+