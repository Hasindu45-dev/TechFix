package com.techfix.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.admin.AdminDashboardActivity;
import com.techfix.app.authentication.LoginActivity;
import com.techfix.app.customer.CustomerDashboardActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.User;
import com.techfix.app.technician.TechnicianDashboardActivity;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkUserRoleAndRedirect(currentUser);
        } else {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }

    private void checkUserRoleAndRedirect(FirebaseUser currentUser) {
        String userId = currentUser.getUid();
        User cachedUser = mDbHelper.getUser(userId);

        mFirestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    String role = "Customer";
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null) {
                            mDbHelper.insertOrUpdateUser(user);
                            role = user.getRole();
                        }
                    } else if (cachedUser != null && cachedUser.getRole() != null) {
                        role = cachedUser.getRole();
                    }

                    final String userRole = (role != null) ? role : "Customer";

                    if ("Admin".equalsIgnoreCase(userRole) || "Technician".equalsIgnoreCase(userRole)) {
                        navigateToDashboard(userRole);
                    } else {
                        currentUser.reload().addOnCompleteListener(reloadTask -> {
                            if (currentUser.isEmailVerified()) {
                                navigateToDashboard("Customer");
                            } else {
                                mAuth.signOut();
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                finish();
                            }
                        });
                    }
                });
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("Admin".equalsIgnoreCase(role)) {
            intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
        } else if ("Technician".equalsIgnoreCase(role)) {
            intent = new Intent(MainActivity.this, TechnicianDashboardActivity.class);
        } else {
            intent = new Intent(MainActivity.this, CustomerDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
