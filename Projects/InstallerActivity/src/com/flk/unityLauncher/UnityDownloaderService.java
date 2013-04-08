package com.flk.unityLauncher;

import com.google.android.vending.expansion.downloader.impl.DownloaderService;

/**
 * This class demonstrates the minimal client implementation of the
 * DownloaderService from the Downloader library.
 */
public class UnityDownloaderService extends DownloaderService {
    /**
     * This public key comes from your Android Market publisher account, and it
     * used by the LVL to validate responses from Market on your behalf.
     */
    @Override
    public String getPublicKey() {
        return UnityDownloaderActivity.DOWNLOAD_SERVICE_PUBLIC_KEY;
    }

    /**
     * This is used by the preference obfuscater to make sure that your
     * obfuscated preferences are different than the ones used by other
     * applications.
     */
    @Override
    public byte[] getSALT() {
        return UnityDownloaderActivity.DOWNLOAD_SERVICE_SALT;
    }

    /**
     * Fill this in with the class name for your alarm receiver. We do this
     * because receivers must be unique across all of Android (it's a good idea
     * to make sure that your receiver is in your unique package)
     */
    @Override
    public String getAlarmReceiverClassName() {
        return UnityDownloaderActivity.DOWNLOAD_SERVICE_ALARM_RECEIVER;
    }
}
