package com.flk.unityLauncher;

import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * You should start your derived downloader class when this receiver gets the message 
 * from the alarm service using the provided service helper function within the
 * DownloaderClientMarshaller. This class must be then registered in your AndroidManifest.xml
 * file with a section like this:
 *         <receiver android:name=".GameSpecificAlarmReceiver"/>
 * This class is abstract only because receivers need to be unique across android. 
 * Just add a non-abstract subclass to your game specific package and set it's name on UnityDownloaderActivity.DOWNLOAD_SERVICE_ALARM_RECEIVER
 */
public abstract class UnityAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            DownloaderClientMarshaller.startDownloadServiceIfRequired(context, intent, UnityDownloaderService.class);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }       
    }

}
