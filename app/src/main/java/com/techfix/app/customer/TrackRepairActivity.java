package com.techfix.app.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class TrackRepairActivity extends AppCompatActivity {

    private AutoCompleteTextView ticketAutoComplete;
    private TextView lblDeviceModel, lblServiceType, lblBranchInfo, lblTechnicianInfo, noActiveRepairsText;
    private LinearLayout contentLayout, timelineContainer;
    private MaterialButton btnProceedToPayment;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private List<Appointment> activeAppointments = new ArrayList<>();
    private List<String> ticketOptions = new ArrayList<>();
    private List<Service> servicesList = new ArrayList<>();

    private Appointment selectedAppointment;
    private double selectedCost = 3000.0; // Base inspection fallback

    private final String[] STATUSES = {
        "Request Submitted",
        "Assigned to Branch",
        "Device Received",
        "Diagnosis Completed",
        "Repair Started",
        "Waiting for Spare Parts",
        "Repair Completed",
        "Ready for Pickup"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_repair);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Bind Views
        ticketAutoComplete = findViewById(R.id.ticketAutoComplete);
        lblDeviceModel = findViewById(R.id.lblDeviceModel);
        lblServiceType = findViewById(R.id.lblServiceType);
        lblBranchInfo = findViewById(R.id.lblBranchInfo);
        lblTechnicianInfo = findViewById(R.id.lblTechnicianInfo);
        noActiveRepairsText = findViewById(R.id.noActiveRepairsText);
        contentLayout = findViewById(R.id.contentLayout);
        timelineContainer = findViewById(R.id.timelineContainer);
        btnProceedToPayment = findViewById(R.id.btnProceedToPayment);

        loadActiveAppointments();

        ticketAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedAppointment = activeAppointments.get(position);
            displayAppointmentTimeline();
        });

        btnProceedToPayment.setOnClickListener(v -> {
            if (selectedAppointment != null) {
                Intent intent = new Intent(TrackRepairActivity.this, PaymentActivity.class);
                intent.putExtra("APPOINTMENT_ID", selectedAppointment.getAppointmentId());
                intent.putExtra("DEVICE_MODEL", selectedAppointment.getDeviceModel());
                intent.putExtra("SERVICE_COST", selectedCost);
                // Find service name
                String serviceName = "General Inspection";
                for (Service s : servicesList) {
                    if (s.getServiceId().equals(selectedAppointment.getServiceId())) {
                        serviceName = s.getName();
                        break;
                    }
                }
                intent.putExtra("SERVICE_NAME", serviceName);
                startActivity(intent);
            }
        });
    }

    private void loadActiveAppointments() {
        String customerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        
        // Cache first
        List<Appointment> cached = mDbHelper.getAppointmentsForCustomer(customerId);
        servicesList = mDbHelper.getAllServices();
        List<Appointment> unpaidCached = new ArrayList<>();
        for (Appointment a : cached) {
            if (isUnpaid(a)) {
                unpaidCached.add(a);
            }
        }
        if (!unpaidCached.isEmpty()) {
            activeAppointments = unpaidCached;
            populateTickets();
        }

        // Real-time online sync listener
        mFirestore.collection("appointments")
                .whereEqualTo("customerId", customerId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }
                    if (snapshots != null) {
                        List<Appointment> temp = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Appointment appt = doc.toObject(Appointment.class);
                            if (appt != null) {
                                mDbHelper.insertOrUpdateAppointment(appt);
                                if (isUnpaid(appt)) {
                                    temp.add(appt);
                                }
                            }
                        }
                        
                        // Handle potential cancellations/deletions on server or all orders paid
                        if (temp.isEmpty()) {
                            activeAppointments.clear();
                            selectedAppointment = null;
                            populateTickets();
                            return;
                        }

                        // Track previously selected ID to restore selection
                        String selectedId = selectedAppointment != null ? selectedAppointment.getAppointmentId() : null;
                        
                        activeAppointments = temp;
                        populateTickets();

                        // Restore selection
                        if (selectedId != null) {
                            for (Appointment a : activeAppointments) {
                                if (a.getAppointmentId().equals(selectedId)) {
                                    selectedAppointment = a;
                                    displayAppointmentTimeline();
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    private boolean isUnpaid(Appointment appt) {
        if (appt == null) return false;
        if ("Completed".equalsIgnoreCase(appt.getStatus())) {
            return false;
        }
        List<DatabaseHelper.HistoryRecord> cachedHist = mDbHelper.getHistoryForCustomer(
                mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : ""
        );
        for (DatabaseHelper.HistoryRecord h : cachedHist) {
            if (h.appointmentId.equals(appt.getAppointmentId()) && 
                    ("Completed".equalsIgnoreCase(h.paymentStatus) || "Paid".equalsIgnoreCase(h.paymentStatus))) {
                return false;
            }
        }
        return true;
    }

    private void populateTickets() {
        ticketOptions.clear();
        for (Appointment a : activeAppointments) {
            ticketOptions.add(a.getDeviceModel() + " (Ticket: #" + a.getAppointmentId().substring(0, 8).toUpperCase() + ")");
        }

        if (activeAppointments.isEmpty()) {
            noActiveRepairsText.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.GONE);
        } else {
            noActiveRepairsText.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ticketOptions);
            ticketAutoComplete.setAdapter(adapter);

            // Select active or fallback to first item
            if (selectedAppointment == null) {
                selectedAppointment = activeAppointments.get(0);
                ticketAutoComplete.setText(ticketOptions.get(0), false);
                displayAppointmentTimeline();
            } else {
                // Keep dropdown text aligned with current selection
                for (int i = 0; i < activeAppointments.size(); i++) {
                    if (activeAppointments.get(i).getAppointmentId().equals(selectedAppointment.getAppointmentId())) {
                        ticketAutoComplete.setText(ticketOptions.get(i), false);
                        break;
                    }
                }
            }
        }
    }

    private void displayAppointmentTimeline() {
        if (selectedAppointment == null) return;

        // Fetch matched service cost
        String serviceName = "General Repair Service";
        selectedCost = 3000.0;
        for (Service s : servicesList) {
            if (s.getServiceId().equalsIgnoreCase(selectedAppointment.getServiceId())) {
                serviceName = s.getName();
                selectedCost = s.getPrice();
                break;
            }
        }

        lblDeviceModel.setText("Device: " + selectedAppointment.getDeviceModel());
        lblServiceType.setText("Service: " + serviceName);
        
        String branch = selectedAppointment.getAssignedBranch();
        lblBranchInfo.setText("Assigned Branch: " + (branch != null && !branch.trim().isEmpty() ? branch : "Not Assigned"));
        
        String tech = selectedAppointment.getAssignedTechnician();
        lblTechnicianInfo.setText("Assigned Technician: " + (tech != null && !tech.trim().isEmpty() ? tech : "Not Assigned"));

        timelineContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        String currentStatus = selectedAppointment.getStatus();
        
        // Find index of current status
        int activeIndex = -1;
        for (int i = 0; i < STATUSES.length; i++) {
            if (STATUSES[i].equalsIgnoreCase(currentStatus)) {
                activeIndex = i;
                break;
            }
        }

        for (int i = 0; i < STATUSES.length; i++) {
            View stepView = inflater.inflate(R.layout.item_timeline_step, timelineContainer, false);
            
            View lineTop = stepView.findViewById(R.id.lineTop);
            View lineBottom = stepView.findViewById(R.id.lineBottom);
            ImageView circleIndicator = stepView.findViewById(R.id.circleIndicator);
            TextView statusTitle = stepView.findViewById(R.id.statusTitle);
            TextView statusSubtitle = stepView.findViewById(R.id.statusSubtitle);

            statusTitle.setText(STATUSES[i]);

            // Set top/bottom connectors
            if (i == 0) {
                lineTop.setVisibility(View.INVISIBLE);
            }
            if (i == STATUSES.length - 1) {
                lineBottom.setVisibility(View.INVISIBLE);
            }

            // Milestone state checking: Completed, Active, or Pending
            if (i < activeIndex) {
                // Completed State
                circleIndicator.setBackground(getDrawable(android.R.drawable.checkbox_on_background));
                circleIndicator.setBackgroundTintList(getColorStateList(android.R.color.holo_green_dark));
                statusTitle.setTextColor(getColor(R.color.textPrimary));
                statusSubtitle.setText("Completed");
                statusSubtitle.setTextColor(getColor(android.R.color.holo_green_dark));
            } else if (i == activeIndex) {
                // Current Active State
                circleIndicator.setBackground(getDrawable(android.R.drawable.radiobutton_on_background));
                circleIndicator.setBackgroundTintList(getColorStateList(R.color.secondaryColor));
                statusTitle.setTextColor(getColor(R.color.primaryColor));
                statusTitle.setTextSize(15f);
                statusTitle.setTypeface(statusTitle.getTypeface(), android.graphics.Typeface.BOLD);
                statusSubtitle.setText("Current Status");
                statusSubtitle.setTextColor(getColor(R.color.secondaryColor));
            } else {
                // Future Pending State
                circleIndicator.setBackground(getDrawable(android.R.drawable.radiobutton_off_background));
                circleIndicator.setBackgroundTintList(getColorStateList(R.color.dividerColor));
                statusTitle.setTextColor(getColor(R.color.textSecondary));
                statusSubtitle.setText("Pending");
                statusSubtitle.setTextColor(getColor(R.color.textSecondary));
            }

            timelineContainer.addView(stepView);
        }

        // Show/hide payment button based on completion status & check local payment records
        boolean repairCompleted = "Repair Completed".equalsIgnoreCase(currentStatus) 
                || "Ready for Pickup".equalsIgnoreCase(currentStatus);

        // Check local SQLite payment status cache
        List<DatabaseHelper.HistoryRecord> cachedHist = mDbHelper.getHistoryForCustomer(
                mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : ""
        );
        boolean isPaid = false;
        for (DatabaseHelper.HistoryRecord h : cachedHist) {
            if (h.appointmentId.equals(selectedAppointment.getAppointmentId()) && "Completed".equalsIgnoreCase(h.paymentStatus)) {
                isPaid = true;
                break;
            }
        }

        if (repairCompleted && !isPaid) {
            btnProceedToPayment.setVisibility(View.VISIBLE);
        } else {
            btnProceedToPayment.setVisibility(View.GONE);
        }
    }
}
