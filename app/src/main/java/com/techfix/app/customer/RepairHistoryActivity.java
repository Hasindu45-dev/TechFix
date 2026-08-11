package com.techfix.app.customer;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.HistoryAdapter;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class RepairHistoryActivity extends AppCompatActivity {

    private TextView offlineBanner, emptyStateText;
    private RecyclerView historyRecyclerView;
    private HistoryAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair_history);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        customerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        offlineBanner = findViewById(R.id.offlineBanner);
        emptyStateText = findViewById(R.id.emptyStateText);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);

        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        historyRecyclerView.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        if (!isNetworkAvailable()) {
            // Offline Mode: Load strictly from SQLite
            offlineBanner.setVisibility(View.VISIBLE);
            List<DatabaseHelper.HistoryRecord> cachedList = mDbHelper.getHistoryForCustomer(customerId);
            adapter.setHistoryRecords(cachedList);
            toggleEmptyState(cachedList.isEmpty());
            Toast.makeText(this, "Loaded cached offline data.", Toast.LENGTH_SHORT).show();
        } else {
            // Online Mode: Fetch from Firestore, sync with SQLite
            offlineBanner.setVisibility(View.GONE);
            mFirestore.collection("appointments")
                    .whereEqualTo("customerId", customerId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<Appointment> appts = new ArrayList<>();
                            for (DocumentSnapshot doc : task.getResult()) {
                                Appointment appt = doc.toObject(Appointment.class);
                                if (appt != null) appts.add(appt);
                            }

                            // Pull services to match cost and name
                            mFirestore.collection("services").get().addOnCompleteListener(serviceTask -> {
                                List<Service> services = new ArrayList<>();
                                if (serviceTask.isSuccessful() && serviceTask.getResult() != null) {
                                    for (DocumentSnapshot doc : serviceTask.getResult()) {
                                        Service s = doc.toObject(Service.class);
                                        if (s != null) services.add(s);
                                    }
                                }

                                syncAndDisplay(appts, services);
                            });
                        } else {
                            // Fallback to cache on firestore read failure
                            loadHistoryOffline();
                        }
                    });
        }
    }

    private void syncAndDisplay(List<Appointment> appointments, List<Service> services) {
        List<DatabaseHelper.HistoryRecord> recordsList = new ArrayList<>();

        for (Appointment appt : appointments) {
            // Match service name and price
            String serviceName = "General Hardware Service";
            double cost = 3000.0; // Default base inspection cost
            for (Service s : services) {
                if (s.getServiceId().equalsIgnoreCase(appt.getServiceId())) {
                    serviceName = s.getName();
                    cost = s.getPrice();
                    break;
                }
            }

            // Sync with local SQLite DB
            String historyId = appt.getAppointmentId(); // Re-use appointment ID as history key
            // Determine payment status
            String payStatus = "Pending";
            if ("Repair Completed".equalsIgnoreCase(appt.getStatus()) || "Ready for Pickup".equalsIgnoreCase(appt.getStatus())) {
                // If repair finished, we will track payment status. In full app, it defaults to Pending until paid
                payStatus = "Pending";
            }
            // For coursework demonstration, check if payment is already recorded in online system or set default
            // In our system, the payment updates the status, so we can verify if it's completed
            
            // Check if user has updated it
            String dbPayStatus = mDbHelper.getHistoryForCustomer(customerId).stream()
                    .filter(h -> h.appointmentId.equals(appt.getAppointmentId()))
                    .map(h -> h.paymentStatus)
                    .findFirst()
                    .orElse("Pending");
            
            mDbHelper.insertOrUpdateHistory(
                    historyId,
                    appt.getAppointmentId(),
                    customerId,
                    appt.getDeviceModel(),
                    serviceName,
                    appt.getAssignedBranch(),
                    appt.getDate(),
                    cost,
                    dbPayStatus, // Retain payment updates locally/online
                    appt.getStatus()
            );

            recordsList.add(new DatabaseHelper.HistoryRecord(
                    historyId,
                    appt.getAppointmentId(),
                    customerId,
                    appt.getDeviceModel(),
                    serviceName,
                    appt.getAssignedBranch(),
                    appt.getDate(),
                    cost,
                    dbPayStatus,
                    appt.getStatus()
            ));
        }

        adapter.setHistoryRecords(recordsList);
        toggleEmptyState(recordsList.isEmpty());
    }

    private void loadHistoryOffline() {
        offlineBanner.setVisibility(View.VISIBLE);
        List<DatabaseHelper.HistoryRecord> cachedList = mDbHelper.getHistoryForCustomer(customerId);
        adapter.setHistoryRecords(cachedList);
        toggleEmptyState(cachedList.isEmpty());
    }

    private void toggleEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateText.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
