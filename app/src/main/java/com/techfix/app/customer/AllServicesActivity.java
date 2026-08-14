package com.techfix.app.customer;

import android.content.Intent;
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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.ServiceAdapter;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class AllServicesActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_FILTER = "CATEGORY_FILTER";

    private EditText searchEditText;
    private ChipGroup categoryChipGroup;
    private Chip chipAll, chipComputer, chipMobile;
    private RecyclerView servicesRecyclerView;
    private TextView emptyStateText;

    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;
    private ServiceAdapter serviceAdapter;

    private List<Service> allServicesList = new ArrayList<>();
    private String currentCategoryFilter = "All";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_services);

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
        searchEditText = findViewById(R.id.searchEditText);
        categoryChipGroup = findViewById(R.id.categoryChipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipComputer = findViewById(R.id.chipComputer);
        chipMobile = findViewById(R.id.chipMobile);
        servicesRecyclerView = findViewById(R.id.servicesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        // Setup RecyclerView
        servicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        serviceAdapter = new ServiceAdapter();
        servicesRecyclerView.setAdapter(serviceAdapter);

        // Read incoming category extra
        String initialCategory = getIntent().getStringExtra(EXTRA_CATEGORY_FILTER);
        if (initialCategory != null) {
            currentCategoryFilter = initialCategory;
        }

        // Pre-select appropriate chip
        if ("Computer".equalsIgnoreCase(currentCategoryFilter)) {
            chipComputer.setChecked(true);
        } else if ("Mobile".equalsIgnoreCase(currentCategoryFilter)) {
            chipMobile.setChecked(true);
        } else {
            chipAll.setChecked(true);
            currentCategoryFilter = "All";
        }

        // Load cached services first
        loadCachedServices();
        fetchServicesFromFirestore();

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

        // Service click action -> Navigate to Booking
        serviceAdapter.setOnServiceClickListener(service -> {
            Intent intent = new Intent(AllServicesActivity.this, BookAppointmentActivity.class);
            intent.putExtra("SERVICE_ID", service.getServiceId());
            startActivity(intent);
        });

        setupBottomNavigation();
    }

    private void loadCachedServices() {
        allServicesList = mDbHelper.getAllServices();
        applyFilters();
    }

    private void fetchServicesFromFirestore() {
        mFirestore.collection("services").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Service> fetched = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult()) {
                    Service s = doc.toObject(Service.class);
                    if (s != null) {
                        fetched.add(s);
                        mDbHelper.insertOrUpdateService(s);
                    }
                }
                if (!fetched.isEmpty()) {
                    allServicesList = fetched;
                    applyFilters();
                }
            }
        });
    }

    private void applyFilters() {
        List<Service> filtered = new ArrayList<>();

        for (Service s : allServicesList) {
            boolean matchesCategory = "All".equalsIgnoreCase(currentCategoryFilter) 
                    || s.getCategory().equalsIgnoreCase(currentCategoryFilter);
            
            boolean matchesSearch = currentSearchQuery.isEmpty() 
                    || s.getName().toLowerCase().contains(currentSearchQuery.toLowerCase())
                    || s.getDescription().toLowerCase().contains(currentSearchQuery.toLowerCase());

            if (matchesCategory && matchesSearch) {
                filtered.add(s);
            }
        }

        serviceAdapter.setServices(filtered);

        if (filtered.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            servicesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            servicesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(AllServicesActivity.this, CustomerDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navTrack).setOnClickListener(v -> {
            startActivity(new Intent(AllServicesActivity.this, TrackRepairActivity.class));
            finish();
        });

        findViewById(R.id.navBook).setOnClickListener(v -> {
            startActivity(new Intent(AllServicesActivity.this, BookAppointmentActivity.class));
            finish();
        });

        findViewById(R.id.navHistory).setOnClickListener(v -> {
            startActivity(new Intent(AllServicesActivity.this, RepairHistoryActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(AllServicesActivity.this, ProfileActivity.class));
            finish();
        });
    }
}
