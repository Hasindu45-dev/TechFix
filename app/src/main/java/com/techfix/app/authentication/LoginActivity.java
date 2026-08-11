package com.techfix.app.authentication;

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
import com.techfix.app.admin.AdminDashboardActivity;
import com.techfix.app.customer.CustomerDashboardActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.User;
import com.techfix.app.technician.TechnicianDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton signInButton;
    private TextView registerLink;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        // Bind views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signInButton = findViewById(R.id.signInButton);
        registerLink = findViewById(R.id.registerLink);
        progressBar = findViewById(R.id.loginProgressBar);

        signInButton.setOnClickListener(v -> handleLogin());

        registerLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        signInButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            fetchUserRoleAndRedirect(firebaseUser.getUid());
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        signInButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserRoleAndRedirect(String userId) {
        // Try local cache first in case of offline login redirection
        User cachedUser = mDbHelper.getUser(userId);

        // Always attempt Firestore fetch online
        mFirestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    signInButton.setEnabled(true);

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                // Cache User to SQLite database
                                mDbHelper.insertOrUpdateUser(user);

                                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                navigateToDashboard(user.getRole());
                                return;
                            }
                        }
                    }

                    // Fallback to SQLite cached user if Firestore is unavailable or document is missing
                    if (cachedUser != null) {
                        Toast.makeText(LoginActivity.this, "Login Successful (Cached Profile)", Toast.LENGTH_SHORT).show();
                        navigateToDashboard(cachedUser.getRole());
                    } else {
                        // Default fallback
                        Toast.makeText(LoginActivity.this, "Profile not found, logging in as Customer", Toast.LENGTH_LONG).show();
                        navigateToDashboard("Customer");
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("Admin".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else if ("Technician".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, TechnicianDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, CustomerDashboardActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
