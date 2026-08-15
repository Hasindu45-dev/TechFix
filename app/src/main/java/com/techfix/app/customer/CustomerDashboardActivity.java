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
import com.techfix.app.models.RequiredPart;
import com.techfix.app.models.Service;
import com.techfix.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardActivity extends AppCompatActivity {

    private TextView welcomeUserText, emptyStateText, txtSeeAllServices;
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
    private boolean isShowingAllServices = false;

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
        txtSeeAllServices = findViewById(R.id.txtSeeAllServices);

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
        txtSeeAllServices.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, AllServicesActivity.class);
            intent.putExtra(AllServicesActivity.EXTRA_CATEGORY_FILTER, currentCategoryFilter);
            startActivity(intent);
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
        actionTrackRepair.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, com.techfix.app.customer.TrackRepairActivity.class);
            startActivity(intent);
        });
        actionHistory.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, com.techfix.app.customer.RepairHistoryActivity.class);
            startActivity(intent);
        });
        actionFindBranch.setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, com.techfix.app.maps.BranchLocatorActivity.class);
            startActivity(intent);
        });

        View btnHeroBookNow = findViewById(R.id.btnHeroBookNow);
        if (btnHeroBookNow != null) {
            btnHeroBookNow.setOnClickListener(v -> navigateToBookingFlow(null));
        }

        // Bottom Navigation Capsule click actions
        findViewById(R.id.navHome).setOnClickListener(v -> {
            findViewById(R.id.scrollViewContent).scrollTo(0, 0);
            Toast.makeText(this, "Home Dashboard", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.navTrack).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, com.techfix.app.customer.TrackRepairActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.navBook).setOnClickListener(v -> navigateToBookingFlow(null));
        findViewById(R.id.navHistory).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, com.techfix.app.customer.RepairHistoryActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerDashboardActivity.this, com.techfix.app.customer.ProfileActivity.class);
            startActivity(intent);
        });
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

    private void seedSQLiteServicesLocally() {
        List<Service> defaultServices = new ArrayList<>();
        // Computer services
        defaultServices.add(new Service("s1", "Laptop Screen Repair", "Computer", "Replacement of cracked, damaged or flickering laptop screen panel.", 15000.0, "1-2 days", ""));
        defaultServices.add(new Service("s2", "Operating System Installation", "Computer", "Clean installation of Windows or macOS with drivers and software setup.", 2500.0, "3 hours", ""));
        defaultServices.add(new Service("s3", "RAM/SSD Upgrade", "Computer", "Speed up your system by upgrading local memory and storage drives.", 8500.0, "1 hour", ""));
        defaultServices.add(new Service("s7", "Keyboard Replacement", "Computer", "Replace broken, sticky, or unresponsive keys with a fresh replacement keyboard layout.", 4500.0, "1-2 hours", ""));
        defaultServices.add(new Service("s8", "Thermal Paste & Cleaning", "Computer", "Prevent overheating and noise by cleaning fans and applying thermal paste.", 2000.0, "1 hour", ""));
        defaultServices.add(new Service("s9", "Motherboard Chip Repair", "Computer", "Advanced diagnostics and micro-soldering for power faults and water damage.", 18000.0, "3-5 days", ""));
        defaultServices.add(new Service("s10", "Data Recovery Service", "Computer", "Retrieve lost or deleted files from damaged hard drives or system failures.", 7500.0, "2 days", ""));
        
        // Mobile services
        defaultServices.add(new Service("s4", "Mobile Screen Replacement", "Mobile", "Premium display replacements for cracked, dead, or unresponsive touch screens.", 9500.0, "2 hours", ""));
        defaultServices.add(new Service("s5", "Battery Replacement", "Mobile", "Restore your device's original battery health with high-quality battery swap.", 4500.0, "1 hour", ""));
        defaultServices.add(new Service("s6", "Charging Port Repair", "Mobile", "Repair or replace faulty micro-USB or USB-C charging ports.", 3500.0, "1 hour", ""));
        defaultServices.add(new Service("s11", "Camera Module Repair", "Mobile", "Replace blurry, cracked, or shaking front/rear camera modules.", 5500.0, "1 hour", ""));
        defaultServices.add(new Service("s12", "Speaker & Mic Replacement", "Mobile", "Fix low call volume, crackling noise, or silent speakers.", 2500.0, "1 hour", ""));
        defaultServices.add(new Service("s13", "Water Damage Recovery", "Mobile", "Ultrasonic cleaning and motherboard treatment to recover liquid damage.", 4000.0, "24 hours", ""));
        defaultServices.add(new Service("s14", "Wi-Fi & Network Repair", "Mobile", "Fix antenna problems, weak cellular reception, or greyed Wi-Fi switch.", 6000.0, "1-2 days", ""));

        for (Service s : defaultServices) {
            mDbHelper.insertOrUpdateService(s);
        }
    }

    private void fetchServicesFromFirestore() {
        mFirestore.collection("services").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            // Seed default services ONLY if the Firestore collection is completely empty
                            seedDefaultServices();
                        } else {
                            List<Service> services = new ArrayList<>();
                            // Clear local SQLite cache table first to keep it in sync with Firestore deletions
                            mDbHelper.clearServicesTable();
                            
                            for (DocumentSnapshot doc : task.getResult()) {
                                Service service = doc.toObject(Service.class);
                                if (service != null) {
                                    // Self-correction: check if requiredParts is empty/null, map it!
                                    if (service.getRequiredParts() == null || service.getRequiredParts().isEmpty()) {
                                        boolean updated = false;
                                        String id = service.getServiceId();
                                        if ("s1".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Laptop Screen", 1));
                                            updated = true;
                                        } else if ("s3".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("SSD", 1));
                                            updated = true;
                                        } else if ("s7".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Laptop Keyboard", 1));
                                            updated = true;
                                        } else if ("s9".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Motherboard IC Chip", 1));
                                            updated = true;
                                        } else if ("s10".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Laptop Battery", 1));
                                            updated = true;
                                        } else if ("s4".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Mobile Screen", 1));
                                            updated = true;
                                        } else if ("s5".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Mobile Battery", 1));
                                            updated = true;
                                        } else if ("s6".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("USB-C Charging Port", 1));
                                            updated = true;
                                        } else if ("s11".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Camera Module", 1));
                                            updated = true;
                                        } else if ("s12".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Speaker Module", 1));
                                            updated = true;
                                        } else if ("s14".equals(id)) {
                                            service.getRequiredParts().add(new RequiredPart("Wi-Fi Antenna Module", 1));
                                            updated = true;
                                        }
                                        
                                        if (updated) {
                                            mFirestore.collection("services").document(id).set(service);
                                        }
                                    }
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
        
        // Computer services
        Service s1 = new Service("s1", "Laptop Screen Repair", "Computer", "Replacement of cracked, damaged or flickering laptop screen panel.", 15000.0, "1-2 days", "");
        s1.getRequiredParts().add(new RequiredPart("Laptop Screen", 1));
        defaultServices.add(s1);

        Service s2 = new Service("s2", "Operating System Installation", "Computer", "Clean installation of Windows or macOS with drivers and software setup.", 2500.0, "3 hours", "");
        defaultServices.add(s2); // OS Install - No spare parts needed!

        Service s3 = new Service("s3", "RAM/SSD Upgrade", "Computer", "Speed up your system by upgrading local memory and storage drives.", 8500.0, "1 hour", "");
        s3.getRequiredParts().add(new RequiredPart("SSD", 1));
        defaultServices.add(s3);

        Service s7 = new Service("s7", "Keyboard Replacement", "Computer", "Replace broken, sticky, or unresponsive keys with a fresh replacement keyboard layout.", 4500.0, "1-2 hours", "");
        s7.getRequiredParts().add(new RequiredPart("Laptop Keyboard", 1));
        defaultServices.add(s7);

        Service s8 = new Service("s8", "Thermal Paste & Cleaning", "Computer", "Prevent overheating and noise by cleaning fans and applying thermal paste.", 2000.0, "1 hour", "");
        defaultServices.add(s8); // Cleaning - No spare parts needed!

        Service s9 = new Service("s9", "Motherboard Chip Repair", "Computer", "Advanced diagnostics and micro-soldering for power faults and water damage.", 18000.0, "3-5 days", "");
        s9.getRequiredParts().add(new RequiredPart("Motherboard IC Chip", 1));
        defaultServices.add(s9);

        Service s10 = new Service("s10", "Laptop Battery Replacement", "Computer", "Restore your laptop's original battery health with high-quality battery swap.", 14500.0, "1-2 days", "");
        s10.getRequiredParts().add(new RequiredPart("Laptop Battery", 1));
        defaultServices.add(s10);
        
        // Mobile services
        Service s4 = new Service("s4", "Mobile Screen Replacement", "Mobile", "Premium display replacements for cracked, dead, or unresponsive touch screens.", 9500.0, "2 hours", "");
        s4.getRequiredParts().add(new RequiredPart("Mobile Screen", 1));
        defaultServices.add(s4);

        Service s5 = new Service("s5", "Battery Replacement", "Mobile", "Restore your device's original battery health with high-quality battery swap.", 4500.0, "1 hour", "");
        s5.getRequiredParts().add(new RequiredPart("Mobile Battery", 1));
        defaultServices.add(s5);

        Service s6 = new Service("s6", "Charging Port Repair", "Mobile", "Repair or replace faulty micro-USB or USB-C charging ports.", 3500.0, "1 hour", "");
        s6.getRequiredParts().add(new RequiredPart("USB-C Charging Port", 1));
        defaultServices.add(s6);

        Service s11 = new Service("s11", "Camera Module Repair", "Mobile", "Replace blurry, cracked, or shaking front/rear camera modules.", 5500.0, "1 hour", "");
        s11.getRequiredParts().add(new RequiredPart("Camera Module", 1));
        defaultServices.add(s11);

        Service s12 = new Service("s12", "Speaker & Mic Replacement", "Mobile", "Fix low call volume, crackling noise, or silent speakers.", 2500.0, "1 hour", "");
        s12.getRequiredParts().add(new RequiredPart("Speaker Module", 1));
        defaultServices.add(s12);

        Service s13 = new Service("s13", "Water Damage Recovery", "Mobile", "Ultrasonic cleaning and motherboard treatment to recover liquid damage.", 4000.0, "24 hours", "");
        defaultServices.add(s13); // Water damage cleaning - No parts needed!

        Service s14 = new Service("s14", "Wi-Fi & Network Repair", "Mobile", "Fix antenna problems, weak cellular reception, or greyed Wi-Fi switch.", 6000.0, "1-2 days", "");
        s14.getRequiredParts().add(new RequiredPart("Wi-Fi Antenna Module", 1));
        defaultServices.add(s14);

        for (Service s : defaultServices) {
            // Always insert into SQLite cache and update local state immediately
            mDbHelper.insertOrUpdateService(s);
            if (!allServicesList.contains(s)) {
                allServicesList.add(s);
            }
            
            // Try to sync with Firestore in background (may fail due to write permissions, which is fine)
            mFirestore.collection("services").document(s.getServiceId()).set(s);
        }
        applyFilters();
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

        // Show/hide 'See All' button based on total matching items
        if (filteredList.size() <= 2) {
            txtSeeAllServices.setVisibility(View.GONE);
        } else {
            txtSeeAllServices.setVisibility(View.VISIBLE);
        }

        // Apply count preview limit
        List<Service> displayedList;
        if (isShowingAllServices) {
            displayedList = filteredList;
            txtSeeAllServices.setText("Collapse");
        } else {
            displayedList = filteredList.subList(0, Math.min(filteredList.size(), 2));
            txtSeeAllServices.setText("See All");
        }

        serviceAdapter.setServices(displayedList);

        if (displayedList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            servicesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            servicesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToBookingFlow(String serviceId) {
        Intent intent = new Intent(CustomerDashboardActivity.this, BookAppointmentActivity.class);
        if (serviceId != null) {
            intent.putExtra("PREFILLED_SERVICE_ID", serviceId);
        }
        startActivity(intent);
    }


    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
