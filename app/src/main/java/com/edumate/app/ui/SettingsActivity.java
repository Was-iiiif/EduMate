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
            String key = binding.etApiKey.getText() != null
                    ? binding.etApiKey.getText().toString().trim()
                    : "";
            aiRepository.setApiKey(key);
            Toast.makeText(this, "API Key saved successfully.", Toast.LENGTH_SHORT).show();
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
}
