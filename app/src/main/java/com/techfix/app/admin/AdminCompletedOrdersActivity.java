package com.techfix.app.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.TechnicianJobsAdapter;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class AdminCompletedOrdersActivity extends AppCompatActivity {

    private RecyclerView completedRecyclerView;
    private TextView noCompletedOrdersText;
    private EditText searchEditText;

    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private TechnicianJobsAdapter adapter;
    private List<Appointment> completedList = new ArrayList<>();
    private List<Service> servicesList = new ArrayList<>();
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_completed_orders);

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
        completedRecyclerView = findViewById(R.id.completedRecyclerView);
        noCompletedOrdersText = findViewById(R.id.noCompletedOrdersText);
        searchEditText = findViewById(R.id.searchEditText);

        // Setup RecyclerView
        completedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianJobsAdapter();
        completedRecyclerView.setAdapter(adapter);

        // Search text watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                applySearchFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadCompletedOrders();
    }

    private void loadCompletedOrders() {
        servicesList = mDbHelper.getAllServices();

        mFirestore.collection("appointments")
                .whereEqualTo("status", "Completed")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        completedList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Appointment appt = doc.toObject(Appointment.class);
                            if (appt != null) {
                                completedList.add(appt);
                            }
                        }
                        applySearchFilter();
                    } else {
                        // Cache fallback
                        List<Appointment> cached = mDbHelper.getAppointmentsForCustomer("");
                        completedList.clear();
                        for (Appointment a : cached) {
                            if ("Completed".equalsIgnoreCase(a.getStatus())) {
                                completedList.add(a);
                            }
                        }
                        applySearchFilter();
                    }
                });
    }

    private void applySearchFilter() {
        if (searchQuery.isEmpty()) {
            adapter.setJobs(completedList, servicesList);
            noCompletedOrdersText.setVisibility(completedList.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<Appointment> filtered = new ArrayList<>();
        for (Appointment appt : completedList) {
            boolean matchesDevice = appt.getDeviceModel() != null && appt.getDeviceModel().toLowerCase().contains(searchQuery);
            boolean matchesTicket = appt.getAppointmentId() != null && appt.getAppointmentId().toLowerCase().contains(searchQuery);
            boolean matchesTech = appt.getAssignedTechnician() != null && appt.getAssignedTechnician().toLowerCase().contains(searchQuery);

            if (matchesDevice || matchesTicket || matchesTech) {
                filtered.add(appt);
            }
        }

        adapter.setJobs(filtered, servicesList);
        noCompletedOrdersText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
