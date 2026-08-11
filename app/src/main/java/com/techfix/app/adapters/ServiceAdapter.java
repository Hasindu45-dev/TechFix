package com.techfix.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techfix.app.R;
import com.techfix.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    private List<Service> services = new ArrayList<>();
    private OnServiceClickListener listener;

    public interface OnServiceClickListener {
        void onServiceClick(Service service);
    }

    public void setServices(List<Service> services) {
        this.services = services;
        notifyDataSetChanged();
    }

    public void setOnServiceClickListener(OnServiceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        Service currentService = services.get(position);
        holder.bind(currentService, listener);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView descText;
        private final TextView priceText;
        private final TextView durationText;
        private final TextView categoryBadge;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.serviceNameText);
            descText = itemView.findViewById(R.id.serviceDescText);
            priceText = itemView.findViewById(R.id.servicePriceText);
            durationText = itemView.findViewById(R.id.serviceDurationText);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
        }

        public void bind(Service service, OnServiceClickListener listener) {
            nameText.setText(service.getName());
            descText.setText(service.getDescription());
            priceText.setText("Rs. " + String.format("%,.2f", service.getPrice()));
            durationText.setText("Duration: " + service.getDuration());
            categoryBadge.setText(service.getCategory());

            // Set background color based on category
            if ("Computer".equalsIgnoreCase(service.getCategory())) {
                categoryBadge.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.primaryLightColor));
            } else {
                categoryBadge.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.secondaryColor));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onServiceClick(service);
                }
            });
        }
    }
}
