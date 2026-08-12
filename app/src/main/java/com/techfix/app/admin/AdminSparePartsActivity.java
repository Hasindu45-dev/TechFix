package com.techfix.app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.SparePartsAdapter;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.utils.BranchAssignmentUtility;
import com.techfix.app.models.SparePart;

import java.util.ArrayList;
import java.util.List;

public class AdminSparePartsActivity extends AppCompatActivity {

    private TextInputEditText searchEditText;
    private AutoCompleteTextView branchFilterAutoComplete, statusFilterAutoComplete;
    private TextView noPartsText;
    private RecyclerView partsRecyclerView;
    private FloatingActionButton fabAddPart;

    private SparePartsAdapter adapter;
    private DatabaseHelper mDbHelper;
    private FirebaseFirestore mFirestore;

    private List<SparePart> allPartsList = new ArrayList<>();
    private List<SparePart> filteredPartsList = new ArrayList<>();

    private final String[] BRANCH_FILTER_OPTIONS = {"All Branches", "TechFix Colombo", "TechFix Galle"};
    private final String[] STATUS_FILTER_OPTIONS = {"All Stock Levels", "Available", "Low Stock", "Out of Stock"};

    private String selectedBranchFilter = "All Branches";
    private String selectedStatusFilter = "All Stock Levels";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_spare_parts);

        mDbHelper = new DatabaseHelper(this);
        mFirestore = FirebaseFirestore.getInstance();

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Bind Views
        searchEditText = findViewById(R.id.searchEditText);
        branchFilterAutoComplete = findViewById(R.id.branchFilterAutoComplete);
        statusFilterAutoComplete = findViewById(R.id.statusFilterAutoComplete);
        noPartsText = findViewById(R.id.noPartsText);
        partsRecyclerView = findViewById(R.id.partsRecyclerView);
        fabAddPart = findViewById(R.id.fabAddPart);

        // RecyclerView setup
        partsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SparePartsAdapter();
        partsRecyclerView.setAdapter(adapter);

        adapter.setOnPartClickListener(part -> {
            Intent intent = new Intent(AdminSparePartsActivity.this, AdminAddEditSparePartActivity.class);
            intent.putExtra("PART_ID", part.getPartId());
            startActivity(intent);
        });

        fabAddPart.setOnClickListener(v -> {
            Intent intent = new Intent(AdminSparePartsActivity.this, AdminAddEditSparePartActivity.class);
            startActivity(intent);
        });

        setupFilters();
        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSpareParts();
    }

    private void setupFilters() {
        // Branch filter dropdown setup
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, BRANCH_FILTER_OPTIONS);
        branchFilterAutoComplete.setAdapter(branchAdapter);
        branchFilterAutoComplete.setText(BRANCH_FILTER_OPTIONS[0], false);
        branchFilterAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedBranchFilter = BRANCH_FILTER_OPTIONS[position];
            applyFilters();
        });

        // Status filter dropdown setup
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, STATUS_FILTER_OPTIONS);
        statusFilterAutoComplete.setAdapter(statusAdapter);
        statusFilterAutoComplete.setText(STATUS_FILTER_OPTIONS[0], false);
        statusFilterAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedStatusFilter = STATUS_FILTER_OPTIONS[position];
            applyFilters();
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadSpareParts() {
        // Load locally first
        allPartsList = mDbHelper.getAllSpareParts();
        applyFilters();

        // Seed if needed, then sync online
        BranchAssignmentUtility.seedTechniciansAndPartsIfEmpty(mFirestore, () -> {
            mFirestore.collection("spareParts")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            allPartsList.clear();
                            mDbHelper.clearSparePartsTable();
                            for (DocumentSnapshot doc : task.getResult()) {
                                SparePart part = doc.toObject(SparePart.class);
                                if (part != null) {
                                    allPartsList.add(part);
                                    mDbHelper.insertOrUpdateSparePart(part);
                                }
                            }
                            applyFilters();
                        } else {
                            Toast.makeText(this, "Offline mode: Showing cached inventory.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    private void applyFilters() {
        filteredPartsList.clear();

        for (SparePart part : allPartsList) {
            // 1. Check Search Query (matches name or category)
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (part.getName() != null && part.getName().toLowerCase().contains(searchQuery)) ||
                    (part.getCategory() != null && part.getCategory().toLowerCase().contains(searchQuery));

            // 2. Check Branch Filter
            boolean matchesBranch = true;
            if (!"All Branches".equals(selectedBranchFilter)) {
                String targetBranchId = "TechFix Colombo".equals(selectedBranchFilter) ? "colombo" : "galle";
                matchesBranch = targetBranchId.equalsIgnoreCase(part.getBranchId());
            }

            // 3. Check Status Filter
            boolean matchesStatus = true;
            if (!"All Stock Levels".equals(selectedStatusFilter)) {
                if ("Available".equals(selectedStatusFilter)) {
                    matchesStatus = part.getQuantity() > part.getMinimumStockLevel();
                } else if ("Low Stock".equals(selectedStatusFilter)) {
                    matchesStatus = part.getQuantity() > 0 && part.getQuantity() <= part.getMinimumStockLevel();
                } else if ("Out of Stock".equals(selectedStatusFilter)) {
                    matchesStatus = part.getQuantity() == 0;
                }
            }

            if (matchesSearch && matchesBranch && matchesStatus) {
                filteredPartsList.add(part);
            }
        }

        adapter.setParts(filteredPartsList);

        if (filteredPartsList.isEmpty()) {
            noPartsText.setVisibility(View.VISIBLE);
            partsRecyclerView.setVisibility(View.GONE);
        } else {
            noPartsText.setVisibility(View.GONE);
            partsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
