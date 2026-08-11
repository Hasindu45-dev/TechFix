package com.techfix.app.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Technician;
import com.techfix.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class AdminAllocateActivity extends AppCompatActivity {

    private TextView allocTicketId, allocDeviceModel, allocProblemDesc;
    private AutoCompleteTextView branchAutoComplete, techAutoComplete;
    private MaterialButton btnConfirmAllocation;
    private ProgressBar progressBar;

    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String appointmentId;
    private Appointment appointment;

    private final String[] BRANCHES = {"TechFix Colombo", "TechFix Galle"};
    private List<Technician> allTechnicians = new ArrayList<>();
    private List<String> techNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_allocate);

        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        appointmentId = getIntent().getStringExtra("APPOINTMENT_ID");

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Bind Views
        allocTicketId = findViewById(R.id.allocTicketId);
        allocDeviceModel = findViewById(R.id.allocDeviceModel);
        allocProblemDesc = findViewById(R.id.allocProblemDesc);
        branchAutoComplete = findViewById(R.id.branchAutoComplete);
        techAutoComplete = findViewById(R.id.techAutoComplete);
        btnConfirmAllocation = findViewById(R.id.btnConfirmAllocation);
        progressBar = findViewById(R.id.allocProgressBar);

        // Setup Branch dropdown
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, BRANCHES);
        branchAutoComplete.setAdapter(branchAdapter);

        // Fetch ticket details
        loadTicketDetails();

        // Listen for branch selection to update technician dropdown
        branchAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedBranch = BRANCHES[position];
            filterTechniciansByBranch(selectedBranch);
        });

        btnConfirmAllocation.setOnClickListener(v -> handleConfirmAllocation());
    }

    private void loadTicketDetails() {
        // SQLite query
        List<Appointment> allAppts = mDbHelper.getAppointmentsForCustomer("");
        for (Appointment a : allAppts) {
            if (a.getAppointmentId().equals(appointmentId)) {
                appointment = a;
                break;
            }
        }

        if (appointment != null) {
            populateUI();
        }

        // Firestore online query
        mFirestore.collection("appointments").document(appointmentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            appointment = doc.toObject(Appointment.class);
                            if (appointment != null) {
                                mDbHelper.insertOrUpdateAppointment(appointment);
                                populateUI();
                            }
                        }
                    }
                });

        // Load all technicians to filter
        loadTechniciansList();
    }

    private void populateUI() {
        allocTicketId.setText("TICKET: #" + appointment.getAppointmentId().substring(0, 8).toUpperCase());
        allocDeviceModel.setText(appointment.getDeviceModel());
        allocProblemDesc.setText("Problem: " + appointment.getProblemDescription());

        branchAutoComplete.setText(appointment.getAssignedBranch(), false);
        techAutoComplete.setText(appointment.getAssignedTechnician(), false);
    }

    private void loadTechniciansList() {
        mFirestore.collection("users")
                .whereEqualTo("role", "Technician")
                .get()
                .addOnCompleteListener(task -> {
                    allTechnicians.clear();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            User u = doc.toObject(User.class);
                            if (u != null) {
                                // Map User account to Technician object dynamically
                                String branchId = (u.getAddress() != null && u.getAddress().toLowerCase().contains("galle")) ? "galle" : "colombo";
                                Technician t = new Technician(u.getUserId(), u.getName(), "General", branchId, true);
                                allTechnicians.add(t);
                            }
                        }
                    }
                    // Fetch admin-created technicians from the dedicated collection
                    fetchAdminCreatedTechnicians();
                });
    }

    private void fetchAdminCreatedTechnicians() {
        mFirestore.collection("technicians")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Technician t = doc.toObject(Technician.class);
                            if (t != null) {
                                boolean exists = false;
                                for (Technician existing : allTechnicians) {
                                    if (existing.getTechnicianId().equals(t.getTechnicianId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    allTechnicians.add(t);
                                }
                            }
                        }
                    }
                    // Filter and bind to dropdown
                    filterTechniciansByBranch(branchAutoComplete.getText().toString());
                });
    }

    private void filterTechniciansByBranch(String branchName) {
        techNames.clear();
        String branchId = branchName.toLowerCase().contains("galle") ? "galle" : "colombo";

        for (Technician t : allTechnicians) {
            if (t.getBranchId().equalsIgnoreCase(branchId) && t.isAvailability()) {
                techNames.add(t.getName());
            }
        }

        // Fallback names if technicians aren't seeded in Firestore yet
        if (techNames.isEmpty()) {
            if ("colombo".equalsIgnoreCase(branchId)) {
                techNames.add("Amal Silva");
                techNames.add("Nimal Perera");
            } else {
                techNames.add("Sunil Fernando");
                techNames.add("Kasun Rajapakshe");
            }
        }

        ArrayAdapter<String> techAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, techNames);
        techAutoComplete.setAdapter(techAdapter);

        // Prepopulate first technician if name is empty or not in filter list
        if (!techNames.isEmpty()) {
            String currentTech = techAutoComplete.getText().toString();
            if (!techNames.contains(currentTech)) {
                techAutoComplete.setText(techNames.get(0), false);
            }
        }
    }

    private void handleConfirmAllocation() {
        String selectedBranch = branchAutoComplete.getText().toString().trim();
        String selectedTech = techAutoComplete.getText().toString().trim();

        if (TextUtils.isEmpty(selectedBranch)) {
            branchAutoComplete.setError("Please select a branch");
            return;
        }
        if (TextUtils.isEmpty(selectedTech)) {
            techAutoComplete.setError("Please select a technician");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmAllocation.setEnabled(false);

        // Update database: status sets to Assigned to Branch, updates branch & technician fields
        String newStatus = "Assigned to Branch";
        mFirestore.collection("appointments").document(appointmentId)
                .update(
                        "assignedBranch", selectedBranch,
                        "assignedTechnician", selectedTech,
                        "status", newStatus
                )
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirmAllocation.setEnabled(true);

                    if (task.isSuccessful()) {
                        if (appointment != null) {
                            appointment.setAssignedBranch(selectedBranch);
                            appointment.setAssignedTechnician(selectedTech);
                            appointment.setStatus(newStatus);
                            mDbHelper.insertOrUpdateAppointment(appointment);
                        }
                        Toast.makeText(AdminAllocateActivity.this, "Ticket reallocated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AdminAllocateActivity.this, "Reallocation failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
