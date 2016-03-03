# AndroidWiimote
Wiimote Plugin for Unity on Android

This is a combined project between Android Studio and Unity 3D. It is intended to provide access to a Wiimote for use with Unity applications, including Google Cardboard applications.

###NOTE
Android does not support the L2CAP protocol as of Android 4.2. I am currently investigating possible workarounds that would not require rooting an Android device. Wiimote devices can be discovered and paired, but connecting fails.

##WiimotePlugin
WiimotePlugin is an Android Studio library project. It is located in the WiimotePlugin folder.
To use, build the project, then navigate to app\build\outputs\aar and rename the .aar file to .zip. Open the .zip file and extract the classes.jar file to your Unity project's Assets\Plugins\Android folder, renaming it as you desire.
You will also need the android-support-v4.jar file in the Assets\Plugins\Android folder. Next, copy your Unity Android project's AndroidManifest.xml file from the Temp\StagingArea folder that Unity generates when you build an Android project. Paste the file in the same folder as your .jar file.
You will need to add several lines to the .xml file:

```
<manifest ...
  ...
  <application
      <activity android:name="com.unity3d.player.UnityPlayerActivity" ...
      ...
      <intent-filter>
        <action android:name="android.bluetooth.device.action.FOUND" />
        <action android:name="android.bluetooth.adapter.action.DISCOVERY_FINISHED" />
        <category android:name="android.intent.category.DEFAULT" />
      </intent-filter>
      <intent-filter>
        <action android:name="android.bluetooth.device.action.PAIRING_REQUEST" />
        <action android:name="android.bluetooth.device.action.BOND_STATE_CHANGED" />
        <category android:name="android.intent.category.DEFAULT" />
      </intent-filter>
    </activity>
  </application>
  ...
  <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
  <uses-permission android:name="android.permission.BLUETOOTH"/>
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
  ...
</manifest>
```

You can then access the Java classes using Unity's Java Helper Classes found here: http://docs.unity3d.com/Manual/PluginsForAndroid.html
See the section labeled: Using Your Java Plugin with helper classes

Class:
- WiimoteDriver
Exposed Methods: 
- public WiimoteDriver(Activity c) //constructor, requires a reference to Unity's Activity
- public void Connect() //discovers, binds, and connects to any wiimote found. Will enable Bluetooth if necessary, but will need to be called again if that is the case.
- public String getStatus() //returns a status log. Reset each time getStatus() is called
- public String getError() //returns an error log. getStatus() will include an "ERROR" line if this is populated. Reset each time getError() is called

##UnityApp
UnityApp is an example app that utilizes WiimotePlugin. It is located in the UnityApp folder.
See Assets\PluginInterface.cs for an example of how to interface with the Java classes.
See the main.unity scene for an example of how PluginInterface.cs can be used.

###NOTE
You cannot deploy Unity projects to an emulator, you must deploy them to a physical device.