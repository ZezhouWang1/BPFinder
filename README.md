# BPFinder
# BPFinder: the framework/tool for detecting the break points (BP) in the call graph of Android Apps. <br>
This BPFinder project is primarily used as the Honour Research Project (COMP4550) for honour student Zezhou Wang at The Australian National University (ANU)<br>
Work with Supervisor Dr. Xiaoyu Sun, Ph.D. student Yitong Wang from ANU <br>

Guideline:
All the sh script for implementing HPC are included in the sh scrpit folder, and all the source code is available src folder.<br>
The static analysis and dynamic analysis component are integrated as BPFinder-Static.jar and BPFinder-Dynamic.jar respectively.<br>
# To use BPFinder-Static.jar, follow this command:
```java
java -jar BPFinder-Static.jar <apk path> <android jar path> <aapt path>  <txt saved path>
```
# To use BPFinder-Dynamic.jar, follow this command:
```java
java -jar BPFinder-Dynamic.jar args[]
```
args[0] = TAG, args[1] = APK Path，args[2] = ADB Path，(you may ignore this), args[3] = APKSigner Path，<br>

args[4] = Keystore Path，args[5] = Keystore Pwd，args[6] = Android Jar Path，args[7] = AAPT Path,<br>

args[8] = Keystore Alias，args[9] = Output Directory

