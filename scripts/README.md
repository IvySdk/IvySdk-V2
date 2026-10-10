cd ../ivy-sdk-xiaomi
gradle clean
gradle build
cd ../scripts
cp ../ivy-sdk-xiaomi/app/build/outputs/apk/release/sdk-mi-1.0.apk sdk-mi-1.0.apk
java -jar apktool_2.4.0.jar d -f -o sdk-mi sdk-mi-1.0.apk


mkdir app
cp -R garfield/* app

# inject mi sdk into the app
cp -R sdk-mi/assets/* app/assets/
cp -R sdk-mi/lib/* app/lib/
cp -R sdk-mi/res/* app/res/
cp -R sdk-mi/smali/* app/smali/
cp -R sdk-mi/unknown app/

1. res: public.xml ids.xml deleted 
2. keep all R.java
3. delete layout/activity_main.xml
4. delete demo activity smaili

java -jar apktool_2.4.0.jar b -o garfield_repack.apk app/

# zipalign
zipalign -f -v 4 garfield_repack.apk garfield_repack_aligned.apk

# generate a keystore 
keytool -genkey -v -keystore release.keystore -alias ivygames -storepass 123456 -keypass 123456

# apksigner
apksigner sign --ks release.keystore --ks-key-alias ivygames --ks-pass pass:123456 garfield_repack_aligned.apk