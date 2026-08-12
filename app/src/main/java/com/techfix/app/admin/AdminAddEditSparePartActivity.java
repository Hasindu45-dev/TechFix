package com.techfix.app.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.SparePart;

import java.util.UUID;

public class AdminAddEditSparePartActivity extends AppCompatActivity {

    private TextInputEditText nameEdit, descEdit, priceEdit, qtyEdit, minStockEdit;
    private AutoCompleteTextView branchAutoComplete;
    private MaterialButton btnSave, btnDelete;

    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String partId = null;
    private boolean isEditMode = false;

    private final String[] BRANCHES = {"TechFix Colombo", "TechFix Galle"};
    private final String[] BRANCH_IDS = {"colombo", "galle"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_edit_spare_part);

        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        // Bind Views
        nameEdit = findViewById(R.id.partNameEditText);
        descEdit = findViewById(R.id.partDescEditText);
        branchAutoComplete = findViewById(R.id.branchAutoComplete);
        priceEdit = findViewById(R.id.partPriceEditText);
        qtyEdit = findViewById(R.id.partQtyEditText);
        minStockEdit = findViewById(R.id.partMinStockEditText);
        btnSave = findViewById(R.id.btnSavePart);
        btnDelete = findViewById(R.id.btnDeletePart);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Branch autocomplete configuration
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, BRANCHES);
        branchAutoComplete.setAdapter(adapter);

        partId = getIntent().getStringExtra("PART_ID");
        if (partId != null) {
            isEditMode = true;
            getSupportActionBar().setTitle("Edit Spare Part");
            btnDelete.setVisibility(View.VISIBLE);
            loadPartDetails();
        } else {
            isEditMode = false;
            getSupportActionBar().setTitle("Add Spare Part");
            btnDelete.setVisibility(View.GONE);
        }

        btnSave.setOnClickListener(v -> handleSavePart());
        btnDelete.setOnClickListener(v -> handleDeletePart());
    }

    private void loadPartDetails() {
        mFirestore.collection("spareParts").document(partId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        SparePart part = doc.toObject(SparePart.class);
                        if (part != null) {
                            nameEdit.setText(part.getName());
                            descEdit.setText(part.getDescription());
                            priceEdit.setText(String.valueOf(part.getPrice()));
                            qtyEdit.setText(String.valueOf(part.getQuantity()));
                            minStockEdit.setText(String.valueOf(part.getMinimumStockLevel()));

                            // Map branch ID to name representation in autocomplete dropdown
                            String branchName = "galle".equalsIgnoreCase(part.getBranchId()) ? BRANCHES[1] : BRANCHES[0];
                            branchAutoComplete.setText(branchName, false);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load part from database", Toast.LENGTH_SHORT).show());
    }

    private void handleSavePart() {
        String name = nameEdit.getText().toString().trim();
        String desc = descEdit.getText().toString().trim();
        String category = "General";
        String branchText = branchAutoComplete.getText().toString().trim();
        String priceText = priceEdit.getText().toString().trim();
        String qtyText = qtyEdit.getText().toString().trim();
        String minStockText = minStockEdit.getText().toString().trim();

        // 1. Validations
        if (TextUtils.isEmpty(name)) {
            nameEdit.setError("Please enter part name.");
            return;
        }
        if (TextUtils.isEmpty(branchText)) {
            Toast.makeText(this, "Please select a branch.", Toast.LENGTH_SHORT).show();
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

        int qty;
        try {
            qty = Integer.parseInt(qtyText);
            if (qty < 0) {
                qtyEdit.setError("Quantity cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            qtyEdit.setError("Please enter a valid quantity.");
            return;
        }

        int minStock;
        try {
            minStock = Integer.parseInt(minStockText);
            if (minStock < 0) {
                minStockEdit.setError("Minimum stock warning level cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            minStockEdit.setError("Please enter a valid warning level.");
            return;
        }

        String branchId = BRANCH_IDS[0];
        if (BRANCHES[1].equalsIgnoreCase(branchText)) {
            branchId = BRANCH_IDS[1];
        }

        if (partId == null) {
            partId = UUID.randomUUID().toString();
        }

        long now = System.currentTimeMillis();
        SparePart part = new SparePart(partId, name, qty, price, branchId, desc, category, minStock, "", now, now);

        mFirestore.collection("spareParts").document(partId)
                .set(part)
                .addOnSuccessListener(aVoid -> {
                    mDbHelper.insertOrUpdateSparePart(part);
                    Toast.makeText(this, "Spare part saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save part online", Toast.LENGTH_SHORT).show());
    }

    private void handleDeletePart() {
        if (partId == null) return;

        mFirestore.collection("spareParts").document(partId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Part deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete part", Toast.LENGTH_SHORT).show());
    }
}
