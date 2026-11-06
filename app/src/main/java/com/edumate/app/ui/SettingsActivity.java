package com.edumate.app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.edumate.app.R;
import com.edumate.app.databinding.ActivitySettingsBinding;
import com.edumate.app.repository.AIRepository;
import com.edumate.app.repository.SessionRepository;
import com.edumate.app.repository.SyncRepository;

public class SettingsActivity extends AppCompatActivity {
    public static final String PREFS_SETTINGS = "edumate_settings";
    public static final String KEY_AUTO_PLAY_TTS = "auto_play_tts";

    private ActivitySettingsBinding binding;
    private AIRepository aiRepository;
    private SessionRepository sessionRepository;
    private SyncRepository syncRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        aiRepository = AIRepository.getInstance(this);
        sessionRepository = SessionRepository.getInstance(this);
        syncRepository = SyncRepository.getInstance(this);

        loadPreferences();
        setupClickListeners();
    }

    private void loadPreferences() {
        SharedPreferences sp = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        boolean autoTts = sp.getBoolean(KEY_AUTO_PLAY_TTS, true);
        binding.switchTtsAuto.setChecked(autoTts);

        String currentApiKey = aiRepository.getApiKey();
        if (currentApiKey != null && !currentApiKey.isEmpty()) {
            binding.etApiKey.setText(currentApiKey);
        }

        binding.tvUserId.setText(syncRepository.getOrCreateUserUid());
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.switchTtsAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences sp = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
            sp.edit().putBoolean(KEY_AUTO_PLAY_TTS, isChecked).apply();
        });

        binding.btnSaveApiKey.setOnClickListener(v -> {
            String rawKey = binding.etApiKey.getText() != null
                    ? binding.etApiKey.getText().toString()
                    : "";
            String key = AIRepository.sanitizeApiKey(rawKey);
            binding.etApiKey.setText(key);
            aiRepository.setApiKey(key);

            if (key.isEmpty()) {
                binding.tvApiKeyStatus.setText("API Key cleared. Using adaptive offline AI student.");
                binding.tvApiKeyStatus.setTextColor(getColor(R.color.text_secondary));
                Toast.makeText(this, "API Key cleared. Offline AI student active.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "API Key saved. Testing connection...", Toast.LENGTH_SHORT).show();
                testKeyInternal(key);
            }
        });

        binding.btnTestApiKey.setOnClickListener(v -> {
            String rawKey = binding.etApiKey.getText() != null
                    ? binding.etApiKey.getText().toString()
                    : "";
            String key = AIRepository.sanitizeApiKey(rawKey);
            binding.etApiKey.setText(key);

            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter an API key to test.", Toast.LENGTH_SHORT).show();
                return;
            }
            testKeyInternal(key);
        });

        binding.btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All Local Data")
                    .setMessage("This will permanently delete all session history stored on this device. Continue?")
                    .setPositiveButton("Clear Data", (d, w) -> {
                        sessionRepository.clearAllLocalData(result -> {
                            Toast.makeText(this, "All local history cleared.", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
        });
    }

    private void testKeyInternal(String key) {
        binding.tvApiKeyStatus.setText("Testing connection to Google Gemini API...");
        binding.tvApiKeyStatus.setTextColor(getColor(R.color.primary));
        binding.btnTestApiKey.setEnabled(false);
        binding.btnSaveApiKey.setEnabled(false);

        aiRepository.testApiKey(key, new AIRepository.TestApiKeyCallback() {
            @Override
            public void onSuccess(String modelUsed) {
                binding.btnTestApiKey.setEnabled(true);
                binding.btnSaveApiKey.setEnabled(true);
                binding.tvApiKeyStatus.setText("✓ Connection Verified! Active model: " + modelUsed);
                binding.tvApiKeyStatus.setTextColor(0xFF16A34A);
                Toast.makeText(SettingsActivity.this, "Gemini Connected! (" + modelUsed + ")", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                binding.btnTestApiKey.setEnabled(true);
                binding.btnSaveApiKey.setEnabled(true);

                String shortStatus = errorMessage;
                if (shortStatus != null && shortStatus.contains("\n")) {
                    shortStatus = shortStatus.substring(0, shortStatus.indexOf("\n")).trim();
                }
                binding.tvApiKeyStatus.setText("✗ " + shortStatus);
                binding.tvApiKeyStatus.setTextColor(0xFFDC2626);

                boolean isNetworkIssue = errorMessage != null && (
                        errorMessage.contains("Timed Out")
                                || errorMessage.contains("Internet Connection")
                                || errorMessage.contains("Network")
                                || errorMessage.contains("Connection Refused")
                );

                String dialogTitle = isNetworkIssue ? "Network Connection Issue" : "Gemini API Error";

                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(dialogTitle)
                        .setPositiveButton("OK", null);

                if (errorMessage != null && errorMessage.contains("Troubleshooting:")) {
                    builder.setMessage(errorMessage);
                } else {
                    builder.setMessage(errorMessage + "\n\nTroubleshooting Guide:\n"
                            + "• Verify your key at: https://aistudio.google.com/app/apikey\n"
                            + "• In Google Cloud Console -> Credentials -> API Key:\n"
                            + "  - Set Application Restrictions to 'None'\n"
                            + "  - Ensure 'Generative Language API' is enabled\n"
                            + "• If you don't have a key, leave it empty to use EduMate's adaptive offline student!");
                }
                builder.show();
            }
        });
    }
}
