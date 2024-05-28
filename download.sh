#!/bin/bash

APK_DIR="/scratch/vh99/zw6098/GOODWARE-Scenarois"

NCI_APK_DIR="/scratch/vh99/zw6098/Goodware_GooglePlay_After2020"

OUTPUT_DIR="/scratch/vh99/zw6098/output-apk"

for file in "$APK_DIR"/Scenario-*.txt; do

    apkname=$(basename "$file" .txt | sed 's/Scenario-//')

    scr_file="$NCI_APK_DIR/$apkname.apk"

    dest_file="$OUTPUT_DIR/$apkname.apk"

    if [ -f "$scr_file" ]; then
        
        echo "Copying $scr_file to $dest_file"
        cp "$scr_file" "$dest_file"
    fi
done