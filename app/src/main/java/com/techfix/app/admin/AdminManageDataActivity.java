package com.techfix.app.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.ImageView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.GenericCrudAdapter;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Branch;
import com.techfix.app.models.RequiredPart;
import com.techfix.app.models.Service;
import com.techfix.app.models.SparePart;
import com.techfix.app.models.Technician;
import com.techfix.app.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminManageDataActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView titleText;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private FloatingActionButton fabAdd;

    private DatabaseHelper mDbHelper;
    private FirebaseFirestore mFirestore;
    private GenericCrudAdapter adapter;
    private String manageType = "technicians";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_data);

        manageType = getIntent().getStringExtra("MANAGE_TYPE");
        if (manageType == null) manageType = "technicians";

        mDbHelper = new DatabaseHelper(this);
        mFirestore = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage " + capitalize(manageType.replace("_", " ")));
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.crudRecyclerView);
        emptyText = findViewById(R.id.crudEmptyStateText);
        fabAdd = findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GenericCrudAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setCrudActionListener(new GenericCrudAdapter.CrudActionListener() {
            @Override
            public void onEdit(GenericCrudAdapter.CrudItem item) {
                showEditDialog(item);
            }

            @Override
            public void onDelete(GenericCrudAdapter.CrudItem item) {
                showDeleteConfirmDialog(item);
            }
        });

        fabAdd.setOnClickListener(v -> showAddDialog());

        loadData();
        setupBottomNavigation();
    }

    private void loadData() {
        String path = getCollectionPath();
        mFirestore.collection(path)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<GenericCrudAdapter.CrudItem> list = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            GenericCrudAdapter.CrudItem item = createCrudItem(doc);
                            if (item != null) list.add(item);
                        }
                        adapter.setItems(list);
                        emptyText.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        emptyText.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Failed to load data from server.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getCollectionPath() {
        if ("services".equalsIgnoreCase(manageType)) return "services";
        if ("technicians".equalsIgnoreCase(manageType)) return "technicians";
        if ("spare_parts".equalsIgnoreCase(manageType)) return "spareParts";
        return "branches";
    }

    private GenericCrudAdapter.CrudItem createCrudItem(DocumentSnapshot doc) {
        String id = doc.getId();
        if ("services".equalsIgnoreCase(manageType)) {
            Service s = doc.toObject(Service.class);
            if (s != null) {
                return new GenericCrudAdapter.CrudItem(id, s.getName(), "Price: Rs. " + s.getPrice() + " (" + s.getCategory() + ")", s);
            }
        } else if ("technicians".equalsIgnoreCase(manageType)) {
            Technician t = doc.toObject(Technician.class);
            if (t != null) {
                String emailStr = (t.getEmail() != null && !t.getEmail().isEmpty()) ? " (" + t.getEmail() + ")" : "";
                return new GenericCrudAdapter.CrudItem(id, t.getName() + emailStr, "Spec: " + t.getSpecialization() + " (Branch: " + t.getBranchId() + ")", t);
            }
        } else if ("spare_parts".equalsIgnoreCase(manageType)) {
            SparePart sp = doc.toObject(SparePart.class);
            if (sp != null) {
                return new GenericCrudAdapter.CrudItem(id, sp.getName(), "Stock: " + sp.getQuantity() + " units (Branch: " + sp.getBranchId() + ")", sp);
            }
        } else {
            Branch b = doc.toObject(Branch.class);
            if (b != null) {
                return new GenericCrudAdapter.CrudItem(id, b.getName(), "Location: " + b.getAddress() + " (" + b.getLatitude() + ", " + b.getLongitude() + ")", b);
            }
        }
        return null;
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        if ("technicians".equalsIgnoreCase(manageType)) {
            final EditText nameInput = new EditText(this);
            nameInput.setHint("Technician Name");
            nameInput.setSingleLine(true);

            final EditText emailInput = new EditText(this);
            emailInput.setHint("Technician Email");
            emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            emailInput.setSingleLine(true);

            final EditText passwordInput = new EditText(this);
            passwordInput.setHint("Initial Password (min 6 chars)");
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordInput.setSingleLine(true);
            
            final Spinner specSpinner = new Spinner(this);
            ArrayAdapter<String> specAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Mobile", "Computer"});
            specAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            specSpinner.setAdapter(specAdapter);

            final TextView specLabel = new TextView(this);
            specLabel.setText("Specialization:");
            specLabel.setPadding(8, 16, 8, 4);

            final Spinner branchSpinner = new Spinner(this);
            ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Colombo", "Galle"});
            branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            branchSpinner.setAdapter(branchAdapter);

            final TextView branchLabel = new TextView(this);
            branchLabel.setText("Branch:");
            branchLabel.setPadding(8, 16, 8, 4);

            layout.addView(nameInput);
            layout.addView(emailInput);
            layout.addView(passwordInput);
            layout.addView(specLabel);
            layout.addView(specSpinner);
            layout.addView(branchLabel);
            layout.addView(branchSpinner);

            builder.setView(layout);

            builder.setPositiveButton("Add", (dialog, which) -> {
                String name = nameInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                String spec = specSpinner.getSelectedItem().toString();
                String branch = branchSpinner.getSelectedItem().toString().toLowerCase();

                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(this, "Name, Email, and Password are required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveNewTechnicianWithAuth(name, email, password, spec, branch);
            });
        } else if ("services".equalsIgnoreCase(manageType)) {
            builder.setTitle("Add New Service");

            final EditText nameInput = new EditText(this);
            nameInput.setHint("Service Name (e.g. Laptop Screen Repair)");
            nameInput.setSingleLine(true);

            final TextView categoryLabel = new TextView(this);
            categoryLabel.setText("Device Category:");
            categoryLabel.setPadding(8, 16, 8, 4);

            final Spinner categorySpinner = new Spinner(this);
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Computer", "Mobile"});
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(catAdapter);

            final EditText priceInput = new EditText(this);
            priceInput.setHint("Price in Rs. (e.g. 15000)");
            priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            priceInput.setSingleLine(true);

            final EditText descInput = new EditText(this);
            descInput.setHint("Description (e.g. Full screen replacement)");
            descInput.setSingleLine(true);

            final TextView partLabel = new TextView(this);
            partLabel.setText("Required Spare Part:");
            partLabel.setPadding(8, 16, 8, 4);

            final Spinner partSpinner = new Spinner(this);
            String[] defaultParts = new String[]{
                "None / Software Service",
                "Laptop Screen",
                "Mobile Screen",
                "Mobile Battery",
                "Laptop Battery",
                "SSD",
                "Laptop Keyboard",
                "Motherboard IC Chip",
                "USB-C Charging Port",
                "Camera Module",
                "Speaker Module",
                "Wi-Fi Antenna Module"
            };
            ArrayAdapter<String> partAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, defaultParts);
            partAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            partSpinner.setAdapter(partAdapter);

            layout.addView(nameInput);
            layout.addView(categoryLabel);
            layout.addView(categorySpinner);
            layout.addView(priceInput);
            layout.addView(descInput);
            layout.addView(partLabel);
            layout.addView(partSpinner);

            builder.setView(layout);

            builder.setPositiveButton("Add Service", (dialog, which) -> {
                String sName = nameInput.getText().toString().trim();
                String sCat = categorySpinner.getSelectedItem().toString();
                String sPriceStr = priceInput.getText().toString().trim();
                String sDesc = descInput.getText().toString().trim();
                String selectedPart = partSpinner.getSelectedItem().toString();

                if (TextUtils.isEmpty(sName)) {
                    Toast.makeText(this, "Service Name is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(sPriceStr)) {
                    Toast.makeText(this, "Price is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                double sPrice = 0.0;
                try {
                    sPrice = Double.parseDouble(sPriceStr);
                } catch (Exception e) {
                    sPrice = 0.0;
                }

                saveNewServiceRecord(sName, sCat, sPrice, sDesc, selectedPart);
            });
        } else {
            final EditText input1 = new EditText(this);
            final EditText input2 = new EditText(this);
            final EditText input3 = new EditText(this);
            layout.addView(input1);
            layout.addView(input2);
            layout.addView(input3);
            builder.setView(layout);
            builder.setPositiveButton("Add", (dialog, which) -> {
                saveNewRecord(input1.getText().toString().trim(), input2.getText().toString().trim(), input3.getText().toString().trim());
            });
        }

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveNewTechnicianWithAuth(String name, String email, String password, String spec, String branch) {
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("SecondaryAuthApp");
        } catch (Exception e) {
            secondaryApp = FirebaseApp.initializeApp(this, FirebaseApp.getInstance().getOptions(), "SecondaryAuthApp");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        String techUid = authResult.getUser().getUid();

                        User techUser = new User(techUid, name, email, "", "Technician", branch);
                        Technician tech = new Technician(techUid, name, email, spec, branch, true);

                        mFirestore.collection("users").document(techUid).set(techUser);
                        mFirestore.collection("technicians").document(techUid).set(tech)
                                .addOnSuccessListener(aVoid -> {
                                    secondaryAuth.signOut();
                                    Toast.makeText(AdminManageDataActivity.this, "Technician account created successfully!", Toast.LENGTH_SHORT).show();
                                    loadData();
                                })
                                .addOnFailureListener(e -> {
                                    secondaryAuth.signOut();
                                    Toast.makeText(AdminManageDataActivity.this, "Error saving record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminManageDataActivity.this, "Failed to create Auth account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveNewRecord(String val1, String val2, String val3) {
        String id = UUID.randomUUID().toString();
        String path = getCollectionPath();

        Object record;
        if ("services".equalsIgnoreCase(manageType)) {
            double price = Double.parseDouble(val3.isEmpty() ? "0" : val3);
            record = new Service(id, val1, val2, "General coursework service description", price, "1-2 Days", "");
            mDbHelper.insertOrUpdateService((Service) record);
        } else if ("technicians".equalsIgnoreCase(manageType)) {
            record = new Technician(id, val1, "", val2, val3, true);
        } else if ("spare_parts".equalsIgnoreCase(manageType)) {
            int qty = Integer.parseInt(val2.isEmpty() ? "0" : val2);
            record = new SparePart(id, val1, qty, 1500.0, val3);
        } else {
            record = new Branch(id, val1, val2, 6.92, 79.86);
        }

        mFirestore.collection(path).document(id).set(record).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show();
            loadData();
        });
    }

    private void showEditDialog(GenericCrudAdapter.CrudItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Record");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        if ("technicians".equalsIgnoreCase(manageType) && item.rawObject instanceof Technician) {
            Technician t = (Technician) item.rawObject;

            final EditText nameInput = new EditText(this);
            nameInput.setHint("Technician Name");
            nameInput.setText(t.getName());
            nameInput.setSingleLine(true);

            final EditText emailInput = new EditText(this);
            emailInput.setHint("Email (Read-only)");
            emailInput.setText(t.getEmail() != null ? t.getEmail() : "");
            emailInput.setEnabled(false);
            emailInput.setFocusable(false);
            emailInput.setSingleLine(true);
            
            final Spinner specSpinner = new Spinner(this);
            ArrayAdapter<String> specAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Mobile", "Computer"});
            specAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            specSpinner.setAdapter(specAdapter);
            if ("computer".equalsIgnoreCase(t.getSpecialization()) || "laptop".equalsIgnoreCase(t.getSpecialization())) {
                specSpinner.setSelection(1);
            } else {
                specSpinner.setSelection(0);
            }

            final TextView specLabel = new TextView(this);
            specLabel.setText("Specialization:");
            specLabel.setPadding(8, 16, 8, 4);

            final Spinner branchSpinner = new Spinner(this);
            ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Colombo", "Galle"});
            branchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            branchSpinner.setAdapter(branchAdapter);
            if ("galle".equalsIgnoreCase(t.getBranchId())) {
                branchSpinner.setSelection(1);
            } else {
                branchSpinner.setSelection(0);
            }

            final TextView branchLabel = new TextView(this);
            branchLabel.setText("Branch:");
            branchLabel.setPadding(8, 16, 8, 4);

            layout.addView(nameInput);
            layout.addView(emailInput);
            layout.addView(specLabel);
            layout.addView(specSpinner);
            layout.addView(branchLabel);
            layout.addView(branchSpinner);

            builder.setView(layout);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String val1 = nameInput.getText().toString().trim();
                String val2 = specSpinner.getSelectedItem().toString();
                String val3 = branchSpinner.getSelectedItem().toString().toLowerCase();

                if (TextUtils.isEmpty(val1)) {
                    Toast.makeText(this, "Technician Name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                updateTechnicianRecord(t.getTechnicianId(), t.getName(), val1, t.getEmail(), val2, val3);
            });
        } else if ("services".equalsIgnoreCase(manageType) && item.rawObject instanceof Service) {
            Service s = (Service) item.rawObject;
            builder.setTitle("Edit Service");

            final EditText nameInput = new EditText(this);
            nameInput.setHint("Service Name");
            nameInput.setText(s.getName());
            nameInput.setSingleLine(true);

            final TextView categoryLabel = new TextView(this);
            categoryLabel.setText("Device Category:");
            categoryLabel.setPadding(8, 16, 8, 4);

            final Spinner categorySpinner = new Spinner(this);
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Computer", "Mobile"});
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            categorySpinner.setAdapter(catAdapter);
            if ("mobile".equalsIgnoreCase(s.getCategory())) {
                categorySpinner.setSelection(1);
            } else {
                categorySpinner.setSelection(0);
            }

            final EditText priceInput = new EditText(this);
            priceInput.setHint("Price in Rs.");
            priceInput.setText(String.valueOf(s.getPrice()));
            priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            priceInput.setSingleLine(true);

            final EditText descInput = new EditText(this);
            descInput.setHint("Description");
            descInput.setText(s.getDescription() != null ? s.getDescription() : "");
            descInput.setSingleLine(true);

            final TextView partLabel = new TextView(this);
            partLabel.setText("Required Spare Part:");
            partLabel.setPadding(8, 16, 8, 4);

            final Spinner partSpinner = new Spinner(this);
            String[] defaultParts = new String[]{
                "None / Software Service",
                "Laptop Screen",
                "Mobile Screen",
                "Mobile Battery",
                "Laptop Battery",
                "SSD",
                "Laptop Keyboard",
                "Motherboard IC Chip",
                "USB-C Charging Port",
                "Camera Module",
                "Speaker Module",
                "Wi-Fi Antenna Module"
            };
            ArrayAdapter<String> partAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, defaultParts);
            partAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            partSpinner.setAdapter(partAdapter);

            String existingPart = "";
            if (s.getRequiredParts() != null && !s.getRequiredParts().isEmpty()) {
                existingPart = s.getRequiredParts().get(0).getPartName();
            }
            if (!existingPart.isEmpty()) {
                for (int i = 0; i < defaultParts.length; i++) {
                    if (defaultParts[i].equalsIgnoreCase(existingPart)) {
                        partSpinner.setSelection(i);
                        break;
                    }
                }
            }

            layout.addView(nameInput);
            layout.addView(categoryLabel);
            layout.addView(categorySpinner);
            layout.addView(priceInput);
            layout.addView(descInput);
            layout.addView(partLabel);
            layout.addView(partSpinner);

            builder.setView(layout);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String sName = nameInput.getText().toString().trim();
                String sCat = categorySpinner.getSelectedItem().toString();
                String sPriceStr = priceInput.getText().toString().trim();
                String sDesc = descInput.getText().toString().trim();
                String selectedPart = partSpinner.getSelectedItem().toString();

                if (TextUtils.isEmpty(sName)) {
                    Toast.makeText(this, "Service Name is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                double sPrice = 0.0;
                try {
                    sPrice = Double.parseDouble(sPriceStr);
                } catch (Exception e) {
                    sPrice = 0.0;
                }

                updateServiceRecord(s.getServiceId(), sName, sCat, sPrice, sDesc, selectedPart);
            });
        } else {
            final EditText input1 = new EditText(this);
            final EditText input2 = new EditText(this);
            final EditText input3 = new EditText(this);

            layout.addView(input1);
            layout.addView(input2);
            layout.addView(input3);

            if ("spare_parts".equalsIgnoreCase(manageType) && item.rawObject instanceof SparePart) {
                SparePart sp = (SparePart) item.rawObject;
                input1.setText(sp.getName());
                input2.setText(String.valueOf(sp.getQuantity()));
                input2.setInputType(InputType.TYPE_CLASS_NUMBER);
                input3.setText(sp.getBranchId());
            } else if (item.rawObject instanceof Branch) {
                Branch b = (Branch) item.rawObject;
                input1.setText(b.getName());
                input2.setText(b.getAddress());
                input3.setText(b.getLatitude() + "," + b.getLongitude());
            } else {
                input1.setText(item.title);
                input2.setText(item.subtitle);
                input3.setVisibility(View.GONE);
            }

            builder.setView(layout);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String val1 = input1.getText().toString().trim();
                String val2 = input2.getText().toString().trim();
                String val3 = input3.getText().toString().trim();

                updateRecord(item.id, val1, val2, val3);
            });
        }

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveNewServiceRecord(String name, String category, double price, String description, String requiredPartName) {
        String id = UUID.randomUUID().toString();
        Service service = new Service(id, name, category, description, price, "1-2 Days", "");
        if (requiredPartName != null && !requiredPartName.startsWith("None")) {
            List<RequiredPart> parts = new ArrayList<>();
            parts.add(new RequiredPart(requiredPartName, 1));
            service.setRequiredParts(parts);
        }

        mDbHelper.insertOrUpdateService(service);
        mFirestore.collection("services").document(id).set(service).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Service added successfully!", Toast.LENGTH_SHORT).show();
            loadData();
        });
    }

    private void updateServiceRecord(String id, String name, String category, double price, String description, String requiredPartName) {
        Service service = new Service(id, name, category, description, price, "1-2 Days", "");
        if (requiredPartName != null && !requiredPartName.startsWith("None")) {
            List<RequiredPart> parts = new ArrayList<>();
            parts.add(new RequiredPart(requiredPartName, 1));
            service.setRequiredParts(parts);
        }

        mDbHelper.insertOrUpdateService(service);
        mFirestore.collection("services").document(id).set(service).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Service updated successfully!", Toast.LENGTH_SHORT).show();
            loadData();
        });
    }

    private void updateTechnicianRecord(String techId, String oldName, String newName, String email, String spec, String branch) {
        Technician updatedTech = new Technician(techId, newName, email, spec, branch, true);

        mFirestore.collection("technicians").document(techId)
                .set(updatedTech)
                .addOnSuccessListener(aVoid -> {
                    mFirestore.collection("users").document(techId)
                            .update("name", newName, "address", branch);

                    if (oldName != null && !oldName.isEmpty() && !oldName.equalsIgnoreCase(newName)) {
                        mFirestore.collection("appointments")
                                .whereEqualTo("assignedTechnician", oldName)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (querySnapshot != null) {
                                        for (DocumentSnapshot doc : querySnapshot) {
                                            mFirestore.collection("appointments").document(doc.getId())
                                                    .update("assignedTechnician", newName);
                                        }
                                    }
                                });
                    }

                    Toast.makeText(this, "Technician record updated successfully!", Toast.LENGTH_SHORT).show();
                    loadData();
                });
    }

    private void updateRecord(String id, String val1, String val2, String val3) {
        String path = getCollectionPath();

        Object record;
        if ("services".equalsIgnoreCase(manageType)) {
            double price = Double.parseDouble(val3.isEmpty() ? "0" : val3);
            record = new Service(id, val1, val2, "General coursework service description", price, "1-2 Days", "");
            mDbHelper.insertOrUpdateService((Service) record);
        } else if ("technicians".equalsIgnoreCase(manageType)) {
            record = new Technician(id, val1, "", val2, val3, true);
        } else if ("spare_parts".equalsIgnoreCase(manageType)) {
            int qty = Integer.parseInt(val2.isEmpty() ? "0" : val2);
            record = new SparePart(id, val1, qty, 1500.0, val3);
        } else {
            record = new Branch(id, val1, val2, 6.92, 79.86);
        }

        mFirestore.collection(path).document(id)
                .set(record)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Record updated successfully!", Toast.LENGTH_SHORT).show();
                    loadData();
                });
    }

    private void showDeleteConfirmDialog(GenericCrudAdapter.CrudItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record?")
                .setMessage("Are you sure you want to permanently delete this record from the system?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    mFirestore.collection(getCollectionPath()).document(item.id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                loadData();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void setupBottomNavigation() {
        ImageView imgHome = findViewById(R.id.imgNavAdminHome);
        TextView txtHome = findViewById(R.id.txtNavAdminHome);
        ImageView imgTechs = findViewById(R.id.imgNavAdminTechs);
        TextView txtTechs = findViewById(R.id.txtNavAdminTechs);
        ImageView imgServices = findViewById(R.id.imgNavAdminServices);
        TextView txtServices = findViewById(R.id.txtNavAdminServices);
        ImageView imgInventory = findViewById(R.id.imgNavAdminInventory);
        TextView txtInventory = findViewById(R.id.txtNavAdminInventory);

        // Reset all to light color first
        int lightColor = getResources().getColor(R.color.primaryLightColor);
        int activeColor = getResources().getColor(R.color.secondaryColor);

        imgHome.setImageTintList(android.content.res.ColorStateList.valueOf(lightColor));
        txtHome.setTextColor(lightColor);
        imgTechs.setImageTintList(android.content.res.ColorStateList.valueOf(lightColor));
        txtTechs.setTextColor(lightColor);
        imgServices.setImageTintList(android.content.res.ColorStateList.valueOf(lightColor));
        txtServices.setTextColor(lightColor);
        imgInventory.setImageTintList(android.content.res.ColorStateList.valueOf(lightColor));
        txtInventory.setTextColor(lightColor);

        if ("technicians".equalsIgnoreCase(manageType)) {
            imgTechs.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            txtTechs.setTextColor(activeColor);
        } else if ("services".equalsIgnoreCase(manageType)) {
            imgServices.setImageTintList(android.content.res.ColorStateList.valueOf(activeColor));
            txtServices.setTextColor(activeColor);
        }

        findViewById(R.id.navAdminHome).setOnClickListener(v -> {
            Intent intent = new Intent(AdminManageDataActivity.this, AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navAdminTechs).setOnClickListener(v -> {
            if ("technicians".equalsIgnoreCase(manageType)) {
                Toast.makeText(AdminManageDataActivity.this, "Already on Technicians screen", Toast.LENGTH_SHORT).show();
            } else {
                switchManageType("technicians");
            }
        });

        findViewById(R.id.navAdminServices).setOnClickListener(v -> {
            if ("services".equalsIgnoreCase(manageType)) {
                Toast.makeText(AdminManageDataActivity.this, "Already on Services screen", Toast.LENGTH_SHORT).show();
            } else {
                switchManageType("services");
            }
        });

        findViewById(R.id.navAdminInventory).setOnClickListener(v -> {
            Intent intent = new Intent(AdminManageDataActivity.this, AdminSparePartsActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navAdminProfile).setOnClickListener(v -> {
            Intent intent = new Intent(AdminManageDataActivity.this, com.techfix.app.customer.ProfileActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void switchManageType(String type) {
        Intent intent = new Intent(AdminManageDataActivity.this, AdminManageDataActivity.class);
        intent.putExtra("MANAGE_TYPE", type);
        startActivity(intent);
        finish();
    }
}
