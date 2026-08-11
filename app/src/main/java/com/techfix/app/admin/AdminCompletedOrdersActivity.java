package com.techfix.app.admin;

import android.os.Bundle;
import android.view.View;
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

    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private TechnicianJobsAdapter adapter;
    private List<Appointment> completedList = new ArrayList<>();
    private List<Service> servicesList = new ArrayList<>();

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

        // Setup RecyclerView
        completedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianJobsAdapter();
        completedRecyclerView.setAdapter(adapter);

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
                        adapter.setJobs(completedList, servicesList);
                        noCompletedOrdersText.setVisibility(completedList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        // Cache fallback
                        List<Appointment> cached = mDbHelper.getAppointmentsForCustomer("");
                        completedList.clear();
                        for (Appointment a : cached) {
                            if ("Completed".equalsIgnoreCase(a.getStatus())) {
                                completedList.add(a);
                            }
                        }
                        adapter.setJobs(completedList, servicesList);
                        noCompletedOrdersText.setVisibility(completedList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }
}
