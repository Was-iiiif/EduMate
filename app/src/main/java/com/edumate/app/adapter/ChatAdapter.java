package com.edumate.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumate.app.databinding.ItemChatLearnerBinding;
import com.edumate.app.databinding.ItemChatStudentBinding;
import com.edumate.app.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_LEARNER = 1;
    private static final int VIEW_TYPE_STUDENT = 2;

    public interface OnReplayClickListener {
        void onReplayClick(String text);
    }

    private final List<Message> messages = new ArrayList<>();
    private final OnReplayClickListener replayClickListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter(OnReplayClickListener replayClickListener) {
        this.replayClickListener = replayClickListener;
    }

    public void setMessages(List<Message> newMessages) {
        this.messages.clear();
        if (newMessages != null) {
            this.messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyItemInserted(this.messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        return msg.isFromLearner() ? VIEW_TYPE_LEARNER : VIEW_TYPE_STUDENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_LEARNER) {
            ItemChatLearnerBinding binding = ItemChatLearnerBinding.inflate(inflater, parent, false);
            return new LearnerViewHolder(binding);
        } else {
            ItemChatStudentBinding binding = ItemChatStudentBinding.inflate(inflater, parent, false);
            return new StudentViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        String timeStr = timeFormat.format(new Date(msg.getCreatedAt()));

        if (holder instanceof LearnerViewHolder) {
            LearnerViewHolder lvh = (LearnerViewHolder) holder;
            lvh.binding.tvLearnerText.setText(msg.getMessageText());
            lvh.binding.tvLearnerTime.setText(timeStr);
        } else if (holder instanceof StudentViewHolder) {
            StudentViewHolder svh = (StudentViewHolder) holder;
            svh.binding.tvStudentText.setText(msg.getMessageText());
            svh.binding.tvStudentTime.setText(timeStr);
            svh.binding.btnReplayVoice.setOnClickListener(v -> {
                if (replayClickListener != null) {
                    replayClickListener.onReplayClick(msg.getMessageText());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class LearnerViewHolder extends RecyclerView.ViewHolder {
        final ItemChatLearnerBinding binding;

        LearnerViewHolder(ItemChatLearnerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        final ItemChatStudentBinding binding;

        StudentViewHolder(ItemChatStudentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
