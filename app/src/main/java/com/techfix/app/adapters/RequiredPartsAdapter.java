package com.techfix.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techfix.app.R;
import com.techfix.app.models.RequiredPart;

import java.util.ArrayList;
import java.util.List;

public class RequiredPartsAdapter extends RecyclerView.Adapter<RequiredPartsAdapter.ViewHolder> {

    private List<RequiredPart> items = new ArrayList<>();
    private OnRemoveClickListener listener;

    public interface OnRemoveClickListener {
        void onRemove(int position);
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<RequiredPart> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_required_part_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequiredPart item = items.get(position);
        holder.nameText.setText(item.getPartName());
        holder.qtyText.setText("Required Qty: " + item.getQuantity());
        holder.removeBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, qtyText;
        ImageView removeBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.reqPartName);
            qtyText = itemView.findViewById(R.id.reqPartQty);
            removeBtn = itemView.findViewById(R.id.reqPartRemoveBtn);
        }
    }
}
