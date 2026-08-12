package com.techfix.app.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.authentication.LoginActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.User;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileInitialsText, lblProfileName, lblProfileRole;
    private TextInputEditText profileNameEditText, profileEmailEditText, profilePhoneEditText, profileAddressEditText;
    private MaterialButton btnSaveProfile, btnLogout;
    private ProgressBar profileProgressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private FirebaseUser currentUser;
    private User cachedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        // Bind views
        profileInitialsText = findViewById(R.id.profileInitialsText);
        lblProfileName = findViewById(R.id.lblProfileName);
        lblProfileRole = findViewById(R.id.lblProfileRole);
        profileNameEditText = findViewById(R.id.profileNameEditText);
        profileEmailEditText = findViewById(R.id.profileEmailEditText);
        profilePhoneEditText = findViewById(R.id.profilePhoneEditText);
        profileAddressEditText = findViewById(R.id.profileAddressEditText);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
        profileProgressBar = findViewById(R.id.profileProgressBar);

        // Load profile data
        loadProfileData();

        // Listeners
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
        btnLogout.setOnClickListener(v -> handleLogout());

        setupBottomNavigation();
    }

    private void loadProfileData() {
        String uid = currentUser.getUid();
        
        // 1. Load from SQLite cache for instant loading
        cachedUser = mDbHelper.getUser(uid);
        if (cachedUser != null) {
            populateUI(cachedUser);
        } else {
            profileEmailEditText.setText(currentUser.getEmail());
        }

        // 2. Load from Firestore online database
        mFirestore.collection("users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            User onlineUser = doc.toObject(User.class);
                            if (onlineUser != null) {
                                mDbHelper.insertOrUpdateUser(onlineUser);
                                populateUI(onlineUser);
                            }
                        }
                    }
                });
    }

    private void populateUI(User user) {
        lblProfileName.setText(user.getName());
        lblProfileRole.setText(user.getRole() != null ? user.getRole() : "Customer");
        profileNameEditText.setText(user.getName());
        profileEmailEditText.setText(user.getEmail());
        profilePhoneEditText.setText(user.getPhone());
        profileAddressEditText.setText(user.getAddress());

        // Show/hide bottom bar depending on role
        boolean isAdmin = user.getRole() != null && user.getRole().equalsIgnoreCase("Admin");
        if (isAdmin) {
            findViewById(R.id.customerBottomNavigationCard).setVisibility(View.GONE);
            findViewById(R.id.adminBottomNavigationCard).setVisibility(View.VISIBLE);
            setupAdminBottomNavigation();
        } else {
            findViewById(R.id.customerBottomNavigationCard).setVisibility(View.VISIBLE);
            findViewById(R.id.adminBottomNavigationCard).setVisibility(View.GONE);
            setupBottomNavigation();
        }

        // Generate initials avatar
        if (!TextUtils.isEmpty(user.getName())) {
            String[] parts = user.getName().split(" ");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(parts.length, 2); i++) {
                if (!parts[i].isEmpty()) {
                    initials.append(parts[i].charAt(0));
                }
            }
            profileInitialsText.setText(initials.toString().toUpperCase());
        } else {
            profileInitialsText.setText("U");
        }
    }

    private void saveProfileChanges() {
        String name = profileNameEditText.getText().toString().trim();
        String phone = profilePhoneEditText.getText().toString().trim();
        String address = profileAddressEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            profileNameEditText.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            profilePhoneEditText.setError("Phone number is required");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            profileAddressEditText.setError("Address is required");
            return;
        }

        profileProgressBar.setVisibility(View.VISIBLE);
        btnSaveProfile.setEnabled(false);

        String uid = currentUser.getUid();
        String email = currentUser.getEmail();
        String role = (cachedUser != null && cachedUser.getRole() != null) ? cachedUser.getRole() : "Customer";
        
        User updatedUser = new User(uid, name, email, phone, role, address);

        // Update Firestore
        mFirestore.collection("users").document(uid)
                .set(updatedUser)
                .addOnCompleteListener(task -> {
                    profileProgressBar.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Update SQLite cache
                        mDbHelper.insertOrUpdateUser(updatedUser);
                        lblProfileName.setText(name);
                        
                        // Update avatar initials
                        populateUI(updatedUser);
                        
                        Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleLogout() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        // Wire listeners for bottom bar items
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, CustomerDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navTrack).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, TrackRepairActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navBook).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, BookAppointmentActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navHistory).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, RepairHistoryActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            // Already on Profile
            Toast.makeText(ProfileActivity.this, "Already on Profile screen", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAdminBottomNavigation() {
        findViewById(R.id.navAdminHome).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, com.techfix.app.admin.AdminDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.navAdminTechs).setOnClickListener(v -> launchAdminCrud("technicians"));
        findViewById(R.id.navAdminServices).setOnClickListener(v -> launchAdminCrud("services"));

        findViewById(R.id.navAdminProfile).setOnClickListener(v -> {
            // Already on Profile
            Toast.makeText(ProfileActivity.this, "Already on Profile screen", Toast.LENGTH_SHORT).show();
        });
    }

    private void launchAdminCrud(String type) {
        Intent intent = new Intent(ProfileActivity.this, com.techfix.app.admin.AdminManageDataActivity.class);
        intent.putExtra("MANAGE_TYPE", type);
        startActivity(intent);
        finish();
    }
}
