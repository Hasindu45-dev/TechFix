package com.techfix.app.admin;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.GenericCrudAdapter;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Branch;
import com.techfix.app.models.Service;
import com.techfix.app.models.SparePart;
import com.techfix.app.models.Technician;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminManageDataActivity extends AppCompatActivity {

    private String manageType;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FloatingActionButton fabAdd;
    private GenericCrudAdapter adapter;

    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private List<GenericCrudAdapter.CrudItem> crudItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_data);

        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        manageType = getIntent().getStringExtra("MANAGE_TYPE");
        if (manageType == null) manageType = "branches";

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Customize header title
        getSupportActionBar().setTitle("Manage " + capitalize(manageType.replace("_", " ")));

        recyclerView = findViewById(R.id.crudRecyclerView);
        emptyStateText = findViewById(R.id.crudEmptyStateText);
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
                showDeleteConfirmation(item);
            }
        });

        fabAdd.setOnClickListener(v -> showAddDialog());

        loadData();
    }

    private void loadData() {
        crudItems.clear();
        String collectionPath = getCollectionPath();

        mFirestore.collection(collectionPath)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            GenericCrudAdapter.CrudItem ci = parseDocument(doc);
                            if (ci != null) crudItems.add(ci);
                        }
                        adapter.setItems(crudItems);
                        emptyStateText.setVisibility(crudItems.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(this, "Offline fallback: Showing local cache", Toast.LENGTH_SHORT).show();
                        loadOfflineData();
                    }
                });
    }

    private void loadOfflineData() {
        // Fallback SQLite loaders
        crudItems.clear();
        if ("branches".equalsIgnoreCase(manageType)) {
            // Colombo & Galle static check
            crudItems.add(new GenericCrudAdapter.CrudItem("colombo", "TechFix Colombo", "Location: Colombo 03", null));
            crudItems.add(new GenericCrudAdapter.CrudItem("galle", "TechFix Galle", "Location: Galle Fort", null));
        } else if ("services".equalsIgnoreCase(manageType)) {
            List<Service> services = mDbHelper.getAllServices();
            for (Service s : services) {
                crudItems.add(new GenericCrudAdapter.CrudItem(
                        s.getServiceId(), s.getName(), "Rs. " + s.getPrice() + " (" + s.getCategory() + ")", s
                ));
            }
        }
        adapter.setItems(crudItems);
        emptyStateText.setVisibility(crudItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getCollectionPath() {
        switch (manageType) {
            case "services": return "services";
            case "technicians": return "technicians";
            case "spare_parts": return "spare_parts";
            default: return "branches";
        }
    }

    private GenericCrudAdapter.CrudItem parseDocument(DocumentSnapshot doc) {
        String id = doc.getId();
        if ("services".equalsIgnoreCase(manageType)) {
            Service s = doc.toObject(Service.class);
            if (s != null) {
                return new GenericCrudAdapter.CrudItem(id, s.getName(), "Price: Rs. " + s.getPrice() + " (" + s.getCategory() + ")", s);
            }
        } else if ("technicians".equalsIgnoreCase(manageType)) {
            Technician t = doc.toObject(Technician.class);
            if (t != null) {
                return new GenericCrudAdapter.CrudItem(id, t.getName(), "Spec: " + t.getSpecialization() + " (Branch: " + t.getBranchId() + ")", t);
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
        if ("services".equalsIgnoreCase(manageType)) {
            android.content.Intent intent = new android.content.Intent(this, AdminAddEditServiceActivity.class);
            startActivity(intent);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add " + capitalize(manageType.substring(0, manageType.length() - (manageType.endsWith("s") ? 1 : 0))));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText input1 = new EditText(this);
        final EditText input2 = new EditText(this);
        final EditText input3 = new EditText(this);

        if ("services".equalsIgnoreCase(manageType)) {
            input1.setHint("Service Name (e.g. Screen Replacement)");
            input2.setHint("Category (Mobile or Computer)");
            input3.setHint("Price (LKR)");
            input3.setInputType(InputType.TYPE_CLASS_NUMBER);
            layout.addView(input1);
            layout.addView(input2);
            layout.addView(input3);
        } else if ("technicians".equalsIgnoreCase(manageType)) {
            input1.setHint("Technician Name");
            input2.setHint("Specialization (Mobile or Laptop)");
            input3.setHint("Branch ID (colombo or galle)");
            layout.addView(input1);
            layout.addView(input2);
            layout.addView(input3);
        } else if ("spare_parts".equalsIgnoreCase(manageType)) {
            input1.setHint("Part Name (e.g. Laptop Screen)");
            input2.setHint("Quantity");
            input2.setInputType(InputType.TYPE_CLASS_NUMBER);
            input3.setHint("Branch ID (colombo or galle)");
            layout.addView(input1);
            layout.addView(input2);
            layout.addView(input3);
        } else {
            input1.setHint("Branch Name");
            input2.setHint("Location Address");
            input3.setHint("GPS Coordinates (Lat,Lng)");
            layout.addView(input1);
            layout.addView(input2);
            layout.addView(input3);
        }

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String val1 = input1.getText().toString().trim();
            String val2 = input2.getText().toString().trim();
            String val3 = input3.getText().toString().trim();

            if (TextUtils.isEmpty(val1) || TextUtils.isEmpty(val2)) {
                Toast.makeText(this, "Required fields are empty", Toast.LENGTH_SHORT).show();
                return;
            }

            saveNewRecord(val1, val2, val3);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
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
            record = new Technician(id, val1, val2, val3, true);
        } else if ("spare_parts".equalsIgnoreCase(manageType)) {
            int qty = Integer.parseInt(val2.isEmpty() ? "0" : val2);
            record = new SparePart(id, val1, qty, 1500.0, val3); // Default mock price 1500.0
        } else {
            record = new Branch(id, val1, val2, 6.92, 79.86); // Mock coords
        }

        mFirestore.collection(path).document(id)
                .set(record)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Record saved successfully!", Toast.LENGTH_SHORT).show();
                    loadData();
                });
    }

    private void showEditDialog(GenericCrudAdapter.CrudItem item) {
        if ("services".equalsIgnoreCase(manageType) && item.rawObject instanceof Service) {
            android.content.Intent intent = new android.content.Intent(this, AdminAddEditServiceActivity.class);
            intent.putExtra("SERVICE_ID", ((Service) item.rawObject).getServiceId());
            startActivity(intent);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Record");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        final EditText input1 = new EditText(this);
        final EditText input2 = new EditText(this);
        final EditText input3 = new EditText(this);

        layout.addView(input1);
        layout.addView(input2);
        layout.addView(input3);

        // Prepopulate based on type
        if ("services".equalsIgnoreCase(manageType) && item.rawObject instanceof Service) {
            Service s = (Service) item.rawObject;
            input1.setText(s.getName());
            input2.setText(s.getCategory());
            input3.setText(String.valueOf(s.getPrice()));
            input3.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else if ("technicians".equalsIgnoreCase(manageType) && item.rawObject instanceof Technician) {
            Technician t = (Technician) item.rawObject;
            input1.setText(t.getName());
            input2.setText(t.getSpecialization());
            input3.setText(t.getBranchId());
        } else if ("spare_parts".equalsIgnoreCase(manageType) && item.rawObject instanceof SparePart) {
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

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateRecord(String id, String val1, String val2, String val3) {
        String path = getCollectionPath();

        Object record;
        if ("services".equalsIgnoreCase(manageType)) {
            double price = Double.parseDouble(val3.isEmpty() ? "0" : val3);
            record = new Service(id, val1, val2, "General coursework service description", price, "1-2 Days", "");
            mDbHelper.insertOrUpdateService((Service) record);
        } else if ("technicians".equalsIgnoreCase(manageType)) {
            record = new Technician(id, val1, val2, val3, true);
        } else if ("spare_parts".equalsIgnoreCase(manageType)) {
            int qty = Integer.parseInt(val2.isEmpty() ? "0" : val2);
            record = new SparePart(id, val1, qty, 1500.0, val3); // Default mock price 1500.0
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

    private void showDeleteConfirmation(GenericCrudAdapter.CrudItem item) {
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
}
