package com.techfix.app.technician;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.authentication.LoginActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.User;

public class TechnicianDashboardActivity extends AppCompatActivity {

    private TextView welcomeUserText;
    private MaterialButton logoutButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_dashboard);

        welcomeUserText = findViewById(R.id.welcomeUserText);
        logoutButton = findViewById(R.id.logoutButton);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        loadUserData(currentUser.getUid());

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(TechnicianDashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });
    }

    private void loadUserData(String userId) {
        // Try local SQLite cache first
        User cachedUser = mDbHelper.getUser(userId);
        if (cachedUser != null) {
            welcomeUserText.setText("Welcome, " + cachedUser.getName() + " (Technician)!");
        }

        // Always sync with Firestore online
        mFirestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                // Cache to SQLite
                                mDbHelper.insertOrUpdateUser(user);
                                // Update UI
                                welcomeUserText.setText("Welcome, " + user.getName() + " (Technician)!");
                            }
                        }
                    }
                });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(TechnicianDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
