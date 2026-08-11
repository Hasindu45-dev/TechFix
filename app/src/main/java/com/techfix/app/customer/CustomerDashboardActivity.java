package com.techfix.app.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.ServiceAdapter;
import com.techfix.app.authentication.LoginActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Service;
import com.techfix.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardActivity extends AppCompatActivity {

    private TextView welcomeUserText, emptyStateText;
    private ImageView logoutIcon;
    private EditText searchEditText;
    private ChipGroup categoryChipGroup;
    private RecyclerView servicesRecyclerView;

    // Quick actions
    private MaterialCardView actionBookRepair, actionTrackRepair, actionHistory, actionFindBranch;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;
    private ServiceAdapter serviceAdapter;

    private List<Service> allServicesList = new ArrayList<>();
    private String currentCategoryFilter = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Bind Views
        welcomeUserText = findViewById(R.id.welcomeUserText);
        logoutIcon = findViewById(R.id.logoutIcon);
        searchEditText = findViewById(R.id.searchEditText);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        actionBookRepair = findViewById(R.id.actionBookRepair);
        actionTrackRepair = findViewById(R.id.actionTrackRepair);
        actionHistory = findViewById(R.id.actionHistory);
        actionFindBranch = findViewById(R.id.actionFindBranch);

        // Setup RecyclerView
        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        serviceAdapter = new ServiceAdapter();
        servicesRecyclerView.setAdapter(serviceAdapter);

        // Load profile
        loadUserProfile(currentUser.getUid());

        // Load services
        loadCachedServices();
        fetchServicesFromFirestore();

        // Listeners
        logoutIcon.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });

        // Search text watcher
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Category filter chips listener
        categoryChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipComputer) {
                currentCategoryFilter = "Computer";
            } else if (checkedId == R.id.chipMobile) {
                currentCategoryFilter = "Mobile";
            } else {
                currentCategoryFilter = "All";
            }
            applyFilters();
        });

        // Service click action
        serviceAdapter.setOnServiceClickListener(service -> {
            Toast.makeText(this, "Selected Service: " + service.getName() + "\n(This service will be pre-filled on booking)", Toast.LENGTH_LONG).show();
            // We can navigate to Book Repair and pass the serviceId
            navigateToBookingFlow(service.getServiceId());
        });

        // Quick actions click listeners
        actionBookRepair.setOnClickListener(v -> navigateToBookingFlow(null));
        actionTrackRepair.setOnClickListener(v -> Toast.makeText(this, "Track Repair - Coming in Phase 7", Toast.LENGTH_SHORT).show());
        actionHistory.setOnClickListener(v -> Toast.makeText(this, "Repair History - Coming in Phase 7", Toast.LENGTH_SHORT).show());
        actionFindBranch.setOnClickListener(v -> Toast.makeText(this, "Find Branch - Coming in Phase 5", Toast.LENGTH_SHORT).show());
    }

    private void loadUserProfile(String userId) {
        User user = mDbHelper.getUser(userId);
        if (user != null) {
            welcomeUserText.setText("Welcome Back, " + user.getName() + "!");
        }

        mFirestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            User u = doc.toObject(User.class);
                            if (u != null) {
                                mDbHelper.insertOrUpdateUser(u);
                                welcomeUserText.setText("Welcome Back, " + u.getName() + "!");
                            }
                        }
                    }
                });
    }

    private void loadCachedServices() {
        allServicesList = mDbHelper.getAllServices();
        applyFilters();
    }

    private void fetchServicesFromFirestore() {
        mFirestore.collection("services").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            // Automatically seed Firestore with default coursework data
                            seedDefaultServices();
                        } else {
                            List<Service> services = new ArrayList<>();
                            for (DocumentSnapshot doc : task.getResult()) {
                                Service service = doc.toObject(Service.class);
                                if (service != null) {
                                    services.add(service);
                                    mDbHelper.insertOrUpdateService(service);
                                }
                            }
                            allServicesList = services;
                            applyFilters();
                        }
                    } else {
                        Toast.makeText(this, "Offline: Loaded from cache", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void seedDefaultServices() {
        List<Service> defaultServices = new ArrayList<>();
        defaultServices.add(new Service("s1", "Laptop Screen Repair", "Computer", "Replacement of cracked, damaged or flickering laptop screen panel.", 15000.0, "1-2 days", ""));
        defaultServices.add(new Service("s2", "Operating System Installation", "Computer", "Clean installation of Windows or macOS with drivers and software setup.", 2500.0, "3 hours", ""));
        defaultServices.add(new Service("s3", "RAM/SSD Upgrade", "Computer", "Speed up your system by upgrading local memory and storage drives.", 8500.0, "1 hour", ""));
        defaultServices.add(new Service("s4", "Mobile Screen Replacement", "Mobile", "Premium display replacements for cracked, dead, or unresponsive touch screens.", 9500.0, "2 hours", ""));
        defaultServices.add(new Service("s5", "Battery Replacement", "Mobile", "Restore your device's original battery health with high-quality battery swap.", 4500.0, "1 hour", ""));
        defaultServices.add(new Service("s6", "Charging Port Repair", "Mobile", "Repair or replace faulty micro-USB or USB-C charging ports.", 3500.0, "1 hour", ""));

        for (Service s : defaultServices) {
            mFirestore.collection("services").document(s.getServiceId())
                    .set(s)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mDbHelper.insertOrUpdateService(s);
                            if (!allServicesList.contains(s)) {
                                allServicesList.add(s);
                                applyFilters();
                            }
                        }
                    });
        }
    }

    private void applyFilters() {
        List<Service> filteredList = new ArrayList<>();

        for (Service service : allServicesList) {
            // Apply category filter
            boolean matchesCategory = currentCategoryFilter.equals("All") || 
                    service.getCategory().equalsIgnoreCase(currentCategoryFilter);

            // Apply search query filter
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                    service.getName().toLowerCase().contains(currentSearchQuery.toLowerCase()) ||
                    service.getCategory().toLowerCase().contains(currentSearchQuery.toLowerCase());

            if (matchesCategory && matchesSearch) {
                filteredList.add(service);
            }
        }

        serviceAdapter.setServices(filteredList);

        if (filteredList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            servicesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            servicesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToBookingFlow(String serviceId) {
        // Will launch the booking screen in Phase 6. Passing serviceId as an extra.
        Toast.makeText(this, "Book Request Form - Coming in Phase 6!", Toast.LENGTH_SHORT).show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
