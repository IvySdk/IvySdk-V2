#!/bin/bash

version=2.0.53
smali_root_dir=/Users/dingjiansheng/workspace/ivyplatform

echo "cleanup...";
rm -rf sdk
rm -rf sdk.apk

echo "build SDK smali version $version...";

cd ../

gradle clean
gradle build

cd ./scripts
cp ../app/build/outputs/apk/release/app-release.apk sdk.apk
java -jar apktool_2.4.0.jar d -f -o sdk sdk.apk

# remove unneeded files
echo "remove abandon files...";
rm -rf sdk/res/values/public.xml
rm -rf sdk/res/layout/activity_main.xml
rm -rf sdk/smali/com/test
rm -rf sdk/smali/com/adsfall/demo
rm -rf sdk/smali/com/unity3d/player
rm -rf sdk/smali_classes2/com/unity3d/player
rm -rf sdk/smali_classes2/com/test


# move files into git
echo "send SDK Smali to git directory...";

rm -rf $smali_root_dir/sdk_smali/0.1/adsfall_v$version/
mkdir $smali_root_dir/sdk_smali/0.1/adsfall_v$version/

cp -R sdk/assets $smali_root_dir/sdk_smali/0.1/adsfall_v$version/
cp -R sdk/lib $smali_root_dir/sdk_smali/0.1/adsfall_v$version/

cp -R sdk/res $smali_root_dir/sdk_smali/0.1/adsfall_v$version/
cp -R sdk/smali $smali_root_dir/sdk_smali/0.1/adsfall_v$version/
cp -R sdk/smali_assets $smali_root_dir/sdk_smali/0.1/adsfall_v$version/
cp -R sdk/smali_classes2 $smali_root_dir/sdk_smali/0.1/adsfall_v$version/

cp -R sdk/unknown $smali_root_dir/sdk_smali/0.1/adsfall_v$version/
cp manifest.xml $smali_root_dir/sdk_smali/0.1/adsfall_v$version/
 
date
echo "Done!!!";
