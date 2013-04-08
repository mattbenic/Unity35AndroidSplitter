package com.flk.unityLauncher;

import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;
import com.yourdomain.yourapp.R;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Messenger;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * This is sample code for a project built against the downloader library. It
 * implements the IDownloaderClient that the client marshaler will talk to as
 * messages are delivered from the DownloaderService.
 */
public class UnityDownloaderActivity extends Activity implements IDownloaderClient {
	/* The following members need to be set by the app specific downloader subclass by the time onCreate is called  */
	public static int GAME_DATA_VERSION_CODE; // The Google Play version code for the apk the data bundle was uploaded with
	public static int GAME_DATA_SIZE; // The expected size of the data file (-1 will skip size check, but will not handle download failure correctly) 
	public static byte[] DOWNLOAD_SERVICE_SALT; // Used by the preferences obfuscator. Any random array of bytes will do.
	public static String DOWNLOAD_SERVICE_PUBLIC_KEY; // Google Play application public key (same as specified in Unity build settings)
	public static String DOWNLOAD_SERVICE_ALARM_RECEIVER; // Class name of application unique alarm receiver so they're not shared between apps
	
	protected static String GAME_DATA_LOCAL_PATH; // If this is null, it will be generated based on GAME_DATA_VERSION_CODE

	private static final String LOG_TAG = "UnityDownloader";
    private ProgressBar mPB;

    private TextView mStatusText;
    private TextView mProgressFraction;
    private TextView mProgressPercent;
    private TextView mAverageSpeed;
    private TextView mTimeRemaining;

    private View mDashboard;
    private View mCellMessage;

    private Button mPauseButton;
    private Button mWiFiSettingsButton;

    private boolean mStatePaused;
    private int mState;

    private IDownloaderService mRemoteService;

    private IStub mDownloaderClientStub;

    private void setState(int newState) {
        if (mState != newState) {
            mState = newState;
            mStatusText.setText(Helpers.getDownloaderStringResourceIDFromState(newState));
        }
    }

    private void setButtonPausedState(boolean paused) {
        mStatePaused = paused;
        int stringResourceID = paused ? R.string.text_button_resume :
                R.string.text_button_pause;
        mPauseButton.setText(stringResourceID);
    }

    /**
     * If the download isn't present, we initialize the download UI. This ties
     * all of the controls into the remote service calls.
     */
    private void initializeDownloadUI() {
        mDownloaderClientStub = DownloaderClientMarshaller.CreateStub
                (this, UnityDownloaderService.class);
        setContentView(R.layout.main);

        mPB = (ProgressBar) findViewById(R.id.progressBar);
        mStatusText = (TextView) findViewById(R.id.statusText);
        mProgressFraction = (TextView) findViewById(R.id.progressAsFraction);
        mProgressPercent = (TextView) findViewById(R.id.progressAsPercentage);
        mAverageSpeed = (TextView) findViewById(R.id.progressAverageSpeed);
        mTimeRemaining = (TextView) findViewById(R.id.progressTimeRemaining);
        mDashboard = findViewById(R.id.downloaderDashboard);
        mCellMessage = findViewById(R.id.approveCellular);
        mPauseButton = (Button) findViewById(R.id.pauseButton);
        mWiFiSettingsButton = (Button) findViewById(R.id.wifiSettingsButton);

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStatePaused) {
                    mRemoteService.requestContinueDownload();
                } else {
                    mRemoteService.requestPauseDownload();
                }
                setButtonPausedState(!mStatePaused);
            }
        });

        mWiFiSettingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        Button resumeOnCell = (Button) findViewById(R.id.resumeOverCellular);
        resumeOnCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRemoteService.setDownloadFlags(IDownloaderService.FLAGS_DOWNLOAD_OVER_CELLULAR);
                mRemoteService.requestContinueDownload();
                mCellMessage.setVisibility(View.GONE);
            }
        });

    }

    /**
     * Called when the activity is first create; we wouldn't create a layout in
     * the case where we have the file and are moving to another activity
     * without downloading.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Both downloading and validation make use of the "download" UI
         */
        initializeDownloadUI();

        // Set the expected local location of our data file
        GAME_DATA_LOCAL_PATH = Helpers.generateSaveFileName(this, Helpers.getExpansionAPKFileName(this, true, GAME_DATA_VERSION_CODE));
        
        /**
         * Before we do anything, are the files we expect already here and
         * delivered (presumably by Market) For free titles, this is probably
         * worth doing. (so no Market request is necessary)
         */
        if (!Helpers.doesFileExist(this, Helpers.getExpansionAPKFileName(this, true, GAME_DATA_VERSION_CODE), GAME_DATA_SIZE, false)) {

            try {
                Intent launchIntent = UnityDownloaderActivity.this
                        .getIntent();
                Intent intentToLaunchThisActivityFromNotification = new Intent(
                        UnityDownloaderActivity.this, UnityDownloaderActivity.this.getClass());
                intentToLaunchThisActivityFromNotification.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentToLaunchThisActivityFromNotification.setAction(launchIntent.getAction());

                if (launchIntent.getCategories() != null) {
                    for (String category : launchIntent.getCategories()) {
                        intentToLaunchThisActivityFromNotification.addCategory(category);
                    }
                }

                // Build PendingIntent used to open this activity from
                // Notification
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        UnityDownloaderActivity.this,
                        0, intentToLaunchThisActivityFromNotification,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                // Request to start the download
                int startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(this,
                        pendingIntent, UnityDownloaderService.class);

                if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                    // The DownloaderService has started downloading the files,
                    // show progress
                    initializeDownloadUI();
                    return;
                } // otherwise, download not needed so we fall through to
                  // starting the movie
            } catch (NameNotFoundException e) {
                Log.e(LOG_TAG, "Cannot find own package! MAYDAY!");
                e.printStackTrace();
            }

        } else {
            //validateXAPKZipFiles();
        	LaunchUnityActivity();
        }

    }

	/**
	 * Launches the custom unity proxy with the data file set on GAME_DATA_LOCAL_PATH
	 */
	public void LaunchUnityActivity()
	{
		// Close this Installer activity
		Log.d(LOG_TAG, "Closing the Installer activity...");
		finish();
		
		// Start the Unity Activity
		Log.d(LOG_TAG, "Loading game from: " + GAME_DATA_LOCAL_PATH);
		Intent launchIntent = new Intent(getApplicationContext(), CustomUnityPlayerProxyActivity.class);
		startActivity(launchIntent);
	}
    
    /**
     * Connect the stub to our service on start.
     */
    @Override
    protected void onStart() {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.connect(this);
        }
        super.onStart();
    }

    /**
     * Disconnect the stub from our service on stop
     */
    @Override
    protected void onStop() {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.disconnect(this);
        }
        super.onStop();
    }

    /**
     * Critical implementation detail. In onServiceConnected we create the
     * remote service and marshaler. This is how we pass the client information
     * back to the service so the client can be properly notified of changes. We
     * must do this every time we reconnect to the service.
     */
    @Override
    public void onServiceConnected(Messenger m) {
        mRemoteService = DownloaderServiceMarshaller.CreateProxy(m);
        mRemoteService.onClientUpdated(mDownloaderClientStub.getMessenger());
    }

    /**
     * The download state should trigger changes in the UI --- it may be useful
     * to show the state as being indeterminate at times. This sample can be
     * considered a guideline.
     */
    @Override
    public void onDownloadStateChanged(int newState) {
        setState(newState);
        boolean showDashboard = true;
        boolean showCellMessage = false;
        boolean paused;
        boolean indeterminate;
        switch (newState) {
            case IDownloaderClient.STATE_IDLE:
                // STATE_IDLE means the service is listening, so it's
                // safe to start making calls via mRemoteService.
                paused = false;
                indeterminate = true;
                break;
            case IDownloaderClient.STATE_CONNECTING:
            case IDownloaderClient.STATE_FETCHING_URL:
                showDashboard = true;
                paused = false;
                indeterminate = true;
                break;
            case IDownloaderClient.STATE_DOWNLOADING:
                paused = false;
                showDashboard = true;
                indeterminate = false;
                break;

            case IDownloaderClient.STATE_FAILED_CANCELED:
            case IDownloaderClient.STATE_FAILED:
            case IDownloaderClient.STATE_FAILED_FETCHING_URL:
            case IDownloaderClient.STATE_FAILED_UNLICENSED:
                paused = true;
                showDashboard = false;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_PAUSED_NEED_CELLULAR_PERMISSION:
            case IDownloaderClient.STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION:
                showDashboard = false;
                paused = true;
                indeterminate = false;
                showCellMessage = true;
                break;

            case IDownloaderClient.STATE_PAUSED_BY_REQUEST:
                paused = true;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_PAUSED_ROAMING:
            case IDownloaderClient.STATE_PAUSED_SDCARD_UNAVAILABLE:
                paused = true;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_COMPLETED:
                showDashboard = false;
                paused = false;
                indeterminate = false;
                //validateXAPKZipFiles();
                LaunchUnityActivity();
                return;
            default:
                paused = true;
                indeterminate = true;
                showDashboard = true;
        }
        int newDashboardVisibility = showDashboard ? View.VISIBLE : View.GONE;
        if (mDashboard.getVisibility() != newDashboardVisibility) {
            mDashboard.setVisibility(newDashboardVisibility);
        }
        int cellMessageVisibility = showCellMessage ? View.VISIBLE : View.GONE;
        if (mCellMessage.getVisibility() != cellMessageVisibility) {
            mCellMessage.setVisibility(cellMessageVisibility);
        }

        mPB.setIndeterminate(indeterminate);
        setButtonPausedState(paused);
    }

    /**
     * Sets the state of the various controls based on the progressinfo object
     * sent from the downloader service.
     */
    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {
        mAverageSpeed.setText(getString(R.string.kilobytes_per_second,
                Helpers.getSpeedString(progress.mCurrentSpeed)));
        mTimeRemaining.setText(getString(R.string.time_remaining,
                Helpers.getTimeRemaining(progress.mTimeRemaining)));

        progress.mOverallTotal = progress.mOverallTotal;
        mPB.setMax((int) (progress.mOverallTotal >> 8));
        mPB.setProgress((int) (progress.mOverallProgress >> 8));
        mProgressPercent.setText(Long.toString(progress.mOverallProgress
                * 100 /
                progress.mOverallTotal) + "%");
        mProgressFraction.setText(Helpers.getDownloadProgressString
                (progress.mOverallProgress,
                        progress.mOverallTotal));
    }
}