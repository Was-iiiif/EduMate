package com.edumate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.edumate.app.R;
import com.edumate.app.adapter.HistoryAdapter;
import com.edumate.app.databinding.ActivityMainBinding;
import com.edumate.app.model.Session;
import com.edumate.app.viewmodel.HomeViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private HomeViewModel viewModel;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Auth guard: redirect to LoginActivity if no Firebase user is signed in
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
        } catch (Throwable ignored) {}

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Auth guard: redirect to login if session was revoked or user signed out
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
        } catch (Throwable ignored) {}
        viewModel.loadHomeData();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(new HistoryAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(Session session) {
                if (Session.STATUS_COMPLETED.equals(session.getStatus())) {
                    Intent intent = new Intent(MainActivity.this, FeedbackActivity.class);
                    intent.putExtra(FeedbackActivity.EXTRA_SESSION_ID, session.getId());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(MainActivity.this, SessionActivity.class);
                    intent.putExtra(SessionActivity.EXTRA_SESSION_ID, session.getId());
                    intent.putExtra(SessionActivity.EXTRA_TOPIC, session.getTopic());
                    startActivity(intent);
                }
            }

            @Override
            public void onDeleteClick(Session session) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.dialog_delete_title)
                        .setMessage(R.string.dialog_delete_message)
                        .setPositiveButton(R.string.action_delete, (d, w) -> {
                            viewModel.softDeleteSession(session.getId(), () -> {
                                Toast.makeText(MainActivity.this, R.string.session_deleted, Toast.LENGTH_SHORT).show();
                            });
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }
        });

        binding.rvRecentSessions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentSessions.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnStartTeaching.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TopicSelectionActivity.class);
            startActivity(intent);
        });

        binding.btnAllHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        binding.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        binding.btnSyncCloud.setOnClickListener(v -> {
            viewModel.syncNow();
        });
    }

    private void observeViewModel() {
        viewModel.getStatistics().observe(this, stats -> {
            if (stats != null) {
                binding.tvStatCount.setText(String.valueOf(stats.completedSessions));
                if (stats.completedSessions > 0) {
                    binding.tvStatAverage.setText(String.format("%.1f", stats.averageScore));
                } else {
                    binding.tvStatAverage.setText("-- / 100");
                }
            }
        });

        viewModel.getRecentSessions().observe(this, sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                binding.tvNoSessions.setVisibility(View.VISIBLE);
                binding.rvRecentSessions.setVisibility(View.GONE);
                adapter.setSessions(null);
            } else {
                binding.tvNoSessions.setVisibility(View.GONE);
                binding.rvRecentSessions.setVisibility(View.VISIBLE);
                adapter.setSessions(sessions);
            }
        });

        viewModel.getIsSyncing().observe(this, isSyncing -> {
            binding.pbSyncing.setVisibility(isSyncing ? View.VISIBLE : View.GONE);
            binding.btnSyncCloud.setEnabled(!isSyncing);
        });

        viewModel.getSyncMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
