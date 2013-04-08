package com.flk.unityLauncher;

import com.unity3d.player.UnityPlayerNativeActivity;

// This class extends the Unity default activity and just overrides the path where Unity will search for it's native libs and resources
public class UnityLauncherNativeActivity extends UnityPlayerNativeActivity { 
	
	@Override
	public String getPackageCodePath() {	
		return UnityDownloaderActivity.GAME_DATA_LOCAL_PATH;
	}
}