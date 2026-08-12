package com.techfix.app.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techfix.app.R;
import com.techfix.app.models.RequiredPart;
import com.techfix.app.models.SparePart;

import java.util.ArrayList;
import java.util.List;

public class TechRequiredPartsAdapter extends RecyclerView.Adapter<TechRequiredPartsAdapter.ViewHolder> {

    private List<RequiredPart> requiredParts = new ArrayList<>();
    private List<SparePart> branchParts = new ArrayList<>();

    public void setData(List<RequiredPart> requiredParts, List<SparePart> branchParts) {
        this.requiredParts = requiredParts;
        this.branchParts = branchParts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tech_required_part, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequiredPart req = requiredParts.get(position);
        holder.nameText.setText(req.getPartName());
        holder.reqQtyText.setText("Required: " + req.getQuantity());

        // Match with branch-specific stock
        int available = 0;
        for (SparePart sp : branchParts) {
            if (sp.getName() != null && sp.getName().equalsIgnoreCase(req.getPartName())) {
                available = sp.getQuantity();
                break;
            }
        }

        holder.availQtyText.setText("Available: " + available);

        if (available < req.getQuantity()) {
            holder.availQtyText.setTextColor(Color.parseColor("#D32F2F")); // Highlight insufficient stock in Red
        } else {
            holder.availQtyText.setTextColor(Color.parseColor("#388E3C")); // Highlight sufficient stock in Green
        }
    }

    @Override
    public int getItemCount() {
        return requiredParts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, reqQtyText, availQtyText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.techPartName);
            reqQtyText = itemView.findViewById(R.id.techPartReqQty);
            availQtyText = itemView.findViewById(R.id.techPartAvailQty);
        }
    }
}
