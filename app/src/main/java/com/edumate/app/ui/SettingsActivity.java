package com.edumate.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.edumate.app.R;
import com.edumate.app.databinding.ActivitySettingsBinding;
import com.edumate.app.repository.SessionRepository;
import com.edumate.app.repository.SyncRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {
    public static final String PREFS_SETTINGS = "edumate_settings";
    public static final String KEY_AUTO_PLAY_TTS = "auto_play_tts";

    private ActivitySettingsBinding binding;
    private SessionRepository sessionRepository;
    private SyncRepository syncRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionRepository = SessionRepository.getInstance(this);
        syncRepository = SyncRepository.getInstance(this);

        loadPreferences();
        setupClickListeners();
    }

    private void loadPreferences() {
        SharedPreferences sp = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        boolean autoTts = sp.getBoolean(KEY_AUTO_PLAY_TTS, true);
        binding.switchTtsAuto.setChecked(autoTts);

        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                String email = user.getEmail() != null && !user.getEmail().isEmpty()
                        ? user.getEmail() : "Guest Account";
                String name = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                        ? user.getDisplayName() + "\n" : "";
                binding.tvUserId.setText(name + email);
                binding.btnSignOut.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvUserId.setText(syncRepository.getOrCreateUserUid());
                binding.btnSignOut.setVisibility(android.view.View.GONE);
            }
        } catch (Throwable t) {
            binding.tvUserId.setText(syncRepository.getOrCreateUserUid());
            binding.btnSignOut.setVisibility(android.view.View.GONE);
        }
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.switchTtsAuto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences sp = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
            sp.edit().putBoolean(KEY_AUTO_PLAY_TTS, isChecked).apply();
        });

        binding.btnSignOut.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Sign Out", (d, w) -> {
                        try {
                            FirebaseAuth.getInstance().signOut();
                        } catch (Throwable ignored) {}
                        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
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
