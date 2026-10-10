#!/bin/bash

echo "cleanup...";
rm -rf sdk
rm -rf sdk.apk

echo "build SDK smali...";

cd ../

gradle clean
gradle build

cd ./scripts
cp ../app/build/outputs/apk/debug/app-debug.apk sdk.apk
java -jar apktool_2.4.0.jar d -f -o sdk sdk.apk

# remove unneeded files
echo "remove abondon files...";
rm -rf sdk/res/values/public.xml
rm -rf sdk/res/layout/activity_main.xml
rm -rf sdk/smali/com/test
rm -rf sdk/smali/com/adsfall/demo
rm -rf sdk/smali/com/unity3d/player
rm -rf sdk/smali_classes2/com/unity3d/player

# move files into git
echo "send SDK Smali to git directory...";

rm -rf /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug
mkdir /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug

cp -R sdk/assets /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug
cp -R sdk/lib /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug

cp -R sdk/res /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug
cp -R sdk/smali /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug
cp -R sdk/smali_assets /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug
cp -R sdk/smali_classes2 /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug

cp -R sdk/unknown /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug
cp manifest.xml /Users/ding/workspace/ivyplatform/sdk_smali/0.1/adsfall_debug

date
echo "Done!!!";
