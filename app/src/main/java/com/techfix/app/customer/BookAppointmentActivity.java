package com.techfix.app.customer;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;
import com.techfix.app.utils.BranchAssignmentUtility;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class BookAppointmentActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 2001;
    private static final int CAMERA_REQUEST_CODE = 2002;
    private static final int LOCATION_PERMISSION_CODE = 2003;

    private AutoCompleteTextView categoryAutoComplete, serviceAutoComplete;
    private TextInputLayout customServiceInputLayout;
    private TextInputEditText modelEditText, dateEditText, descEditText, customServiceEditText;
    private MaterialButton btnTakePhoto, btnBookAppointment;
    private ImageView deviceImagePreview;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseStorage mStorage;
    private DatabaseHelper mDbHelper;
    private FusedLocationProviderClient fusedLocationClient;

    private final String[] CATEGORIES = {"Computer", "Mobile"};
    private List<Service> servicesList = new ArrayList<>();
    private List<String> filteredServiceNames = new ArrayList<>();
    private List<Service> filteredServices = new ArrayList<>();

    private Bitmap capturedBitmap;
    private double customerLat = 6.9271; // Default fallback to Colombo coordinates
    private double customerLng = 79.8612;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mDbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Bind Views
        categoryAutoComplete = findViewById(R.id.categoryAutoComplete);
        serviceAutoComplete = findViewById(R.id.serviceAutoComplete);
        customServiceInputLayout = findViewById(R.id.customServiceInputLayout);
        customServiceEditText = findViewById(R.id.customServiceEditText);
        modelEditText = findViewById(R.id.modelEditText);
        dateEditText = findViewById(R.id.dateEditText);
        descEditText = findViewById(R.id.descEditText);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnBookAppointment = findViewById(R.id.btnBookAppointment);
        deviceImagePreview = findViewById(R.id.deviceImagePreview);
        progressBar = findViewById(R.id.bookingProgressBar);

        // Listen for service selection change to show/hide custom service field
        serviceAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selected = filteredServiceNames.get(position);
            if ("Other".equalsIgnoreCase(selected)) {
                customServiceInputLayout.setVisibility(View.VISIBLE);
            } else {
                customServiceInputLayout.setVisibility(View.GONE);
                customServiceEditText.setText("");
            }
        });

        // Setup Categories dropdown
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        categoryAutoComplete.setAdapter(catAdapter);

        // Load all services from cache
        servicesList = mDbHelper.getAllServices();

        // Listen for category selection change to update specific services dropdown
        categoryAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = CATEGORIES[position];
            updateServiceDropdown(selectedCategory);
        });

        // Date picker dialog
        dateEditText.setOnClickListener(v -> showDatePicker());

        // Camera trigger
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndLaunch());

        // Booking trigger
        btnBookAppointment.setOnClickListener(v -> checkLocationAndSubmit());

        // Handle prefilled service from Dashboard browsing
        String prefilledServiceId = getIntent().getStringExtra("PREFILLED_SERVICE_ID");
        if (prefilledServiceId != null) {
            prefillService(prefilledServiceId);
        }

        // Request Location permission immediately to start tracking location
        requestLocationPermission();
    }

    private void updateServiceDropdown(String category) {
        filteredServices.clear();
        filteredServiceNames.clear();

        for (Service s : servicesList) {
            if (s.getCategory().equalsIgnoreCase(category)) {
                filteredServices.add(s);
                filteredServiceNames.add(s.getName());
            }
        }

        // Dynamically append "Other" option for custom user-entered services
        filteredServiceNames.add("Other");
        filteredServices.add(new Service("other", "Other", category, "Custom user-defined service", 0.0, "Variable", ""));

        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, filteredServiceNames);
        serviceAutoComplete.setAdapter(serviceAdapter);
        
        if (customServiceInputLayout != null) {
            customServiceInputLayout.setVisibility(View.GONE);
        }
        if (customServiceEditText != null) {
            customServiceEditText.setText("");
        }

        if (!filteredServiceNames.isEmpty()) {
            serviceAutoComplete.setText(filteredServiceNames.get(0), false);
        } else {
            serviceAutoComplete.setText("", false);
        }
    }

    private void prefillService(String serviceId) {
        for (Service s : servicesList) {
            if (s.getServiceId().equals(serviceId)) {
                categoryAutoComplete.setText(s.getCategory(), false);
                updateServiceDropdown(s.getCategory());
                serviceAutoComplete.setText(s.getName(), false);
                break;
            }
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String dateStr = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    dateEditText.setText(dateStr);
                }, year, month, day);
        
        // Prevent booking in the past
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            launchCamera();
        }
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Camera app not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            acquireLocation();
        }
    }

    private void acquireLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                customerLat = location.getLatitude();
                customerLng = location.getLongitude();
            }
        });
    }

    private void checkLocationAndSubmit() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    customerLat = location.getLatitude();
                    customerLng = location.getLongitude();
                }
                submitAppointment();
            }).addOnFailureListener(e -> submitAppointment());
        } else {
            submitAppointment();
        }
    }

    private void submitAppointment() {
        String category = categoryAutoComplete.getText().toString();
        String serviceName = serviceAutoComplete.getText().toString();
        String model = modelEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        String problemDesc = descEditText.getText().toString().trim();

        if (TextUtils.isEmpty(category)) {
            categoryAutoComplete.setError("Please select a category");
            return;
        }
        if (TextUtils.isEmpty(serviceName)) {
            serviceAutoComplete.setError("Please select a service");
            return;
        }

        // If "Other" is chosen, validate custom title and override serviceName
        if ("Other".equalsIgnoreCase(serviceName)) {
            String customTitle = customServiceEditText.getText().toString().trim();
            if (TextUtils.isEmpty(customTitle)) {
                customServiceEditText.setError("Specify custom service title");
                return;
            }
            serviceName = customTitle;
        }

        if (TextUtils.isEmpty(model)) {
            modelEditText.setError("Device model is required");
            return;
        }
        if (TextUtils.isEmpty(date)) {
            dateEditText.setError("Preferred date is required");
            return;
        }
        if (TextUtils.isEmpty(problemDesc)) {
            descEditText.setError("Problem description is required");
            return;
        }

        // Find Service ID matching selected name
        String serviceId = "s1";
        if ("Other".equalsIgnoreCase(serviceAutoComplete.getText().toString())) {
            serviceId = "other";
        } else {
            for (Service s : servicesList) {
                if (s.getName().equalsIgnoreCase(serviceName)) {
                    serviceId = s.getServiceId();
                    break;
                }
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        btnBookAppointment.setEnabled(false);

        final String appointmentId = UUID.randomUUID().toString();
        final String finalServiceId = serviceId;
        final String finalCategory = category;
        final String finalServiceName = serviceName;

        if (capturedBitmap != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            capturedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] data = baos.toByteArray();

            StorageReference imageRef = mStorage.getReference().child("device_images/" + appointmentId + ".jpg");
            UploadTask uploadTask = imageRef.putBytes(data);

            uploadTask.addOnFailureListener(exception -> {
                progressBar.setVisibility(View.GONE);
                btnBookAppointment.setEnabled(true);
                Toast.makeText(BookAppointmentActivity.this, "Image upload failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }).addOnSuccessListener(taskSnapshot -> {
                imageRef.getDownloadUrl().addOnCompleteListener(urlTask -> {
                    if (urlTask.isSuccessful() && urlTask.getResult() != null) {
                        String downloadUrl = urlTask.getResult().toString();
                        runAssignmentAndSave(appointmentId, finalServiceId, model, date, problemDesc, downloadUrl, finalCategory, finalServiceName);
                    } else {
                        runAssignmentAndSave(appointmentId, finalServiceId, model, date, problemDesc, "", finalCategory, finalServiceName);
                    }
                });
            });
        } else {
            runAssignmentAndSave(appointmentId, finalServiceId, model, date, problemDesc, "", finalCategory, finalServiceName);
        }
    }


    private void runAssignmentAndSave(
            String appointmentId,
            String serviceId,
            String model,
            String date,
            String problemDesc,
            String imageUrl,
            String category,
            String serviceName) {

        String customerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

        // Execute Nearest Available Branch Auto-Assignment Algorithm
        BranchAssignmentUtility.assignBranch(customerLat, customerLng, category, serviceName, 
                new BranchAssignmentUtility.OnAssignmentCompleteListener() {
                    @Override
                    public void onAssignmentComplete(com.techfix.app.models.Branch assignedBranch, double distanceKm, String assignedTechnicianName, String reason) {
                        
                        if (assignedBranch == null) {
                            progressBar.setVisibility(View.GONE);
                            btnBookAppointment.setEnabled(true);
                            new AlertDialog.Builder(BookAppointmentActivity.this)
                                    .setTitle("Booking Unavailable")
                                    .setMessage("No suitable branch is currently available with the required spare parts or technicians. Your booking cannot be placed at this time.")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }

                        String branchName = assignedBranch.getName();
                        String status = "Request Submitted";

                        Appointment appt = new Appointment(
                                appointmentId,
                                customerId,
                                serviceId,
                                model,
                                problemDesc,
                                imageUrl,
                                branchName,
                                "Unassigned",
                                status,
                                date
                        );

                        // Save to Firebase Firestore
                        mFirestore.collection("appointments").document(appointmentId)
                                .set(appt)
                                .addOnCompleteListener(task -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnBookAppointment.setEnabled(true);

                                    if (task.isSuccessful()) {
                                        // Cache to local SQLite
                                        mDbHelper.insertOrUpdateAppointment(appt);
                                        
                                        // Display Success Dialog
                                        showSuccessDialog(branchName, distanceKm, reason);
                                    } else {
                                        Toast.makeText(BookAppointmentActivity.this, "Booking failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onAssignmentFailed(String errorMsg) {
                        progressBar.setVisibility(View.GONE);
                        btnBookAppointment.setEnabled(true);
                        Toast.makeText(BookAppointmentActivity.this, "Assignment failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showSuccessDialog(String branchName, double distanceKm, String reason) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if ("Unassigned".equals(branchName)) {
            builder.setTitle("Appointment Submitted")
                    .setMessage("No suitable branch is currently available. Your repair request has been submitted and is waiting for required resources.");
        } else {
            builder.setTitle("Appointment Placed Successfully!")
                    .setMessage("Your request has been assigned to our closest available branch.\n\n"
                            + "Assigned Branch: " + branchName + "\n"
                            + "Estimated Distance: " + String.format("%.1f km", distanceKm) + "\n"
                            + "Technician Assignment: Pending Allocation by Admin\n\n"
                            + "Assignment Rationale:\n" + reason);
        }
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            finish();
        })
        .setCancelable(false)
        .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                capturedBitmap = (Bitmap) extras.get("data");
                deviceImagePreview.setImageBitmap(capturedBitmap);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                acquireLocation();
            }
        }
    }
}
