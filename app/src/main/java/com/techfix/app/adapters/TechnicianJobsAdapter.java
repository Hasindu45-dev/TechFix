package com.techfix.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class TechnicianJobsAdapter extends RecyclerView.Adapter<TechnicianJobsAdapter.JobViewHolder> {

    private List<Appointment> appointments = new ArrayList<>();
    private List<Service> services = new ArrayList<>();
    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(Appointment appointment);
    }

    public void setJobs(List<Appointment> list, List<Service> services) {
        this.appointments = list;
        this.services = services;
        notifyDataSetChanged();
    }

    public void setOnJobClickListener(OnJobClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_technician_job, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        Appointment appt = appointments.get(position);
        holder.bind(appt, services, listener);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {

        private final TextView jobIdText, jobDateText, jobDeviceText, jobServiceText, jobCustomerText, jobStatusText;
        private final MaterialButton btnUpdateJob;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            jobIdText = itemView.findViewById(R.id.jobIdText);
            jobDateText = itemView.findViewById(R.id.jobDateText);
            jobDeviceText = itemView.findViewById(R.id.jobDeviceText);
            jobServiceText = itemView.findViewById(R.id.jobServiceText);
            jobCustomerText = itemView.findViewById(R.id.jobCustomerText);
            jobStatusText = itemView.findViewById(R.id.jobStatusText);
            btnUpdateJob = itemView.findViewById(R.id.btnUpdateJob);
        }

        public void bind(Appointment appt, List<Service> services, OnJobClickListener listener) {
            jobIdText.setText("TICKET: #" + appt.getAppointmentId().substring(0, 8).toUpperCase());
            jobDateText.setText("Date: " + appt.getDate());
            jobDeviceText.setText(appt.getDeviceModel());
            
            // Match service name
            String serviceName = "General Repair";
            for (Service s : services) {
                if (s.getServiceId().equalsIgnoreCase(appt.getServiceId())) {
                    serviceName = s.getName();
                    break;
                }
            }
            jobServiceText.setText("Service: " + serviceName);
            
            // In a full implementation, customer name and phone can be retrieved from users collection.
            // For this view, we display the customer's ID and contact detail fallback.
            jobCustomerText.setText("Client ID: " + appt.getCustomerId().substring(0, 8).toUpperCase());
            jobStatusText.setText(appt.getStatus());

            btnUpdateJob.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onJobClick(appt);
                }
            });
        }
    }
}
