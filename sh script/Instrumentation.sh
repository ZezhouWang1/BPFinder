#/bin/bash

ANDROID_JARS=/scratch/vh99/zw6098/android-sdk/platforms/

APK_NAME=`basename $1 .apk`

echo $1

java -Xmx10G -jar /scratch/vh99/zw6098/instrumenting-extend.jar WZZTEST /scratch/vh99/zw6098/output-apk/$1 xxx /scratch/vh99/zw6098/android-sdk/build-tools/34.0.0/apksigner /scratch/vh99/zw6098/KS.keystore 123456 $ANDROID_JARS /scratch/vh99/zw6098/android-sdk/build-tools/34.0.0/aapt KA /scratch/vh99/zw6098/outputFiles/instrumentation
