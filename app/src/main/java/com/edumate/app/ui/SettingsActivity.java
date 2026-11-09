package com.edumate.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.edumate.app.R;
import com.edumate.app.databinding.ActivitySettingsBinding;
import com.edumate.app.repository.SessionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {
    public static final String PREFS_SETTINGS = "edumate_settings";
    public static final String KEY_AUTO_PLAY_TTS = "auto_play_tts";
    public static final String KEY_LEARNING_LEVEL = "learning_level";
    public static final String KEY_EXPLANATION_STYLE = "explanation_style";
    public static final String KEY_LANGUAGE = "learning_language";
    public static final String KEY_FOLLOW_UP = "follow_up_questions";
    public static final String KEY_THEME = "app_theme";

    private ActivitySettingsBinding binding;
    private SessionRepository sessionRepository;
    private SharedPreferences preferences;
    private boolean loadingPreferences = true;

    private final String[] levels = {"Beginner", "Intermediate", "Advanced"};
    private final String[] explanationStyles = {"Concise", "Balanced", "Detailed"};
    private final String[] languages = {"English", "Bangla"};
    private final String[] themes = {"System default", "Light", "Dark"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferences = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        sessionRepository = SessionRepository.getInstance(this);

        setupSpinners();
        loadPreferences();
        loadAccountDetails();
        loadAppVersion();
        setupClickListeners();
        loadingPreferences = false;
    }

    private void setupSpinners() {
        setSpinnerAdapter(binding.spinnerLearningLevel, levels);
        setSpinnerAdapter(binding.spinnerExplanationStyle, explanationStyles);
        setSpinnerAdapter(binding.spinnerLanguage, languages);
        setSpinnerAdapter(binding.spinnerTheme, themes);
    }

    private void setSpinnerAdapter(android.widget.Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void loadPreferences() {
        binding.switchTtsAuto.setChecked(preferences.getBoolean(KEY_AUTO_PLAY_TTS, true));
        binding.switchFollowUp.setChecked(preferences.getBoolean(KEY_FOLLOW_UP, true));
        selectValue(binding.spinnerLearningLevel, levels,
                preferences.getString(KEY_LEARNING_LEVEL, "Intermediate"));
        selectValue(binding.spinnerExplanationStyle, explanationStyles,
                preferences.getString(KEY_EXPLANATION_STYLE, "Balanced"));
        selectValue(binding.spinnerLanguage, languages,
                preferences.getString(KEY_LANGUAGE, "English"));
        selectValue(binding.spinnerTheme, themes,
                preferences.getString(KEY_THEME, "System default"));
    }

    private void selectValue(android.widget.Spinner spinner, String[] values, String selected) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(selected)) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(0);
    }

    private void loadAccountDetails() {
        FirebaseUser user = null;
        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
        } catch (Exception ignored) { }

        if (user == null) {
            binding.tvProfileName.setText("Local profile");
            binding.tvProfileEmail.setText("No active Firebase account");
            binding.tvAccountType.setText("Local mode");
            binding.btnSignOut.setVisibility(View.GONE);
            return;
        }

        String displayName = user.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = user.isAnonymous() ? "Guest learner" : "EduMate learner";
        }
        binding.tvProfileName.setText(displayName);
        binding.tvProfileEmail.setText(user.getEmail() == null
                ? "Email not linked" : user.getEmail());
        binding.tvAccountType.setText(user.isAnonymous()
                ? "Guest account · data may not follow you to another device"
                : "Signed-in account");
        binding.btnSignOut.setVisibility(View.VISIBLE);
    }

    private void loadAppVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = info.versionName == null ? "Unknown" : info.versionName;
            binding.tvVersion.setText("Version " + version);
        } catch (Exception e) {
            binding.tvVersion.setText("Version unavailable");
        }
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.switchTtsAuto.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean(KEY_AUTO_PLAY_TTS, checked).apply());
        binding.switchFollowUp.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean(KEY_FOLLOW_UP, checked).apply());

        binding.spinnerLearningLevel.setOnItemSelectedListener(simpleListener(
                value -> preferences.edit().putString(KEY_LEARNING_LEVEL, value).apply()));
        binding.spinnerExplanationStyle.setOnItemSelectedListener(simpleListener(
                value -> preferences.edit().putString(KEY_EXPLANATION_STYLE, value).apply()));
        binding.spinnerLanguage.setOnItemSelectedListener(simpleListener(
                value -> preferences.edit().putString(KEY_LANGUAGE, value).apply()));
        binding.spinnerTheme.setOnItemSelectedListener(simpleListener(value -> {
            preferences.edit().putString(KEY_THEME, value).apply();
            applyTheme(value);
        }));

        binding.btnSignOut.setOnClickListener(v -> confirmSignOut());
        binding.btnClearData.setOnClickListener(v -> confirmClearLocalData());
        binding.rowPrivacy.setOnClickListener(v -> showPrivacyDialog());
        binding.rowAiService.setOnClickListener(v -> showAiServiceDialog());
        binding.rowHelp.setOnClickListener(v -> showHelpDialog());
        binding.rowFeedback.setOnClickListener(v -> openFeedbackComposer());
        binding.rowAbout.setOnClickListener(v -> showAboutDialog());
    }

    private android.widget.AdapterView.OnItemSelectedListener simpleListener(
            java.util.function.Consumer<String> onSelected) {
        return new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                    View view, int position, long id) {
                if (!loadingPreferences) onSelected.accept(String.valueOf(parent.getItemAtPosition(position)));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        };
    }

    private void applyTheme(String theme) {
        int mode;
        switch (theme) {
            case "Light": mode = AppCompatDelegate.MODE_NIGHT_NO; break;
            case "Dark": mode = AppCompatDelegate.MODE_NIGHT_YES; break;
            default: mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign out?")
                .setMessage("You will need to sign in again to access this account.")
                .setPositiveButton("Sign out", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmClearLocalData() {
        new AlertDialog.Builder(this)
                .setTitle("Clear local learning history?")
                .setMessage("This permanently removes session history stored on this device. It does not delete any cloud-synced records.")
                .setPositiveButton("Clear local history", (dialog, which) ->
                        sessionRepository.clearAllLocalData(result ->
                                runOnUiThread(() -> Toast.makeText(this,
                                        "Local learning history cleared.", Toast.LENGTH_SHORT).show())))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }


    private void openFeedbackComposer() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, "EduMate feedback");
        intent.putExtra(Intent.EXTRA_TEXT,
                "EduMate version: " + binding.tvVersion.getText() + "\\n\\n"
                        + "Feedback / issue description:\\n");
        try {
            startActivity(Intent.createChooser(intent, "Send EduMate feedback"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app is available on this device.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrivacyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Privacy & learning data")
                .setMessage("When you use AI learning features, the topic and conversation content needed to answer your request is sent from EduMate to its Cloudflare Worker and then to Google Gemini. Avoid entering passwords, personal identifiers, or other sensitive information. Clearing local history does not delete cloud-synced records. Account and cloud-data deletion are not provided by this screen.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showAiServiceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("EduMate AI service")
                .setMessage("EduMate sends AI requests through its secured Cloudflare Worker, which forwards them to Google Gemini. Model selection and API credentials are managed on the service side; they are not stored as an API key in this app. Availability depends on your internet connection and service quotas.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Help & support")
                .setMessage("Start a teaching session, explain the selected topic in your own words, and review the feedback after evaluation. Use the Feedback section to share comments or report an issue.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About EduMate")
                .setMessage("EduMate is an AI-assisted learning companion built around learning by teaching. Explain concepts, respond to AI student questions, and reflect on rubric-based feedback. AI responses are generated by Google Gemini and may occasionally be inaccurate.")
                .setPositiveButton("Close", null)
                .show();
    }
}
