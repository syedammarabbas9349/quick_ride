package com.example.quickride;

import android.app.Application;

import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose logging for debugging
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // Initialize OneSignal
        OneSignal.initWithContext(this, getString(R.string.onesignal_app_id));

        // Ask permission for notifications
        OneSignal.getNotifications().requestPermission(false, null);
    }
}