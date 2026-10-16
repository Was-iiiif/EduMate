package com.edumate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edumate.app.R;
import com.edumate.app.databinding.ActivityTopicSelectionBinding;
import com.edumate.app.model.Persona;
import com.google.android.material.chip.Chip;

public class TopicSelectionActivity extends AppCompatActivity {
    private ActivityTopicSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTopicSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Chip selection clears custom edit text focus
        binding.chipGroupTopics.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                binding.etCustomTopic.setText("");
            }
        });

        binding.etCustomTopic.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.chipGroupTopics.clearCheck();
            }
        });

        binding.btnBegin.setOnClickListener(v -> {
            String topic = getSelectedTopic();
            if (topic.trim().isEmpty()) {
                Toast.makeText(this, R.string.error_empty_topic, Toast.LENGTH_SHORT).show();
                return;
            }

            Persona persona = getSelectedPersona();

            Intent intent = new Intent(TopicSelectionActivity.this, SessionActivity.class);
            intent.putExtra(SessionActivity.EXTRA_TOPIC, topic.trim());
            intent.putExtra(SessionActivity.EXTRA_PERSONA, persona.name());
            startActivity(intent);
            finish();
        });
    }

    private String getSelectedTopic() {
        String custom = binding.etCustomTopic.getText() != null ? binding.etCustomTopic.getText().toString().trim() : "";
        if (!custom.isEmpty()) {
            return custom;
        }

        int checkedId = binding.chipGroupTopics.getCheckedChipId();
        if (checkedId != -1) {
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                return chip.getText().toString();
            }
        }
        return "";
    }

    private Persona getSelectedPersona() {
        int checkedId = binding.rgPersonas.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_skeptical) {
            return Persona.SKEPTICAL_PEER;
        } else if (checkedId == R.id.rb_exam) {
            return Persona.EXAM_STYLE;
        } else {
            return Persona.BEGINNER;
        }
    }
}
