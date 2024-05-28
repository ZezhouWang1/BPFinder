# Dynamic jar user guide

## Parameter List

```cmd
java -jar instrumenting-extend.jar args[]
```

args参数列表：

args[0] = TAG, fill in as you like

args[1] = APK Path，the path to testing APK

args[2] = ADB Path，you may ignore this

args[3] = APKSigner Path，the path to APKSigner scrpit，commonly in sdk//build-tools//(version) directory

args[4] = Keystore Path，the path to keystore file，KS.keystore is provided there

args[5] = Keystore Pwd，password foro keystore，KS.keystore's password is 123456

args[6] = Android Jar Path，the path to Android Jar，commonly sdk//platforms

args[7] = AAPT Path，the path to aapt，commonly in sdk//build-tools//(version) directory

args[8] = Keystore Alias，if KS.keystore is used, then type KA

args[9] = Output Directory，the output directory 

## Example

```cmd
java -jar instrumenting-extend.jar VirusShare E:\virusshare\output-apk\apk\2.apk "D:\Program Files\Nox\bin\adb.exe" D:\\BaiduNetdiskDownload\\android-sdk_r24.4.1-windows\\android-sdk-windows\\build-tools\\29.0.3\\apksigner.bat D:\\BaiduNetdiskDownload\\android-sdk_r24.4.1-windows\\android-sdk-windows\\build-tools\\29.0.3\\KS.keystore 123456 D:\\BaiduNetdiskDownload\\android-sdk_r24.4.1-windows\\android-sdk-windows\\platforms D:\\BaiduNetdiskDownload\\android-sdk_r24.4.1-windows\\android-sdk-windows\\build-tools\\33.0.1\\aapt.exe KA .\\test2
```

Aftering executing, jar will generate SootOutput file in the same level，storing the apk after code instrumentation；Output Directory will store the generated callgraph-dot，callgraph-txt，dfs，gml，method-names and output file.
