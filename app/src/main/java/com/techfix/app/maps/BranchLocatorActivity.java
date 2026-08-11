package com.techfix.app.maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Branch;

import java.util.ArrayList;
import java.util.List;

public class BranchLocatorActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private TextView locationStatusText, colomboDistanceText, galleDistanceText;
    private FloatingActionButton btnBack, btnCurrentLocation;

    private LatLng userLatLng;
    private final List<Branch> branchList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_locator);

        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationStatusText = findViewById(R.id.locationStatusText);
        colomboDistanceText = findViewById(R.id.colomboDistanceText);
        galleDistanceText = findViewById(R.id.galleDistanceText);
        btnBack = findViewById(R.id.btnBack);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        btnBack.setOnClickListener(v -> finish());
        btnCurrentLocation.setOnClickListener(v -> moveToCurrentLocation());

        // Initialize SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        checkLocationPermissionAndGetLocation();
        loadBranches();
    }

    private void checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);
        locationStatusText.setText("Getting current GPS location...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                locationStatusText.setText("Current Location acquired.");
                
                // Add marker for user
                mMap.addMarker(new MarkerOptions()
                        .position(userLatLng)
                        .title("Your Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f));
                calculateAllDistances();
            } else {
                locationStatusText.setText("Failed to acquire GPS location. Mocking location...");
                // Default to Colombo center if GPS is null on emulator
                userLatLng = new LatLng(6.9271, 79.8612);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f));
                calculateAllDistances();
            }
        });
    }

    private void moveToCurrentLocation() {
        if (userLatLng != null && mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f));
        } else {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBranches() {
        // Load cached branches first
        List<Branch> cached = mDbHelper.getAllBranches();
        if (!cached.isEmpty()) {
            branchList.clear();
            branchList.addAll(cached);
            plotBranchMarkers();
        }

        mFirestore.collection("branches").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            seedBranches();
                        } else {
                            branchList.clear();
                            for (DocumentSnapshot doc : task.getResult()) {
                                Branch branch = doc.toObject(Branch.class);
                                if (branch != null) {
                                    branchList.add(branch);
                                    mDbHelper.insertOrUpdateBranch(branch);
                                }
                            }
                            plotBranchMarkers();
                        }
                    }
                });
    }

    private void seedBranches() {
        List<Branch> defaults = new ArrayList<>();
        defaults.add(new Branch("colombo", "TechFix Colombo", "123 Galle Road, Colombo 03", 6.9271, 79.8612));
        defaults.add(new Branch("galle", "TechFix Galle", "45 Marine Drive, Galle", 6.0329, 80.2168));

        for (Branch b : defaults) {
            mFirestore.collection("branches").document(b.getBranchId())
                    .set(b)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            mDbHelper.insertOrUpdateBranch(b);
                            if (!branchList.contains(b)) {
                                branchList.add(b);
                                plotBranchMarkers();
                            }
                        }
                    });
        }
    }

    private void plotBranchMarkers() {
        if (mMap == null) return;
        mMap.clear();

        // Restore user marker if we have it
        if (userLatLng != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(userLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        }

        for (Branch branch : branchList) {
            LatLng pos = new LatLng(branch.getLatitude(), branch.getLongitude());
            mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(branch.getName())
                    .snippet(branch.getAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }

        calculateAllDistances();
    }

    private void calculateAllDistances() {
        if (userLatLng == null || branchList.isEmpty()) return;

        for (Branch branch : branchList) {
            float[] results = new float[1];
            Location.distanceBetween(userLatLng.latitude, userLatLng.longitude,
                    branch.getLatitude(), branch.getLongitude(), results);
            float distanceMeters = results[0];
            String distanceStr = String.format("%.1f km", distanceMeters / 1000.0);

            if ("colombo".equalsIgnoreCase(branch.getBranchId())) {
                colomboDistanceText.setText(distanceStr);
            } else if ("galle".equalsIgnoreCase(branch.getBranchId())) {
                galleDistanceText.setText(distanceStr);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Toast.makeText(this, "GPS permission denied. Distance calculation disabled.", Toast.LENGTH_LONG).show();
                locationStatusText.setText("Permission denied. Showing default Colombo branch.");
                // Show default zoom on Colombo
                userLatLng = new LatLng(6.9271, 79.8612);
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10f));
                }
            }
        }
    }
}
