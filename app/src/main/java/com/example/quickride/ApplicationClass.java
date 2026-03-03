package com.example.quickride;

import android.app.Application;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class ApplicationClass extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose logging for debugging (remove in production)
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);

        // Initialize OneSignal with App ID from strings.xml
        OneSignal.initWithContext(this, getString(R.string.onesignal_app_id));

        // Prompt user for push notification permission
        OneSignal.getNotifications().requestPermission(false, null);
    }
}