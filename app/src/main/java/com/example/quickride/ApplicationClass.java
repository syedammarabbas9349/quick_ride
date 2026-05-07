package com.example.quickride;

import android.app.Application;
import com.example.quickride.R;
import com.example.quickride.utils.VoiceHelper;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class ApplicationClass extends Application {


    public static VoiceHelper voiceHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose logging for debugging
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // Initialize OneSignal
        OneSignal.initWithContext(this, getString(R.string.onesignal_app_id));

        // Initialize Global Voice Helper
        voiceHelper = new VoiceHelper(this);
    }
}