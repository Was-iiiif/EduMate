package com.edumate.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.edumate.app.R;
import com.edumate.app.adapter.ChatAdapter;
import com.edumate.app.databinding.ActivitySessionBinding;
import com.edumate.app.viewmodel.SessionViewModel;
import com.edumate.app.voice.VoiceController;

import java.util.Locale;

public class SessionActivity extends AppCompatActivity {
    public static final String EXTRA_TOPIC = "extra_topic";
    public static final String EXTRA_SESSION_ID = "extra_session_id";

    private ActivitySessionBinding binding;
    private SessionViewModel viewModel;
    private ChatAdapter chatAdapter;
    private VoiceController voiceController;
    private boolean autoPlayTts = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SessionViewModel.class);

        readSettings();
        initVoiceController();
        setupRecyclerView();
        setupClickListeners();
        observeViewModel();

        String topic = getIntent().getStringExtra(EXTRA_TOPIC);
        String existingSessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);

        viewModel.initSession(topic != null ? topic : "General Topic", existingSessionId);
    }

    private void readSettings() {
        SharedPreferences sp = getSharedPreferences(SettingsActivity.PREFS_SETTINGS, Context.MODE_PRIVATE);
        autoPlayTts = sp.getBoolean(SettingsActivity.KEY_AUTO_PLAY_TTS, true);
    }

    private void initVoiceController() {
        voiceController = new VoiceController(this, new VoiceController.VoiceCallback() {
            @Override
            public void onSpeechRecognized(String text) {
                // Return text to input box for learner to review and edit as specified
                binding.etMessageInput.setText(text);
                binding.etMessageInput.setSelection(text.length());
                Toast.makeText(SessionActivity.this, "Speech captured. Review and tap Send.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSpeechRecognitionStarted() {
                binding.layoutStatusBanner.setVisibility(View.VISIBLE);
                binding.tvStatusText.setText(R.string.listening_prompt);
                binding.pbStatus.setVisibility(View.VISIBLE);
                binding.btnStopAudio.setVisibility(View.GONE);
            }

            @Override
            public void onSpeechRecognitionEnded() {
                if (!Boolean.TRUE.equals(viewModel.getIsLoading().getValue())
                        && !Boolean.TRUE.equals(viewModel.getIsEvaluating().getValue())) {
                    binding.layoutStatusBanner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSpeechError(String errorMessage) {
                Toast.makeText(SessionActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                if (!Boolean.TRUE.equals(viewModel.getIsLoading().getValue())) {
                    binding.layoutStatusBanner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTtsStateChanged(boolean isSpeaking) {
                viewModel.setTtsSpeaking(isSpeaking);
                if (isSpeaking) {
                    binding.layoutStatusBanner.setVisibility(View.VISIBLE);
                    binding.tvStatusText.setText("AI Student is speaking...");
                    binding.pbStatus.setVisibility(View.GONE);
                    binding.btnStopAudio.setVisibility(View.VISIBLE);
                } else {
                    binding.btnStopAudio.setVisibility(View.GONE);
                    if (!Boolean.TRUE.equals(viewModel.getIsLoading().getValue())
                            && !Boolean.TRUE.equals(viewModel.getIsEvaluating().getValue())) {
                        binding.layoutStatusBanner.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(text -> {
            voiceController.speakText(text);
        });

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(lm);
        binding.rvMessages.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSendMessage.setOnClickListener(v -> {
            String text = binding.etMessageInput.getText() != null
                    ? binding.etMessageInput.getText().toString().trim()
                    : "";
            if (!text.isEmpty()) {
                binding.etMessageInput.setText("");
                viewModel.sendLearnerExplanation(text);
            }
        });

        binding.btnMic.setOnClickListener(v -> {
            if (voiceController.checkAudioPermission()) {
                voiceController.startListening();
            } else {
                voiceController.requestAudioPermission();
            }
        });

        binding.btnStopAudio.setOnClickListener(v -> {
            voiceController.stopSpeaking();
        });

        binding.tvAiModeBadge.setOnClickListener(v -> {
            Intent intent = new Intent(SessionActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.btnFinishEarly.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_finish_title)
                    .setMessage(R.string.confirm_finish_message)
                    .setPositiveButton(R.string.action_finish, (dialog, which) -> {
                        viewModel.evaluateSession();
                    })
                    .setNegativeButton(R.string.action_continue, null)
                    .show();
        });
    }

    private void observeViewModel() {
        viewModel.getSession().observe(this, session -> {
            if (session != null) {
                binding.tvSessionTopic.setText(session.getTopic());
            }
        });

        viewModel.getMessages().observe(this, messages -> {
            chatAdapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                binding.rvMessages.smoothScrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getCurrentTurn().observe(this, turn -> {
            binding.tvTurnIndicator.setText(getString(R.string.session_turn_format, turn, SessionViewModel.MAX_TURNS));
            binding.progressTurns.setProgress(turn);
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (loading) {
                binding.layoutStatusBanner.setVisibility(View.VISIBLE);
                binding.tvStatusText.setText(R.string.ai_student_typing);
                binding.pbStatus.setVisibility(View.VISIBLE);
                binding.btnStopAudio.setVisibility(View.GONE);
                binding.btnSendMessage.setEnabled(false);
            } else if (!Boolean.TRUE.equals(viewModel.getIsEvaluating().getValue())
                    && !Boolean.TRUE.equals(viewModel.getIsTtsSpeaking().getValue())) {
                binding.layoutStatusBanner.setVisibility(View.GONE);
                binding.btnSendMessage.setEnabled(true);
            }
        });

        viewModel.getIsEvaluating().observe(this, evaluating -> {
            if (evaluating) {
                binding.layoutStatusBanner.setVisibility(View.VISIBLE);
                binding.tvStatusText.setText(R.string.evaluating_progress);
                binding.pbStatus.setVisibility(View.VISIBLE);
                binding.btnStopAudio.setVisibility(View.GONE);
                binding.btnSendMessage.setEnabled(false);
                binding.etMessageInput.setEnabled(false);
            }
        });

        viewModel.getLatestStudentQuestion().observe(this, question -> {
            if (autoPlayTts && question != null && !question.isEmpty()) {
                voiceController.speakText(question);
            }
        });

        viewModel.getEvaluationResult().observe(this, evaluation -> {
            if (evaluation != null && viewModel.getSession().getValue() != null) {
                Intent intent = new Intent(SessionActivity.this, FeedbackActivity.class);
                intent.putExtra(FeedbackActivity.EXTRA_SESSION_ID, viewModel.getSession().getValue().getId());
                startActivity(intent);
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getAiEngineStatus().observe(this, status -> {
            if (status != null && binding.tvAiModeBadge != null) {
                binding.tvAiModeBadge.setText(status);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        readSettings();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == VoiceController.REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                voiceController.startListening();
            } else {
                Toast.makeText(this, R.string.permission_audio_rationale, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (voiceController != null) {
            voiceController.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceController != null) {
            voiceController.onDestroy();
        }
    }
}
