package com.example.quickride.utils;

import android.util.Log;
import com.example.quickride.ApplicationClass;
import com.onesignal.notifications.INotificationReceivedEvent;
import com.onesignal.notifications.INotificationServiceExtension;
import org.json.JSONObject;

/**
 * Custom Notification Service Extension for OneSignal v5.
 * Handles background voice alerts when notifications are received.
 */
public class MyNotificationServiceExtension implements INotificationServiceExtension {
    private static final String TAG = "OneSignalExtension";

    @Override
    public void onNotificationReceived(INotificationReceivedEvent event) {
        Log.d(TAG, "Notification received in background: " + event.getNotification().getBody());

        // Extract additional data
        JSONObject data = event.getNotification().getAdditionalData();
        if (data != null) {
            String type = data.optString("type", "");
            Log.d(TAG, "Notification type: " + type);

            if (ApplicationClass.voiceHelper != null) {
                switch (type) {
                    case "ride_request":
                        ApplicationClass.voiceHelper.speak("New ride request received");
                        break;
                    case "driver_arrived":
                        ApplicationClass.voiceHelper.speak("Your driver has arrived and is waiting outside");
                        break;
                    case "ride_accepted":
                        String driverName = data.optString("driverName", "A driver");
                        ApplicationClass.voiceHelper.speak(driverName + " has accepted your ride request");
                        break;
                    case "ride_completed":
                        ApplicationClass.voiceHelper.speak("Your ride is complete. Thank you for riding with QuickRide.");
                        break;
                }
            }
        }
    }
}
