package com.techfix.app.technician;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.techfix.app.R;
import com.techfix.app.adapters.TechRequiredPartsAdapter;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.RequiredPart;
import com.techfix.app.models.Service;
import com.techfix.app.models.SparePart;
import com.techfix.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class TechnicianJobDetailActivity extends AppCompatActivity {

    private TextView detailJobId, detailDeviceModel, detailServiceName, detailCustName, detailCustPhone, detailCustAddress, detailProblemDesc, lblAttachedPhoto;
    private ImageView detailDeviceImage;
    private MaterialCardView detailImageCard;
    private AutoCompleteTextView statusAutoComplete;
    private MaterialButton btnSaveProgress, btnMarkPartUsed;
    private ProgressBar progressBar;
    private RecyclerView techRequiredPartsRecyclerView;

    private TechRequiredPartsAdapter techRequiredPartsAdapter;
    private List<SparePart> branchPartsList = new ArrayList<>();
    private Service linkedService;

    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String appointmentId;
    private Appointment appointment;
    private List<Service> servicesList;

    private final String[] PROGRESS_STATUSES = {
        "Device Received",
        "Diagnosis Completed",
        "Repair Started",
        "Waiting for Spare Parts",
        "Repair Completed"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_job_detail);

        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);
        servicesList = mDbHelper.getAllServices();

        appointmentId = getIntent().getStringExtra("APPOINTMENT_ID");

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Bind Views
        detailJobId = findViewById(R.id.detailJobId);
        detailDeviceModel = findViewById(R.id.detailDeviceModel);
        detailServiceName = findViewById(R.id.detailServiceName);
        detailCustName = findViewById(R.id.detailCustName);
        detailCustPhone = findViewById(R.id.detailCustPhone);
        detailCustAddress = findViewById(R.id.detailCustAddress);
        detailProblemDesc = findViewById(R.id.detailProblemDesc);
        detailDeviceImage = findViewById(R.id.detailDeviceImage);
        lblAttachedPhoto = findViewById(R.id.lblAttachedPhoto);
        detailImageCard = findViewById(R.id.detailImageCard);
        statusAutoComplete = findViewById(R.id.statusAutoComplete);
        btnSaveProgress = findViewById(R.id.btnSaveProgress);
        progressBar = findViewById(R.id.detailProgressBar);
        techRequiredPartsRecyclerView = findViewById(R.id.techRequiredPartsRecyclerView);
        btnMarkPartUsed = findViewById(R.id.btnMarkPartUsed);

        techRequiredPartsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        techRequiredPartsAdapter = new TechRequiredPartsAdapter();
        techRequiredPartsRecyclerView.setAdapter(techRequiredPartsAdapter);

        // Setup progress spinner dropdown
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, PROGRESS_STATUSES);
        statusAutoComplete.setAdapter(statusAdapter);

        loadJobDetails();

        btnSaveProgress.setOnClickListener(v -> handleSaveProgress());
        btnMarkPartUsed.setOnClickListener(v -> handleMarkPartsUsed());
    }

    private void loadJobDetails() {
        // Read local SQLite cache first
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

        // Online sync
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
    }

    private void populateUI() {
        detailJobId.setText("TICKET ID: #" + appointment.getAppointmentId().substring(0, 8).toUpperCase());
        detailDeviceModel.setText(appointment.getDeviceModel());
        detailProblemDesc.setText(appointment.getProblemDescription());

        // Find service name
        String serviceName = "General Repair";
        for (Service s : servicesList) {
            if (s.getServiceId().equalsIgnoreCase(appointment.getServiceId())) {
                serviceName = s.getName();
                break;
            }
        }
        detailServiceName.setText(serviceName);

        // Set default dropdown matching current status
        boolean foundStatus = false;
        for (String status : PROGRESS_STATUSES) {
            if (status.equalsIgnoreCase(appointment.getStatus())) {
                statusAutoComplete.setText(status, false);
                foundStatus = true;
                break;
            }
        }
        if (!foundStatus) {
            statusAutoComplete.setText(PROGRESS_STATUSES[0], false);
        }

        if (appointment.getImageURL() != null && !appointment.getImageURL().isEmpty()) {
            lblAttachedPhoto.setVisibility(View.VISIBLE);
            detailImageCard.setVisibility(View.VISIBLE);
            loadImageFromUrl(appointment.getImageURL(), detailDeviceImage);
        } else {
            lblAttachedPhoto.setVisibility(View.GONE);
            detailImageCard.setVisibility(View.GONE);
        }

        // Load customer contact details
        loadCustomerContact(appointment.getCustomerId());

        // Load service required parts and branch spare parts
        mFirestore.collection("services").document(appointment.getServiceId()).get()
                .addOnSuccessListener(serviceDoc -> {
                    if (serviceDoc.exists()) {
                        linkedService = serviceDoc.toObject(Service.class);
                    }
                    loadBranchSpareParts();
                })
                .addOnFailureListener(e -> {
                    // Fallback to local cache
                    for (Service s : servicesList) {
                        if (s.getServiceId().equalsIgnoreCase(appointment.getServiceId())) {
                            linkedService = s;
                            break;
                        }
                    }
                    loadBranchSpareParts();
                });
    }

    private void loadBranchSpareParts() {
        if (appointment == null) return;
        
        String branchId = "galle".equalsIgnoreCase(appointment.getAssignedBranch()) || "TechFix Galle".equalsIgnoreCase(appointment.getAssignedBranch()) ? "galle" : "colombo";

        // Fetch online first
        mFirestore.collection("spareParts").whereEqualTo("branchId", branchId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    branchPartsList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SparePart sp = doc.toObject(SparePart.class);
                        if (sp != null) {
                            branchPartsList.add(sp);
                        }
                    }
                    updatePartsUI();
                })
                .addOnFailureListener(e -> {
                    // Fallback to SQLite cache
                    branchPartsList = mDbHelper.getSparePartsForBranch(branchId);
                    updatePartsUI();
                });
    }

    private void updatePartsUI() {
        if (linkedService == null || linkedService.getRequiredParts() == null || linkedService.getRequiredParts().isEmpty()) {
            findViewById(R.id.lblTechRequiredParts).setVisibility(View.GONE);
            techRequiredPartsRecyclerView.setVisibility(View.GONE);
            btnMarkPartUsed.setVisibility(View.GONE);
            return;
        }

        findViewById(R.id.lblTechRequiredParts).setVisibility(View.VISIBLE);
        techRequiredPartsRecyclerView.setVisibility(View.VISIBLE);
        btnMarkPartUsed.setVisibility(View.VISIBLE);

        techRequiredPartsAdapter.setData(linkedService.getRequiredParts(), branchPartsList);

        // Check if already completed/used
        if ("Repair Completed".equalsIgnoreCase(appointment.getStatus())
                || "Ready for Pickup".equalsIgnoreCase(appointment.getStatus())
                || "Completed".equalsIgnoreCase(appointment.getStatus())) {
            btnMarkPartUsed.setEnabled(false);
            btnMarkPartUsed.setText("Parts Already Marked As Used");
        } else {
            // Check if any part is out of stock (available < required)
            boolean hasShortage = false;
            for (RequiredPart req : linkedService.getRequiredParts()) {
                int available = 0;
                for (SparePart sp : branchPartsList) {
                    if (sp.getName() != null && sp.getName().equalsIgnoreCase(req.getPartName())) {
                        available = sp.getQuantity();
                        break;
                    }
                }
                if (available < req.getQuantity()) {
                    hasShortage = true;
                    break;
                }
            }

            if (hasShortage) {
                btnMarkPartUsed.setEnabled(false);
                btnMarkPartUsed.setText("Waiting for Spare Parts (Insufficient Stock)");
            } else {
                btnMarkPartUsed.setEnabled(true);
                btnMarkPartUsed.setText("Mark Parts As Used");
            }
        }
    }

    private void handleMarkPartsUsed() {
        if (linkedService == null || appointment == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnMarkPartUsed.setEnabled(false);

        // 1. Double check stock validity before executing writes
        boolean stockOk = true;
        for (RequiredPart req : linkedService.getRequiredParts()) {
            int available = 0;
            for (SparePart sp : branchPartsList) {
                if (sp.getName() != null && sp.getName().equalsIgnoreCase(req.getPartName())) {
                    available = sp.getQuantity();
                    break;
                }
            }
            if (available < req.getQuantity()) {
                stockOk = false;
                break;
            }
        }

        if (!stockOk) {
            progressBar.setVisibility(View.GONE);
            btnMarkPartUsed.setEnabled(true);
            Toast.makeText(this, "Insufficient spare-part stock.", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Perform safe decrement writes using Firestore WriteBatch
        WriteBatch batch = mFirestore.batch();
        for (RequiredPart req : linkedService.getRequiredParts()) {
            for (SparePart sp : branchPartsList) {
                if (sp.getName() != null && sp.getName().equalsIgnoreCase(req.getPartName())) {
                    int newQty = sp.getQuantity() - req.getQuantity();
                    sp.setQuantity(newQty);
                    batch.update(mFirestore.collection("spareParts").document(sp.getPartId()), "quantity", newQty);
                    mDbHelper.insertOrUpdateSparePart(sp);
                }
            }
        }

        // 3. Update appointment status to "Repair Completed"
        appointment.setStatus("Repair Completed");
        batch.update(mFirestore.collection("appointments").document(appointmentId), "status", "Repair Completed");
        mDbHelper.insertOrUpdateAppointment(appointment);

        batch.commit().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(this, "Spare parts marked as used and inventory updated!", Toast.LENGTH_SHORT).show();
                // Update dropdown text matching current status
                statusAutoComplete.setText("Repair Completed", false);
                updatePartsUI();
            } else {
                btnMarkPartUsed.setEnabled(true);
                Toast.makeText(this, "Failed to update inventory: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadImageFromUrl(String url, ImageView imageView) {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            try {
                java.io.InputStream in = new java.net.URL(url).openStream();
                android.graphics.Bitmap image = android.graphics.BitmapFactory.decodeStream(in);
                handler.post(() -> {
                    imageView.setImageBitmap(image);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadCustomerContact(String customerId) {
        // Local SQLite cache check
        User cachedUser = mDbHelper.getUser(customerId);
        if (cachedUser != null) {
            detailCustName.setText(cachedUser.getName());
            detailCustPhone.setText("Phone: " + cachedUser.getPhone());
            detailCustAddress.setText("Address: " + cachedUser.getAddress());
        }

        // Firestore online fetch
        mFirestore.collection("users").document(customerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                mDbHelper.insertOrUpdateUser(user);
                                detailCustName.setText(user.getName());
                                detailCustPhone.setText("Phone: " + user.getPhone());
                                detailCustAddress.setText("Address: " + user.getAddress());
                            }
                        }
                    }
                });
    }

    private void handleSaveProgress() {
        String newStatus = statusAutoComplete.getText().toString();
        if (TextUtils.isEmpty(newStatus) || appointment == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnSaveProgress.setEnabled(false);

        // Check if updating to a completed status:
        boolean isCompleting = "Repair Completed".equalsIgnoreCase(newStatus) || "Completed".equalsIgnoreCase(newStatus);
        boolean partsNeedConsumption = btnMarkPartUsed.getVisibility() == View.VISIBLE && btnMarkPartUsed.isEnabled();

        if (isCompleting && partsNeedConsumption) {
            // Check if stock is sufficient
            boolean stockOk = true;
            for (RequiredPart req : linkedService.getRequiredParts()) {
                int available = 0;
                for (SparePart sp : branchPartsList) {
                    if (sp.getName() != null && sp.getName().equalsIgnoreCase(req.getPartName())) {
                        available = sp.getQuantity();
                        break;
                    }
                }
                if (available < req.getQuantity()) {
                    stockOk = false;
                    break;
                }
            }

            if (!stockOk) {
                progressBar.setVisibility(View.GONE);
                btnSaveProgress.setEnabled(true);
                Toast.makeText(this, "Cannot complete: Insufficient spare-part stock.", Toast.LENGTH_LONG).show();
                return;
            }

            // Decrement stock in batch with status update
            WriteBatch batch = mFirestore.batch();
            for (RequiredPart req : linkedService.getRequiredParts()) {
                for (SparePart sp : branchPartsList) {
                    if (sp.getName() != null && sp.getName().equalsIgnoreCase(req.getPartName())) {
                        int newQty = sp.getQuantity() - req.getQuantity();
                        sp.setQuantity(newQty);
                        batch.update(mFirestore.collection("spareParts").document(sp.getPartId()), "quantity", newQty);
                        mDbHelper.insertOrUpdateSparePart(sp);
                    }
                }
            }

            appointment.setStatus(newStatus);
            batch.update(mFirestore.collection("appointments").document(appointmentId), "status", newStatus);
            mDbHelper.insertOrUpdateAppointment(appointment);

            // Sync to SQLite history
            double cost = 3000.0;
            String serviceName = "General Repair";
            for (Service s : servicesList) {
                if (s.getServiceId().equalsIgnoreCase(appointment.getServiceId())) {
                    cost = s.getPrice();
                    serviceName = s.getName();
                    break;
                }
            }
            mDbHelper.insertOrUpdateHistory(
                    appointmentId,
                    appointmentId,
                    appointment.getCustomerId(),
                    appointment.getDeviceModel(),
                    serviceName,
                    appointment.getAssignedBranch(),
                    appointment.getDate(),
                    cost,
                    "Pending",
                    newStatus
            );

            batch.commit().addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                btnSaveProgress.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(TechnicianJobDetailActivity.this, "Job status updated and spare parts deducted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(TechnicianJobDetailActivity.this, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            // Regular status update
            mFirestore.collection("appointments").document(appointmentId)
                    .update("status", newStatus)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        btnSaveProgress.setEnabled(true);

                        if (task.isSuccessful()) {
                            appointment.setStatus(newStatus);
                            mDbHelper.insertOrUpdateAppointment(appointment);

                            if ("Repair Completed".equalsIgnoreCase(newStatus)) {
                                double cost = 3000.0;
                                String serviceName = "General Repair";
                                for (Service s : servicesList) {
                                    if (s.getServiceId().equalsIgnoreCase(appointment.getServiceId())) {
                                        cost = s.getPrice();
                                        serviceName = s.getName();
                                        break;
                                    }
                                }
                                mDbHelper.insertOrUpdateHistory(
                                        appointmentId,
                                        appointmentId,
                                        appointment.getCustomerId(),
                                        appointment.getDeviceModel(),
                                        serviceName,
                                        appointment.getAssignedBranch(),
                                        appointment.getDate(),
                                        cost,
                                        "Pending",
                                        newStatus
                                );
                            }

                            Toast.makeText(TechnicianJobDetailActivity.this, "Job status updated successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(TechnicianJobDetailActivity.this, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}
