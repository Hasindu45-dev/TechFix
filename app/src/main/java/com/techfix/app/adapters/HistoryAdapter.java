package com.techfix.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<DatabaseHelper.HistoryRecord> historyList = new ArrayList<>();

    public void setHistoryRecords(List<DatabaseHelper.HistoryRecord> list) {
        this.historyList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        DatabaseHelper.HistoryRecord record = historyList.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView idText, dateText, deviceText, serviceText, branchText, priceText, repairStatus, paymentStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            idText = itemView.findViewById(R.id.historyIdText);
            dateText = itemView.findViewById(R.id.historyDateText);
            deviceText = itemView.findViewById(R.id.historyDeviceText);
            serviceText = itemView.findViewById(R.id.historyServiceText);
            branchText = itemView.findViewById(R.id.historyBranchText);
            priceText = itemView.findViewById(R.id.historyPriceText);
            repairStatus = itemView.findViewById(R.id.historyRepairStatus);
            paymentStatus = itemView.findViewById(R.id.historyPaymentStatus);
        }

        public void bind(DatabaseHelper.HistoryRecord record) {
            idText.setText("Ticket ID: #" + record.appointmentId.substring(0, 8).toUpperCase());
            dateText.setText("Date: " + record.date);
            deviceText.setText(record.deviceModel);
            serviceText.setText(record.serviceName);
            branchText.setText(record.branchName);
            priceText.setText("Rs. " + String.format("%,.2f", record.cost));
            repairStatus.setText(record.repairStatus);
            paymentStatus.setText(record.paymentStatus);

            // Color code repair status
            if ("Ready for Pickup".equalsIgnoreCase(record.repairStatus) || "Repair Completed".equalsIgnoreCase(record.repairStatus)) {
                repairStatus.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.secondaryColor));
            } else {
                repairStatus.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.primaryColor));
            }

            // Color code payment status
            if ("Completed".equalsIgnoreCase(record.paymentStatus) || "Paid".equalsIgnoreCase(record.paymentStatus)) {
                paymentStatus.setText("PAID");
                paymentStatus.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                paymentStatus.setText("UNPAID");
                paymentStatus.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_orange_dark));
            }
        }
    }
}
