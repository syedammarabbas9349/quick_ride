package com.example.quickride.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Helper class for Text-To-Speech (TTS) functionality.
 */
public class VoiceHelper {
    private static final String TAG = "VoiceHelper";
    private TextToSpeech tts;
    private boolean isInitialized = false;

    public VoiceHelper(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported or missing data");
                } else {
                    isInitialized = true;
                    Log.d(TAG, "TTS Initialized successfully");
                }
            } else {
                Log.e(TAG, "TTS Initialization failed");
            }
        });
    }

    public void speak(String text) {
        if (isInitialized && tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Log.w(TAG, "TTS not initialized yet. Skipping speech: " + text);
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
