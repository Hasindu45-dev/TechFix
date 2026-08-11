package com.techfix.app.technician;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.TechnicianJobsAdapter;
import com.techfix.app.authentication.LoginActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;
import com.techfix.app.models.Technician;

import java.util.ArrayList;
import java.util.List;

public class TechnicianDashboardActivity extends AppCompatActivity {

    private TextView technicianNameText, technicianBranchText, noJobsText;
    private ImageView logoutIcon;
    private RecyclerView jobsRecyclerView;
    private TechnicianJobsAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String branchNameFilter = "TechFix Colombo"; // Fallback default
    private String branchId = "colombo";

    private List<Appointment> jobsList = new ArrayList<>();
    private List<Service> servicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Bind Views
        technicianNameText = findViewById(R.id.technicianNameText);
        technicianBranchText = findViewById(R.id.technicianBranchText);
        noJobsText = findViewById(R.id.noJobsText);
        logoutIcon = findViewById(R.id.logoutIcon);
        jobsRecyclerView = findViewById(R.id.jobsRecyclerView);

        jobsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianJobsAdapter();
        jobsRecyclerView.setAdapter(adapter);

        // Load profile and jobs
        loadTechnicianProfile(currentUser.getUid());

        logoutIcon.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });

        adapter.setOnJobClickListener(appt -> {
            Intent intent = new Intent(TechnicianDashboardActivity.this, TechnicianJobDetailActivity.class);
            intent.putExtra("APPOINTMENT_ID", appt.getAppointmentId());
            startActivity(intent);
        });
    }

    private void loadTechnicianProfile(String userId) {
        // Load basic name from local DB
        mDbHelper = new DatabaseHelper(this);
        servicesList = mDbHelper.getAllServices();

        mFirestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            technicianNameText.setText("Welcome, " + name + "!");
                        }
                    }
                });

        // Determine technician branch
        mFirestore.collection("technicians").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Technician tech = doc.toObject(Technician.class);
                            if (tech != null) {
                                branchId = tech.getBranchId();
                                branchNameFilter = "galle".equalsIgnoreCase(branchId) ? "TechFix Galle" : "TechFix Colombo";
                                technicianBranchText.setText("Branch: " + branchNameFilter);
                            }
                        }
                    }
                    // Fetch jobs assigned to this branch
                    fetchJobs();
                });
    }

    private void fetchJobs() {
        mFirestore.collection("appointments")
                .whereEqualTo("assignedBranch", branchNameFilter)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        jobsList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Appointment appt = doc.toObject(Appointment.class);
                            if (appt != null) {
                                jobsList.add(appt);
                                mDbHelper.insertOrUpdateAppointment(appt);
                            }
                        }
                        adapter.setJobs(jobsList, servicesList);
                        toggleEmptyState(jobsList.isEmpty());
                    } else {
                        // Offline caching fallback
                        loadJobsOffline();
                    }
                });
    }

    private void loadJobsOffline() {
        // Query appointments locally for this branch
        List<Appointment> allAppts = mDbHelper.getAppointmentsForCustomer(""); // empty gets all locally
        List<Appointment> branchAppts = new ArrayList<>();
        for (Appointment a : allAppts) {
            if (a.getAssignedBranch().equalsIgnoreCase(branchNameFilter)) {
                branchAppts.add(a);
            }
        }
        jobsList = branchAppts;
        adapter.setJobs(jobsList, servicesList);
        toggleEmptyState(jobsList.isEmpty());
    }

    private void toggleEmptyState(boolean isEmpty) {
        if (isEmpty) {
            noJobsText.setVisibility(View.VISIBLE);
            jobsRecyclerView.setVisibility(View.GONE);
        } else {
            noJobsText.setVisibility(View.GONE);
            jobsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
