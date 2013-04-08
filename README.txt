There are two elements to using this, building a plugin that will work specifically with your app, and splitting your app. It's based on the splitter and instructions from here: http://forum.unity3d.com/threads/129042-Android-Builder-Tool/page4 and on Google's obb sample.

Set up involves a couple of steps:

1) Install Eclipse (classic edition for example), the Android SDK and the ADT Plugin for Eclipse

2) Download Android Builder Tool 

5) In Eclipse, right click and chose Import, Android Project, from existing source code. Browse to "AndroidBuilder/Projects/InstallerActivity", choose the InstallerActivity project, uncheck "copy to Workspace", and finish.

If you get an error like "Unable to resolve target 'android-X'" after the Import process, this is because the right SDK platform is not installed on your computer. To solve this, launch the Android SDK _from Eclipse_ (not the standalone one), and choose to install the right one (for example, if you get "Unable to resolve target 'android-15', you need to install the SDK platform marked as API 15 in the Android SDK.

5) You need to copy a file, "classes.jar", corresponding to your unity version. 

It is located to:
- Windows: C:\Program Files\Unity\Editor\Data\PlaybackEngine\AndroidBuil der\bin
- Mac: Go to Applications/Unity and right-click on Unity (executable) and chose "Display package content", then go to Content/PlaybackEngine/AndroidBuilder/bin

So, copy "Classes.jar" to your eclipse project in "/AndroidBuilder/Projects/InstallerActivity/libs" (replacing the existing one)

6) Right click on your project (in Eclipse) and chose "Build Path" -> "Configure Build Path". Then go to the "Libraries tab" and Remove whatever "classes.jar" there are in the list. Then, click "Add JARs" and browse to your fresh copied "classes.jar" (<yourProject>/libs)

7) Update the com.yourproject.yourapp java files in the InstallerActivity project (src and gen). Rename that package to the same bundle name as your app in Unity. In the com.yourdomain.yourapp.YourAppDownloaderActivity.java file you must set your public key, version code and change the salt.

8) Modify the Manifest.xml file package name. It needs to match the bundle name you enter in "player settings" in Unity. 

You may need to correct errors after steps 7 & 8, because you change the package name. Just rename the Import line to point to your new package (example: if your bundle name is com.example.test, the line must be: Import com.example.test.R

9) Right click on your "src" folder, in Eclipse, and choose refresh. Then right click it again and choose "Export -> Java -> JAR". Save it to /AndroidBuilder/Tools/InstallerActivity.jar

10) Go to Unity, and launch an Android Build.

11) cd into /AndroidBuilder/Tools and use the commandline splitter as below. You will find your Unity APK, and your two splitted APK (Installer + GameData) at the location you set in Unity. 

12) Rename the gamedata file to main.X.com.yourdomain.yourapp.obb where X is the same version code you set in the manifest and DownloaderActivity class above.

13) Remove any existing instance of your app from your test device, manually create the /Android/obb/com.yourdomain.yourapp/ folder on your device's SDcard (there should already at least be an /Android/data/) and copy the renamed file there. I find having an FTP server app on the device is the quickest/easiest way to do this on mac, google's file manager app is very unreliable. Note you'll need to recreate and recopy this file every time you redeploy

14) Run your app, if all is correct it should load your game from the obb file

15) Upload your installer app and obb to google play (when you upload your installer, you can upload an extension file)

16) Delete the obb file you copied across earlier from your device and run the game again, this time it should detect and download the obb from google play.

17) You should be ready to publish your app and have it download from the store.

USING THE ANDROIDBUILDER COMMANDLINE TOOL:
AndroidBuilder is a commandline tool that will split an existing apk into two, a small installer and a data package that it will download to the device. Run it with java using commandline args similar to the following:

java -jar AndroidBuilder.jar /path/to/originalbuild.apk -t /path/to/AndroidBuilder/Tools/ -r /path/to/AndroidBuilder/Projects/InstallerActivity -u /path/to/UnityProject/ -s /path/to/yourproject.keystore keystorePass alias aliasPass

Arguments: (note you should use absolute paths)
	-t Path to tools (folder containing this jar)
	-r Path to InstallerActivity project
	-u Path to Unity project
	-a Path to android tools root
	-dm Turns debug mode on or off
	-dc Turns debug certificate on or off
	-s Specifies non-debug signing details (as specified in Unity Android Signing settings)
