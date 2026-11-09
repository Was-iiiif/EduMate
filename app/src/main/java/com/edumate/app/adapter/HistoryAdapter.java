package com.edumate.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.edumate.app.R;
import com.edumate.app.databinding.ItemHistorySessionBinding;
import com.edumate.app.model.Session;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    public interface OnSessionClickListener {
        void onSessionClick(Session session);
        void onDeleteClick(Session session);
    }

    private final List<Session> sessions = new ArrayList<>();
    private final OnSessionClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());

    public HistoryAdapter(OnSessionClickListener listener) {
        this.listener = listener;
    }

    public void setSessions(List<Session> newSessions) {
        this.sessions.clear();
        if (newSessions != null) {
            this.sessions.addAll(newSessions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemHistorySessionBinding binding = ItemHistorySessionBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Session session = sessions.get(position);

        holder.binding.tvSessionTopic.setText(session.getTopic());
        holder.binding.tvSessionDate.setText(dateFormat.format(new Date(session.getStartedAt())));

        // Score display
        if (Session.STATUS_COMPLETED.equals(session.getStatus())) {
            int scoreInt = (int) Math.round(session.getOverallScore());
            holder.binding.tvSessionScore.setText(String.valueOf(scoreInt));
            if (scoreInt >= 80) {
                holder.binding.tvSessionScore.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.score_excellent));
            } else if (scoreInt >= 70) {
                holder.binding.tvSessionScore.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.score_good));
            } else if (scoreInt >= 50) {
                holder.binding.tvSessionScore.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.score_average));
            } else {
                holder.binding.tvSessionScore.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.score_poor));
            }
        } else {
            holder.binding.tvSessionScore.setText("--");
            holder.binding.tvSessionScore.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
        }

        // Sync Status
        if (Session.SYNC_SYNCED.equals(session.getSyncStatus())) {
            holder.binding.tvSyncStatus.setText("Synced");
            holder.binding.tvSyncStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_synced));
        } else {
            holder.binding.tvSyncStatus.setText("Pending Sync");
            holder.binding.tvSyncStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_pending));
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSessionClick(session);
        });

        holder.binding.btnDeleteSession.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(session);
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHistorySessionBinding binding;

        ViewHolder(ItemHistorySessionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
