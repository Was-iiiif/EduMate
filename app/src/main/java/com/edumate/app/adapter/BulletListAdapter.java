package com.edumate.app.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.edumate.app.databinding.ItemBulletPointBinding;

import java.util.ArrayList;
import java.util.List;

public class BulletListAdapter extends RecyclerView.Adapter<BulletListAdapter.ViewHolder> {
    private final List<String> items = new ArrayList<>();
    private final int bulletColorRes;

    public BulletListAdapter(int bulletColorRes) {
        this.bulletColorRes = bulletColorRes;
    }

    public void setItems(List<String> newItems) {
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
        ItemBulletPointBinding binding = ItemBulletPointBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = items.get(position);
        holder.binding.tvBulletText.setText(item);
        if (bulletColorRes != 0) {
            holder.binding.tvBulletIcon.setTextColor(
                    holder.itemView.getContext().getColor(bulletColorRes)
            );
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemBulletPointBinding binding;

        ViewHolder(ItemBulletPointBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
