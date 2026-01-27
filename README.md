Camera2Basic-WithAI
===========================
A horrible Android AI app that do Image recognization stuff that AI image recognization stuff.
I've made this app only because it's part of a college homework, and what a massive pain in the butt to making this.
Even with decyphering English documentation, help of AIs, it took a few weeks to make this nonsense.

Introduction
------------

The [Camera2 API][1] allows users to capture RAW images, i.e. unprocessed pixel data
directly from the camera sensor that has not yet been converted into a format and
colorspace typically used for displaying and storing images viewed by humans.  The
[DngCreator][2] class is provided as part of the Camera2 API as a utility for saving
RAW images as DNG files.

This project incoporates that, but with androidx.activity:activity:1.7.0, androidx.activity:activity-compose:1.7.0 to load images, and Firebase and Gemini APIs to proccessing image recognization to do job for AI.

This sample displays a live camera preview in a TextureView, and saves JPEG and DNG
file for each image captured.

[1]: https://developer.android.com/reference/android/hardware/camera2/package-summary.html
[2]: https://developer.android.com/reference/android/hardware/camera2/DngCreator.html

Pre-requisites
--------------

- Android SDK 29+
- Android Studio 3.5+

Screenshots
-------------

<img src="screenshots/main.png" height="400" alt="Screenshot"/> 

Getting Started
---------------

This sample uses the Gradle build system. To build this project, use the
"gradlew build" command or use "Import Project" in Android Studio.
