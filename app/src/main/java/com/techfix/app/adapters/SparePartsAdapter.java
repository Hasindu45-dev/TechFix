package com.techfix.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.techfix.app.R;
import com.techfix.app.models.SparePart;

import java.util.ArrayList;
import java.util.List;

public class SparePartsAdapter extends RecyclerView.Adapter<SparePartsAdapter.ViewHolder> {

    private List<SparePart> partsList = new ArrayList<>();
    private OnPartClickListener listener;

    public interface OnPartClickListener {
        void onEditClick(SparePart part);
    }

    public void setOnPartClickListener(OnPartClickListener listener) {
        this.listener = listener;
    }

    public void setParts(List<SparePart> parts) {
        this.partsList = parts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spare_part, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SparePart part = partsList.get(position);
        holder.bind(part, listener);
    }

    @Override
    public int getItemCount() {
        return partsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, categoryText, branchText, priceText, statusText, stockText;
        MaterialCardView statusCard;
        ImageView editBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.partItemName);
            categoryText = itemView.findViewById(R.id.partItemCategory);
            branchText = itemView.findViewById(R.id.partItemBranch);
            priceText = itemView.findViewById(R.id.partItemPrice);
            statusText = itemView.findViewById(R.id.partItemStatusText);
            stockText = itemView.findViewById(R.id.partItemStock);
            statusCard = itemView.findViewById(R.id.partItemStatusCard);
            editBtn = itemView.findViewById(R.id.partItemEditBtn);
        }

        public void bind(final SparePart part, final OnPartClickListener listener) {
            nameText.setText(part.getName());
            categoryText.setText("Category: " + part.getCategory());
            
            String branchName = "galle".equalsIgnoreCase(part.getBranchId()) ? "TechFix Galle" : "TechFix Colombo";
            branchText.setText("Branch: " + branchName);
            priceText.setText("Price: Rs. " + String.format("%,.2f", part.getPrice()));
            stockText.setText("Qty: " + part.getQuantity());

            // Status tag logic
            if (part.getQuantity() == 0) {
                statusText.setText("OUT OF STOCK");
                statusText.setTextColor(Color.parseColor("#D32F2F"));
                statusCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            } else if (part.getQuantity() <= part.getMinimumStockLevel()) {
                statusText.setText("LOW STOCK");
                statusText.setTextColor(Color.parseColor("#E65100"));
                statusCard.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
            } else {
                statusText.setText("AVAILABLE");
                statusText.setTextColor(Color.parseColor("#388E3C"));
                statusCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            }

            editBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(part);
                }
            });
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClick(part);
                }
            });
        }
    }
}
