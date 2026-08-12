package com.techfix.app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.TechnicianJobsAdapter;
import com.techfix.app.authentication.LoginActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private MaterialCardView cardManageBranches, cardManageServices, cardManageTechnicians, cardManageParts, cardCompletedOrders;
    private RecyclerView adminRecyclerView;
    private TextView noAdminJobsText;
    private ImageView logoutIcon;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private TechnicianJobsAdapter adapter;
    private List<Appointment> appointmentsList = new ArrayList<>();
    private List<Service> servicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        // Bind dashboard shortcut cards
        cardManageBranches = findViewById(R.id.cardManageBranches);
        cardManageServices = findViewById(R.id.cardManageServices);
        cardManageTechnicians = findViewById(R.id.cardManageTechnicians);
        cardManageParts = findViewById(R.id.cardManageParts);
        cardCompletedOrders = findViewById(R.id.cardCompletedOrders);
        adminRecyclerView = findViewById(R.id.adminRecyclerView);
        noAdminJobsText = findViewById(R.id.noAdminJobsText);
        logoutIcon = findViewById(R.id.adminLogoutIcon);

        // RecyclerView setup
        adminRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianJobsAdapter();
        adapter.setIsAdmin(true); // Flag to change action button text to "Allocate"
        adminRecyclerView.setAdapter(adapter);

        // Setup CRUD Click Listeners
        cardManageBranches.setOnClickListener(v -> launchCrud("branches"));
        cardManageServices.setOnClickListener(v -> launchCrud("services"));
        cardManageTechnicians.setOnClickListener(v -> launchCrud("technicians"));
        cardManageParts.setOnClickListener(v -> startActivity(new Intent(AdminDashboardActivity.this, AdminSparePartsActivity.class)));
        cardCompletedOrders.setOnClickListener(v -> startActivity(new Intent(AdminDashboardActivity.this, AdminCompletedOrdersActivity.class)));

        // Admin Bottom Navigation Click Listeners
        findViewById(R.id.navAdminHome).setOnClickListener(v -> {
            Toast.makeText(this, "Already on Admin Dashboard", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.navAdminTechs).setOnClickListener(v -> launchCrud("technicians"));
        findViewById(R.id.navAdminServices).setOnClickListener(v -> launchCrud("services"));
        findViewById(R.id.navAdminProfile).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, com.techfix.app.customer.ProfileActivity.class);
            startActivity(intent);
        });

        // Click on job card opens the manual technician allocation view
        adapter.setOnJobClickListener(appt -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminAllocateActivity.class);
            intent.putExtra("APPOINTMENT_ID", appt.getAppointmentId());
            startActivity(intent);
        });

        loadAllAppointments();
    }

    private void launchCrud(String type) {
        Intent intent = new Intent(AdminDashboardActivity.this, AdminManageDataActivity.class);
        intent.putExtra("MANAGE_TYPE", type);
        startActivity(intent);
    }

    private void loadAllAppointments() {
        servicesList = mDbHelper.getAllServices();

        mFirestore.collection("appointments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        appointmentsList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            Appointment appt = doc.toObject(Appointment.class);
                            if (appt != null) {
                                mDbHelper.insertOrUpdateAppointment(appt);
                                if (!"Completed".equalsIgnoreCase(appt.getStatus())) {
                                    appointmentsList.add(appt);
                                }
                            }
                        }
                        adapter.setJobs(appointmentsList, servicesList);
                        noAdminJobsText.setVisibility(appointmentsList.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        // Offline caching fallback
                        List<Appointment> cached = mDbHelper.getAppointmentsForCustomer("");
                        appointmentsList.clear();
                        for (Appointment a : cached) {
                            if (!"Completed".equalsIgnoreCase(a.getStatus())) {
                                appointmentsList.add(a);
                            }
                        }
                        adapter.setJobs(appointmentsList, servicesList);
                        noAdminJobsText.setVisibility(appointmentsList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllAppointments();
    }
}
