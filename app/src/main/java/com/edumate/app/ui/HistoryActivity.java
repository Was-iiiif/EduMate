package com.edumate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.edumate.app.R;
import com.edumate.app.adapter.HistoryAdapter;
import com.edumate.app.databinding.ActivityHistoryBinding;
import com.edumate.app.model.Session;
import com.edumate.app.viewmodel.HistoryViewModel;

public class HistoryActivity extends AppCompatActivity {
    private ActivityHistoryBinding binding;
    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(new HistoryAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(Session session) {
                if (Session.STATUS_COMPLETED.equals(session.getStatus())) {
                    Intent intent = new Intent(HistoryActivity.this, FeedbackActivity.class);
                    intent.putExtra(FeedbackActivity.EXTRA_SESSION_ID, session.getId());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(HistoryActivity.this, SessionActivity.class);
                    intent.putExtra(SessionActivity.EXTRA_SESSION_ID, session.getId());
                    intent.putExtra(SessionActivity.EXTRA_TOPIC, session.getTopic());
                    startActivity(intent);
                }
            }

            @Override
            public void onDeleteClick(Session session) {
                new AlertDialog.Builder(HistoryActivity.this)
                        .setTitle(R.string.dialog_delete_title)
                        .setMessage(R.string.dialog_delete_message)
                        .setPositiveButton(R.string.action_delete, (d, w) -> {
                            viewModel.softDeleteSession(session.getId());
                            Toast.makeText(HistoryActivity.this, "Session removed.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }
        });

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSync.setOnClickListener(v -> {
            viewModel.syncNow();
        });

        binding.etSearchFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                viewModel.setSearchQuery(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getFilteredSessions().observe(this, sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                binding.tvEmptyHistory.setVisibility(View.VISIBLE);
                binding.rvHistory.setVisibility(View.GONE);
                adapter.setSessions(null);
            } else {
                binding.tvEmptyHistory.setVisibility(View.GONE);
                binding.rvHistory.setVisibility(View.VISIBLE);
                adapter.setSessions(sessions);
            }
        });

        viewModel.getIsSyncing().observe(this, syncing -> {
            binding.pbSync.setVisibility(syncing ? View.VISIBLE : View.GONE);
            binding.btnSync.setEnabled(!syncing);
        });

        viewModel.getSyncStatusMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
