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
    private ImageView techProfileIcon;
    private RecyclerView jobsRecyclerView;
    private TechnicianJobsAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String branchNameFilter = "TechFix Colombo"; // Fallback default
    private String branchId = "colombo";
    private String technicianName = "";

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
        techProfileIcon = findViewById(R.id.techProfileIcon);
        jobsRecyclerView = findViewById(R.id.jobsRecyclerView);

        jobsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianJobsAdapter();
        jobsRecyclerView.setAdapter(adapter);

        // Load profile and jobs
        loadTechnicianProfile(currentUser.getUid());

        if (techProfileIcon != null) {
            techProfileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(TechnicianDashboardActivity.this, com.techfix.app.customer.ProfileActivity.class);
                startActivity(intent);
            });
        }

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
                            technicianName = doc.getString("name");
                            technicianNameText.setText("Welcome, " + technicianName + "!");
                        }
                    }
                    // Load branch info next
                    loadTechnicianBranch(userId);
                });
    }

    private void loadTechnicianBranch(String userId) {
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
                    // Fetch jobs assigned specifically to this technician name
                    fetchJobs();
                });
    }

    private void fetchJobs() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail().trim() : "";
        String userId = (currentUser != null) ? currentUser.getUid() : "";

        if (technicianName == null || technicianName.isEmpty()) {
            technicianName = "Unassigned";
        }

        String firstName = (technicianName.contains(" ")) ? technicianName.split(" ")[0] : technicianName;

        mFirestore.collection("appointments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        jobsList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Appointment appt = doc.toObject(Appointment.class);
                            if (appt != null && appt.getAssignedTechnician() != null) {
                                String assigned = appt.getAssignedTechnician().trim();
                                boolean matchesEmail = !userEmail.isEmpty() && assigned.equalsIgnoreCase(userEmail);
                                boolean matchesName = !technicianName.isEmpty() && assigned.equalsIgnoreCase(technicianName);
                                boolean matchesUid = !userId.isEmpty() && assigned.equalsIgnoreCase(userId);
                                boolean matchesFirst = !firstName.isEmpty() && assigned.equalsIgnoreCase(firstName);

                                if (matchesEmail || matchesName || matchesUid || matchesFirst) {
                                    jobsList.add(appt);
                                    mDbHelper.insertOrUpdateAppointment(appt);
                                }
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userEmail = (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail().trim() : "";
        String userId = (currentUser != null) ? currentUser.getUid() : "";
        String firstName = (technicianName != null && technicianName.contains(" ")) ? technicianName.split(" ")[0] : technicianName;

        List<Appointment> allAppts = mDbHelper.getAppointmentsForCustomer("");
        List<Appointment> techAppts = new ArrayList<>();
        for (Appointment a : allAppts) {
            if (a.getAssignedTechnician() != null) {
                String assigned = a.getAssignedTechnician().trim();
                boolean matchesEmail = !userEmail.isEmpty() && assigned.equalsIgnoreCase(userEmail);
                boolean matchesName = technicianName != null && assigned.equalsIgnoreCase(technicianName);
                boolean matchesUid = !userId.isEmpty() && assigned.equalsIgnoreCase(userId);
                boolean matchesFirst = firstName != null && assigned.equalsIgnoreCase(firstName);

                if (matchesEmail || matchesName || matchesUid || matchesFirst) {
                    techAppts.add(a);
                }
            }
        }
        jobsList = techAppts;
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
