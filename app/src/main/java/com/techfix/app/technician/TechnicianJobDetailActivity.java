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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;
import com.techfix.app.models.User;

import java.util.List;

public class TechnicianJobDetailActivity extends AppCompatActivity {

    private TextView detailJobId, detailDeviceModel, detailServiceName, detailCustName, detailCustPhone, detailCustAddress, detailProblemDesc, lblAttachedPhoto;
    private ImageView detailDeviceImage;
    private MaterialCardView detailImageCard;
    private AutoCompleteTextView statusAutoComplete;
    private MaterialButton btnSaveProgress;
    private ProgressBar progressBar;

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

        // Setup progress spinner dropdown
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, PROGRESS_STATUSES);
        statusAutoComplete.setAdapter(statusAdapter);

        loadJobDetails();

        btnSaveProgress.setOnClickListener(v -> handleSaveProgress());
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

        // Update status online in Firestore
        mFirestore.collection("appointments").document(appointmentId)
                .update("status", newStatus)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveProgress.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Update local SQLite cache
                        appointment.setStatus(newStatus);
                        mDbHelper.insertOrUpdateAppointment(appointment);

                        // If completed, sync to SQLite history table for offline cache listing
                        if ("Repair Completed".equalsIgnoreCase(newStatus)) {
                            // Find matched service cost
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
                                    "Pending", // Payment remains Pending until customer simulation paid
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
