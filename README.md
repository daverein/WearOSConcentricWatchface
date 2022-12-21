Concentric Native (WearOS Watchface)
===============================
My version of the Pixel Concentric watchface.

Features
--------
* 3 faces
* Large(r) fonts
* 2-3 Complications on the watchfaces
* Always On Display friendly, and can adjust watchface, complications, and even the time
* Full-on AOD for when you want it to show nicely (but probably uses more power)
* Multiple highlight colors
* Pixel-shifting to avoid burn-in during AOD
* Control over colors and complication elements

Introduction
------------
This is my version of the Google Pixel Concentric Watchface.  It started off being as closely mirroring it as possible, but then I realized I was too old to see the time with those font sizes... and lack of complications for several of the different faces it provided.  So I made the fonts bigger and added complications.  And made the fonts even bigger when in Always on Display (ambient) mode.

Always On Display
-----------------
The Watchface is optimized for AOD, allowing everything from parts of the watchface, the complications, and even the time to be optionally displayed.  It can even be stripped down to just showing just the new notification indicator, enabling those of us with OCD to avoid constantly flipping our wrists to see if we missed a message.  But there is also a "damn the battery, show me everthing!" mode, too!

Code
----
The code is a direct dirivative work from the Kotlin watchface example of the androidx code samples:
https://github.com/android/wear-os-samples/tree/main/WatchFaceKotlin

Screenshots
-------------

Face 1:

<img src="screenshots/screenshot_full.png" width="400" alt="Concentric Native Watchface"/>

Face 2:

<img src="screenshots/screenshot_half.png" width="400" alt="Concentric Native Config"/>

Face 3:

<img src="screenshots/screenshot_halfzoom.png" width="400" alt="Concentric Native Config"/>

Battery-Friendly Always On Display screens (with options to show or hide complications):

<img src="screenshots/aod1.png" width="400" alt="Concentric Native Config"/>
<img src="screenshots/aod2.png" width="400" alt="Concentric Native Config"/>
<img src="screenshots/aod3.png" width="400" alt="Concentric Native Config"/>
<img src="screenshots/aod4.png" width="400" alt="Concentric Native Config"/>

Full-on Always On Display screens:

<img src="screenshots/aod5.png" width="400" alt="Concentric Native Config"/>
<img src="screenshots/aod6.png" width="400" alt="Concentric Native Config"/>
<img src="screenshots/aod7.png" width="400" alt="Concentric Native Config"/>

Configuration screen:

<img src="screenshots/config.png" width="400" alt="Concentric Native"/>


Getting Started to build
---------------

This sample uses the Gradle build system. To build this project, use the "gradlew build" command or
use "Import Project" in Android Studio.

Support
-------

- Stack Overflow: http://stackoverflow.com/questions/tagged/android

If you've found an error in this sample, please file an issue:
https://github.com/daverein/WearOSConcentricWatchface/issues

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub.
