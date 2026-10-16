package com.edumate.app.voice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceController {
    public static final int REQUEST_RECORD_AUDIO_PERMISSION = 2001;

    public interface VoiceCallback {
        void onSpeechRecognized(String text);
        void onSpeechRecognitionStarted();
        void onSpeechRecognitionEnded();
        void onSpeechError(String errorMessage);
        void onTtsStateChanged(boolean isSpeaking);
    }

    private final Activity activity;
    private final VoiceCallback callback;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isTtsInitialized = false;
    private boolean isListening = false;
    private Locale currentLocale = Locale.US;

    public VoiceController(Activity activity, VoiceCallback callback) {
        this.activity = activity;
        this.callback = callback;
        initTts();
    }

    public void setLocale(Locale locale) {
        this.currentLocale = locale != null ? locale : Locale.US;
        if (textToSpeech != null && isTtsInitialized) {
            textToSpeech.setLanguage(currentLocale);
        }
    }

    private void initTts() {
        textToSpeech = new TextToSpeech(activity.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(currentLocale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.US);
                }
                isTtsInitialized = true;
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        activity.runOnUiThread(() -> {
                            if (callback != null) callback.onTtsStateChanged(true);
                        });
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        activity.runOnUiThread(() -> {
                            if (callback != null) callback.onTtsStateChanged(false);
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        activity.runOnUiThread(() -> {
                            if (callback != null) callback.onTtsStateChanged(false);
                        });
                    }
                });
            } else {
                isTtsInitialized = false;
            }
        });
    }

    public boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(activity,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestAudioPermission() {
        ActivityCompat.requestPermissions(activity,
                new String[]{android.Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

    public void startListening() {
        if (!checkAudioPermission()) {
            requestAudioPermission();
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            if (callback != null) {
                callback.onSpeechError("Speech recognition is not available on this device.");
            }
            return;
        }

        stopSpeaking();
        destroySpeechRecognizer();

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                    if (callback != null) callback.onSpeechRecognitionStarted();
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                    if (callback != null) callback.onSpeechRecognitionEnded();
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    String message;
                    switch (error) {
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            message = "No speech detected. Please try speaking again.";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            message = "Speech timed out.";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            message = "Network error during speech recognition.";
                            break;
                        case SpeechRecognizer.ERROR_AUDIO:
                            message = "Audio recording error.";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            message = "Speech recognition stopped.";
                            break;
                        default:
                            message = "Speech error code: " + error;
                            break;
                    }
                    if (callback != null) {
                        callback.onSpeechRecognitionEnded();
                        callback.onSpeechError(message);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    if (callback != null) callback.onSpeechRecognitionEnded();

                    ArrayList<String> matches = results != null
                            ? results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            : null;
                    if (matches != null && !matches.isEmpty()) {
                        String recognizedText = matches.get(0);
                        if (callback != null) {
                            callback.onSpeechRecognized(recognizedText);
                        }
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLocale.toLanguageTag());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Explain the concept aloud...");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            isListening = false;
            if (callback != null) {
                callback.onSpeechError("Unable to start speech recognition: " + e.getMessage());
            }
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            if (callback != null) callback.onSpeechRecognitionEnded();
        }
    }

    public void speakText(String text) {
        if (textToSpeech != null && isTtsInitialized && text != null && !text.trim().isEmpty()) {
            stopSpeaking();
            String utteranceId = "utterance_" + System.currentTimeMillis();
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }

    public void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            if (callback != null) callback.onTtsStateChanged(false);
        }
    }

    public boolean isSpeaking() {
        return textToSpeech != null && textToSpeech.isSpeaking();
    }

    public boolean isListening() {
        return isListening;
    }

    public void onPause() {
        stopListening();
        stopSpeaking();
    }

    public void onDestroy() {
        onPause();
        destroySpeechRecognizer();
        if (textToSpeech != null) {
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private void destroySpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
