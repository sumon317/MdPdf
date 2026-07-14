#!/bin/sh
adb install -r /home/snas/code/AndroidStudioProjects/MdPdf/app/build/outputs/apk/debug/app-debug.apk 2>&1 | tr '\r' '\n' | tail -1
echo "---"
adb shell am start -n com.example.mdpdf/.MainActivity 2>&1
