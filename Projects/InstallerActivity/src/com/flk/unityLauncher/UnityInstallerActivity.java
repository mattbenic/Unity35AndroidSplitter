package com.flk.unityLauncher;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public abstract class UnityInstallerActivity extends Activity {
	// This member will be initialized by the installer with the external
	// storage path where the game data file will be downloaded
	public static String GAME_DATA_LOCAL_PATH;
	
	/**
	 * Launches the custom unity proxy with the data file set on GAME_DATA_LOCAL_PATH
	 */
	public void LaunchUnityActivity()
	{
		// Close this Installer activity
		Log.d("Installer", "Closing the Installer activity...");
		finish();
		
		// Start the Unity Activity
		Log.d("Installer", "Loading game from: " + GAME_DATA_LOCAL_PATH);
		Intent launchIntent = new Intent(getApplicationContext(), CustomUnityPlayerProxyActivity.class);
		startActivity(launchIntent);
	}
}
