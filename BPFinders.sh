#/bin/bash

ANDROID_JARS=/scratch/vh99/zw6098/android-sdk/platforms/

APK_NAME=`basename $1 .apk`

echo $1

java -Xmx10G -jar /scratch/vh99/zw6098/BreakPointsFinder.jar /scratch/vh99/zw6098/Malware_GooglePlay_After2020/$1 $ANDROID_JARS /scratch/vh99/zw6098/android-sdk/build-tools/34.0.0/aapt /scratch/vh99/zw6098/outputFiles/Scenarios
