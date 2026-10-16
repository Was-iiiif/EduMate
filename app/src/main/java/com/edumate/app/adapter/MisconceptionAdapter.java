package com.edumate.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumate.app.databinding.ItemMisconceptionBinding;
import com.edumate.app.model.Misconception;

import java.util.ArrayList;
import java.util.List;

public class MisconceptionAdapter extends RecyclerView.Adapter<MisconceptionAdapter.ViewHolder> {
    private final List<Misconception> items = new ArrayList<>();

    public void setItems(List<Misconception> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemMisconceptionBinding binding = ItemMisconceptionBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Misconception m = items.get(position);
        holder.binding.tvMisconceptionStatement.setText(m.getStatement());
        if (m.getCitation() != null && !m.getCitation().isEmpty()) {
            holder.binding.tvMisconceptionCitation.setText("Cited quote: " + m.getCitation());
        } else {
            holder.binding.tvMisconceptionCitation.setText("Identified from learner explanations");
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemMisconceptionBinding binding;

        ViewHolder(ItemMisconceptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
