package com.techfix.app.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.admin.AdminDashboardActivity;
import com.techfix.app.customer.CustomerDashboardActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Technician;
import com.techfix.app.models.User;
import com.techfix.app.technician.TechnicianDashboardActivity;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameEditText, emailEditText, phoneEditText, addressEditText, passwordEditText, confirmPasswordEditText;
    private AutoCompleteTextView roleAutoComplete;
    private MaterialButton registerButton;
    private TextView loginLink;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private final String[] ROLES = {"Customer", "Technician", "Admin"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        // Bind views
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailRegEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        roleAutoComplete = findViewById(R.id.roleAutoComplete);
        passwordEditText = findViewById(R.id.passwordRegEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);
        progressBar = findViewById(R.id.registerProgressBar);

        // Setup dropdown adapter for roles
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ROLES);
        roleAutoComplete.setAdapter(adapter);
        // Default selection
        roleAutoComplete.setText(ROLES[0], false);

        registerButton.setOnClickListener(v -> handleRegistration());

        loginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegistration() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String role = roleAutoComplete.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            phoneEditText.setError("Phone number is required");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            addressEditText.setError("Address is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        registerButton.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            User user = new User(uid, name, email, phone, role, address);

                            // Save user to Firestore
                            mFirestore.collection("users").document(uid)
                                    .set(user)
                                    .addOnCompleteListener(dbTask -> {
                                        progressBar.setVisibility(View.GONE);
                                        registerButton.setEnabled(true);
                                        if (dbTask.isSuccessful()) {
                                            // Cache user to local SQLite
                                            mDbHelper.insertOrUpdateUser(user);

                                            // If registering as a technician, auto-create technician collection document
                                            if ("Technician".equalsIgnoreCase(role)) {
                                                String branchId = (address != null && address.toLowerCase().contains("galle")) ? "galle" : "colombo";
                                                Technician tech = new Technician(uid, name, "General", branchId, true);
                                                mFirestore.collection("technicians").document(uid).set(tech);
                                            }

                                            Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                            navigateToDashboard(role);
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Database error: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        registerButton.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("Admin".equalsIgnoreCase(role)) {
            intent = new Intent(RegisterActivity.this, AdminDashboardActivity.class);
        } else if ("Technician".equalsIgnoreCase(role)) {
            intent = new Intent(RegisterActivity.this, TechnicianDashboardActivity.class);
        } else {
            intent = new Intent(RegisterActivity.this, CustomerDashboardActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
