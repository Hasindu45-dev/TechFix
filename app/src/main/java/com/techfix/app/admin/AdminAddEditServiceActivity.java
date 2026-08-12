package com.techfix.app.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.RequiredPartsAdapter;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.RequiredPart;
import com.techfix.app.models.Service;
import com.techfix.app.models.SparePart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AdminAddEditServiceActivity extends AppCompatActivity {

    private TextInputEditText nameEdit, descEdit, priceEdit, durationEdit;
    private AutoCompleteTextView categoryAutoComplete;
    private RecyclerView requiredPartsRecyclerView;
    private MaterialButton btnAddPart, btnSave, btnDelete;

    private RequiredPartsAdapter adapter;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String serviceId = null;
    private boolean isEditMode = false;

    private List<RequiredPart> linkedParts = new ArrayList<>();
    private final String[] CATEGORIES = {"Computer", "Mobile"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_edit_service);

        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        // Bind Views
        nameEdit = findViewById(R.id.serviceNameEditText);
        categoryAutoComplete = findViewById(R.id.categoryAutoComplete);
        descEdit = findViewById(R.id.serviceDescEditText);
        priceEdit = findViewById(R.id.servicePriceEditText);
        durationEdit = findViewById(R.id.serviceDurationEditText);
        requiredPartsRecyclerView = findViewById(R.id.requiredPartsRecyclerView);
        btnAddPart = findViewById(R.id.btnAddPartToService);
        btnSave = findViewById(R.id.btnSaveService);
        btnDelete = findViewById(R.id.btnDeleteService);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Category dropdown setup
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        categoryAutoComplete.setAdapter(catAdapter);

        // RecyclerView setup
        requiredPartsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RequiredPartsAdapter();
        requiredPartsRecyclerView.setAdapter(adapter);

        adapter.setOnRemoveClickListener(position -> {
            linkedParts.remove(position);
            adapter.setData(linkedParts);
        });

        serviceId = getIntent().getStringExtra("SERVICE_ID");
        if (serviceId != null) {
            isEditMode = true;
            getSupportActionBar().setTitle("Edit Service");
            btnDelete.setVisibility(View.VISIBLE);
            loadServiceDetails();
        } else {
            isEditMode = false;
            getSupportActionBar().setTitle("Add Service");
            btnDelete.setVisibility(View.GONE);
        }

        btnAddPart.setOnClickListener(v -> showAddPartDialog());
        btnSave.setOnClickListener(v -> handleSaveService());
        btnDelete.setOnClickListener(v -> handleDeleteService());
    }

    private void loadServiceDetails() {
        mFirestore.collection("services").document(serviceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Service service = doc.toObject(Service.class);
                        if (service != null) {
                            nameEdit.setText(service.getName());
                            categoryAutoComplete.setText(service.getCategory(), false);
                            descEdit.setText(service.getDescription());
                            priceEdit.setText(String.valueOf(service.getPrice()));
                            durationEdit.setText(service.getDuration());
                            
                            if (service.getRequiredParts() != null) {
                                linkedParts.clear();
                                linkedParts.addAll(service.getRequiredParts());
                                adapter.setData(linkedParts);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load service", Toast.LENGTH_SHORT).show());
    }

    private void showAddPartDialog() {
        // Fetch all spare parts to extract unique names
        mFirestore.collection("spareParts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniquePartNames = new HashSet<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SparePart sp = doc.toObject(SparePart.class);
                        if (sp != null && sp.getName() != null) {
                            uniquePartNames.add(sp.getName());
                        }
                    }

                    if (uniquePartNames.isEmpty()) {
                        // Fallback to local SQLite parts if empty
                        List<SparePart> localParts = mDbHelper.getAllSpareParts();
                        for (SparePart sp : localParts) {
                            if (sp.getName() != null) {
                                uniquePartNames.add(sp.getName());
                            }
                        }
                    }

                    if (uniquePartNames.isEmpty()) {
                        Toast.makeText(this, "No spare parts registered. Please add spare parts first.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<String> partNamesList = new ArrayList<>(uniquePartNames);
                    showSelectionDialog(partNamesList);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching parts", Toast.LENGTH_SHORT).show());
    }

    private void showSelectionDialog(List<String> partNamesList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Required Spare Part");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_required_part, null);
        AutoCompleteTextView partDropdown = dialogView.findViewById(R.id.dialogPartAutoComplete);
        EditText qtyInput = dialogView.findViewById(R.id.dialogQtyInput);

        ArrayAdapter<String> dropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, partNamesList);
        partDropdown.setAdapter(dropAdapter);

        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String selectedPart = partDropdown.getText().toString().trim();
            String qtyStr = qtyInput.getText().toString().trim();

            if (TextUtils.isEmpty(selectedPart)) {
                Toast.makeText(this, "Part name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
                if (qty <= 0) {
                    Toast.makeText(this, "Required quantity must be positive.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid quantity.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if already mapped to avoid duplicates
            boolean updated = false;
            for (RequiredPart rp : linkedParts) {
                if (rp.getPartName().equalsIgnoreCase(selectedPart)) {
                    rp.setQuantity(qty);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                linkedParts.add(new RequiredPart(selectedPart, qty));
            }

            adapter.setData(linkedParts);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void handleSaveService() {
        String name = nameEdit.getText().toString().trim();
        String category = categoryAutoComplete.getText().toString().trim();
        String desc = descEdit.getText().toString().trim();
        String priceText = priceEdit.getText().toString().trim();
        String duration = durationEdit.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameEdit.setError("Please enter service name.");
            return;
        }
        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please select category.", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
            if (price < 0) {
                priceEdit.setError("Price cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            priceEdit.setError("Please enter a valid price.");
            return;
        }

        if (TextUtils.isEmpty(duration)) {
            durationEdit.setError("Please enter duration.");
            return;
        }

        if (serviceId == null) {
            serviceId = UUID.randomUUID().toString();
        }

        Service service = new Service(serviceId, name, category, desc, price, duration, "");
        service.setRequiredParts(linkedParts);

        mFirestore.collection("services").document(serviceId)
                .set(service)
                .addOnSuccessListener(aVoid -> {
                    mDbHelper.insertOrUpdateService(service);
                    Toast.makeText(this, "Service saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save service online", Toast.LENGTH_SHORT).show());
    }

    private void handleDeleteService() {
        if (serviceId == null) return;

        mFirestore.collection("services").document(serviceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Service deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete service", Toast.LENGTH_SHORT).show());
    }
}
