package com.edumate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.edumate.app.R;
import com.edumate.app.adapter.BulletListAdapter;
import com.edumate.app.adapter.MisconceptionAdapter;
import com.edumate.app.databinding.ActivityFeedbackBinding;
import com.edumate.app.model.Evaluation;
import com.edumate.app.model.Session;
import com.edumate.app.viewmodel.FeedbackViewModel;

public class FeedbackActivity extends AppCompatActivity {
    public static final String EXTRA_SESSION_ID = "extra_session_id";

    private ActivityFeedbackBinding binding;
    private FeedbackViewModel viewModel;
    private BulletListAdapter strengthsAdapter;
    private BulletListAdapter improvementsAdapter;
    private MisconceptionAdapter misconceptionAdapter;
    private String currentSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(FeedbackViewModel.class);

        currentSessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);

        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();

        if (currentSessionId != null && !currentSessionId.isEmpty()) {
            viewModel.loadSessionAndEvaluation(currentSessionId);
        } else {
            Toast.makeText(this, "Session ID missing.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerViews() {
        strengthsAdapter = new BulletListAdapter(R.color.score_excellent);
        binding.rvStrengths.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStrengths.setAdapter(strengthsAdapter);

        improvementsAdapter = new BulletListAdapter(R.color.score_average);
        binding.rvImprovements.setLayoutManager(new LinearLayoutManager(this));
        binding.rvImprovements.setAdapter(improvementsAdapter);

        misconceptionAdapter = new MisconceptionAdapter();
        binding.rvMisconceptions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMisconceptions.setAdapter(misconceptionAdapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(FeedbackActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        binding.btnReviewChat.setOnClickListener(v -> {
            if (currentSessionId != null) {
                Intent intent = new Intent(FeedbackActivity.this, SessionActivity.class);
                intent.putExtra(SessionActivity.EXTRA_SESSION_ID, currentSessionId);
                startActivity(intent);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getSession().observe(this, session -> {
            if (session != null) {
                binding.tvEvaluatedTopic.setText(session.getTopic());
            }
        });

        viewModel.getEvaluation().observe(this, this::bindEvaluationData);

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindEvaluationData(Evaluation eval) {
        if (eval == null) return;

        int overall = (int) Math.round(eval.getOverallScore());
        binding.tvOverallScore.setText(String.valueOf(overall));

        // Rating tier
        if (overall >= 85) {
            binding.tvPerformanceTier.setText("Mastery Level Explanation");
            binding.tvPerformanceTier.setTextColor(ContextCompat.getColor(this, R.color.score_excellent));
        } else if (overall >= 70) {
            binding.tvPerformanceTier.setText("Competent Explanation");
            binding.tvPerformanceTier.setTextColor(ContextCompat.getColor(this, R.color.score_good));
        } else if (overall >= 55) {
            binding.tvPerformanceTier.setText("Developing Understanding");
            binding.tvPerformanceTier.setTextColor(ContextCompat.getColor(this, R.color.score_average));
        } else {
            binding.tvPerformanceTier.setText("Needs Foundational Review");
            binding.tvPerformanceTier.setTextColor(ContextCompat.getColor(this, R.color.score_poor));
        }

        // Subscores
        int acc = (int) Math.round(eval.getAccuracy());
        int comp = (int) Math.round(eval.getCompleteness());
        int clar = (int) Math.round(eval.getClarity());
        int reas = (int) Math.round(eval.getReasoning());

        binding.tvScoreAccuracy.setText(acc + " / 100");
        binding.pbAccuracy.setProgress(acc);

        binding.tvScoreCompleteness.setText(comp + " / 100");
        binding.pbCompleteness.setProgress(comp);

        binding.tvScoreClarity.setText(clar + " / 100");
        binding.pbClarity.setProgress(clar);

        binding.tvScoreReasoning.setText(reas + " / 100");
        binding.pbReasoning.setProgress(reas);

        // Feedback
        binding.tvFeedbackSummary.setText(eval.getFeedback());

        // Lists
        strengthsAdapter.setItems(eval.getStrengths());
        improvementsAdapter.setItems(eval.getImprovements());

        if (eval.getMisconceptions() == null || eval.getMisconceptions().isEmpty()) {
            binding.tvNoMisconceptions.setVisibility(View.VISIBLE);
            binding.rvMisconceptions.setVisibility(View.GONE);
        } else {
            binding.tvNoMisconceptions.setVisibility(View.GONE);
            binding.rvMisconceptions.setVisibility(View.VISIBLE);
            misconceptionAdapter.setItems(eval.getMisconceptions());
        }
    }
}
