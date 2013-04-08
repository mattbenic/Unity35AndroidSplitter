package com.yourdomain.yourapp;

import android.os.Bundle;

import com.flk.unityLauncher.UnityDownloaderActivity;

/**
 * Game specific downloader activity.
 */
public class YourAppDownloaderActivity extends UnityDownloaderActivity {

    /**
     * Called when the activity is created, we just use it to set up required info for the superclass 
     */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		GAME_DATA_VERSION_CODE = 1; // Version code you will be uploading to Google Play, must match manifest
		GAME_DATA_SIZE = -1; // Size in bytes of your data/obb file. -1 will skip size check, but knowing size is safer
		DOWNLOAD_SERVICE_SALT = new byte[] { // Random salt, change this so it doesn't match anyone else's
	            15, 11, -13, -21, 56, 29, -121, -11, 127, 21, -18, -43, 59, 65, -16, -124, -13, 15, -21, 1
	    };
		DOWNLOAD_SERVICE_PUBLIC_KEY = "YOUR PUBLIC KEY FROM GOOGLE PLAY";
		DOWNLOAD_SERVICE_ALARM_RECEIVER = YourAppAlarmReceiver.class.getName();
		
		super.onCreate(savedInstanceState);
    }	
	
}
