package com.techfix.app.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.adapters.TechnicianJobsAdapter;
import com.techfix.app.authentication.LoginActivity;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Appointment;
import com.techfix.app.models.Service;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private MaterialCardView cardManageBranches, cardCompletedOrders;
    private RecyclerView adminRecyclerView;
    private TextView noAdminJobsText;
    private ImageView logoutIcon;

    private TextView txtActiveRepairsCount, txtLowStockCount, txtRevenueAmount;
    private PieChart devicePieChart;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private TechnicianJobsAdapter adapter;
    private List<Appointment> appointmentsList = new ArrayList<>();
    private List<Service> servicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        // Bind dashboard shortcut cards
        cardManageBranches = findViewById(R.id.cardManageBranches);
        cardCompletedOrders = findViewById(R.id.cardCompletedOrders);
        adminRecyclerView = findViewById(R.id.adminRecyclerView);
        noAdminJobsText = findViewById(R.id.noAdminJobsText);
        logoutIcon = findViewById(R.id.adminLogoutIcon);

        txtActiveRepairsCount = findViewById(R.id.txtActiveRepairsCount);
        txtLowStockCount = findViewById(R.id.txtLowStockCount);
        txtRevenueAmount = findViewById(R.id.txtRevenueAmount);
        devicePieChart = findViewById(R.id.devicePieChart);

        // RecyclerView setup
        adminRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TechnicianJobsAdapter();
        adapter.setIsAdmin(true); // Flag to change action button text to "Allocate"
        adminRecyclerView.setAdapter(adapter);

        // Setup CRUD Click Listeners
        cardManageBranches.setOnClickListener(v -> launchCrud("branches"));
        cardCompletedOrders.setOnClickListener(v -> startActivity(new Intent(AdminDashboardActivity.this, AdminCompletedOrdersActivity.class)));

        View cardLowStockParts = findViewById(R.id.cardLowStockParts);
        if (cardLowStockParts != null) {
            cardLowStockParts.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, AdminSparePartsActivity.class);
                intent.putExtra("EXTRA_STOCK_FILTER", "Low Stock");
                startActivity(intent);
            });
        }

        View cardActiveRepairs = findViewById(R.id.cardActiveRepairs);
        View adminDashboardScrollView = findViewById(R.id.adminDashboardScrollView);
        View adminListTitle = findViewById(R.id.adminListTitle);
        if (cardActiveRepairs != null && adminDashboardScrollView != null && adminListTitle != null) {
            cardActiveRepairs.setOnClickListener(v -> {
                adminDashboardScrollView.post(() -> {
                    adminDashboardScrollView.scrollTo(0, adminListTitle.getTop());
                });
            });
        }

        // Admin Bottom Navigation Click Listeners
        findViewById(R.id.navAdminHome).setOnClickListener(v -> {
            Toast.makeText(this, "Already on Admin Dashboard", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.navAdminTechs).setOnClickListener(v -> launchCrud("technicians"));
        findViewById(R.id.navAdminServices).setOnClickListener(v -> launchCrud("services"));
        findViewById(R.id.navAdminInventory).setOnClickListener(v -> {
            startActivity(new Intent(AdminDashboardActivity.this, AdminSparePartsActivity.class));
        });
        findViewById(R.id.navAdminProfile).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, com.techfix.app.customer.ProfileActivity.class);
            startActivity(intent);
        });

        // Click on job card opens the manual technician allocation view
        adapter.setOnJobClickListener(appt -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminAllocateActivity.class);
            intent.putExtra("APPOINTMENT_ID", appt.getAppointmentId());
            startActivity(intent);
        });

        loadAllAppointments();
    }

    private void launchCrud(String type) {
        Intent intent = new Intent(AdminDashboardActivity.this, AdminManageDataActivity.class);
        intent.putExtra("MANAGE_TYPE", type);
        startActivity(intent);
    }

    private void loadAllAppointments() {
        servicesList = mDbHelper.getAllServices();

        mFirestore.collection("appointments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        appointmentsList.clear();
                        double totalRevenue = 0.0;
                        int activeCount = 0;
                        int mobileCount = 0;
                        int computerCount = 0;

                        for (DocumentSnapshot doc : task.getResult()) {
                            Appointment appt = doc.toObject(Appointment.class);
                            if (appt != null) {
                                mDbHelper.insertOrUpdateAppointment(appt);
                                
                                // Calculate total revenue of all completed repair orders
                                if ("Completed".equalsIgnoreCase(appt.getStatus()) || "Repair Completed".equalsIgnoreCase(appt.getStatus())) {
                                    double cost = 3000.0; // Fallback cost
                                    for (Service s : servicesList) {
                                        if (s.getServiceId().equalsIgnoreCase(appt.getServiceId())) {
                                            cost = s.getPrice();
                                            break;
                                        }
                                    }
                                    totalRevenue += cost;
                                } else {
                                    activeCount++;
                                    appointmentsList.add(appt);
                                }

                                // Count device category ratio
                                String category = "Mobile";
                                for (Service s : servicesList) {
                                    if (s.getServiceId().equalsIgnoreCase(appt.getServiceId())) {
                                        category = s.getCategory();
                                        break;
                                    }
                                }
                                if ("Computer".equalsIgnoreCase(category)) {
                                    computerCount++;
                                } else {
                                    mobileCount++;
                                }
                            }
                        }

                        txtActiveRepairsCount.setText(String.valueOf(activeCount));
                        txtRevenueAmount.setText(String.format("%,.0f LKR", totalRevenue));
                        updatePieChart(mobileCount, computerCount);

                        adapter.setJobs(appointmentsList, servicesList);
                        noAdminJobsText.setVisibility(appointmentsList.isEmpty() ? View.VISIBLE : View.GONE);
                        
                        // Query spare parts count for stock alerts
                        fetchLowStockPartsCount();
                    } else {
                        // Offline caching fallback
                        List<Appointment> cached = mDbHelper.getAppointmentsForCustomer("");
                        appointmentsList.clear();
                        double totalRevenue = 0.0;
                        int activeCount = 0;
                        int mobileCount = 0;
                        int computerCount = 0;

                        for (Appointment a : cached) {
                            if (!"Completed".equalsIgnoreCase(a.getStatus())) {
                                activeCount++;
                                appointmentsList.add(a);
                            } else {
                                double cost = 3000.0;
                                for (Service s : servicesList) {
                                    if (s.getServiceId().equalsIgnoreCase(a.getServiceId())) {
                                        cost = s.getPrice();
                                        break;
                                    }
                                }
                                totalRevenue += cost;
                            }

                            String category = "Mobile";
                            for (Service s : servicesList) {
                                if (s.getServiceId().equalsIgnoreCase(a.getServiceId())) {
                                    category = s.getCategory();
                                    break;
                                }
                            }
                            if ("Computer".equalsIgnoreCase(category)) {
                                computerCount++;
                            } else {
                                mobileCount++;
                            }
                        }

                        txtActiveRepairsCount.setText(String.valueOf(activeCount));
                        txtRevenueAmount.setText(String.format("%,.0f LKR", totalRevenue));
                        updatePieChart(mobileCount, computerCount);

                        adapter.setJobs(appointmentsList, servicesList);
                        noAdminJobsText.setVisibility(appointmentsList.isEmpty() ? View.VISIBLE : View.GONE);
                        
                        // Local SQLite count for low stock alerts
                        int lowStockCount = 0;
                        List<com.techfix.app.models.SparePart> localParts = mDbHelper.getAllSpareParts();
                        for (com.techfix.app.models.SparePart sp : localParts) {
                            if (sp.getQuantity() < sp.getMinimumStockLevel()) {
                                lowStockCount++;
                            }
                        }
                        txtLowStockCount.setText(String.valueOf(lowStockCount));
                    }
                });
    }

    private void fetchLowStockPartsCount() {
        mFirestore.collection("spareParts").get().addOnCompleteListener(task -> {
            int lowStockCount = 0;
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult()) {
                    com.techfix.app.models.SparePart part = doc.toObject(com.techfix.app.models.SparePart.class);
                    if (part != null && part.getQuantity() < part.getMinimumStockLevel()) {
                        lowStockCount++;
                    }
                }
            }
            txtLowStockCount.setText(String.valueOf(lowStockCount));
        });
    }

    private void updatePieChart(int mobileCount, int computerCount) {
        if (mobileCount == 0 && computerCount == 0) {
            devicePieChart.clear();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        if (mobileCount > 0) entries.add(new PieEntry(mobileCount, "Mobile"));
        if (computerCount > 0) entries.add(new PieEntry(computerCount, "Computer"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#FF4081"), Color.parseColor("#3F51B5")});
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        devicePieChart.setData(data);
        devicePieChart.getDescription().setEnabled(false);
        devicePieChart.setDrawHoleEnabled(true);
        devicePieChart.setHoleColor(Color.TRANSPARENT);
        devicePieChart.setHoleRadius(50f);
        devicePieChart.setTransparentCircleRadius(55f);
        devicePieChart.setDrawEntryLabels(false);
        devicePieChart.getLegend().setEnabled(true);
        devicePieChart.getLegend().setTextColor(Color.parseColor("#757575"));
        devicePieChart.animateY(800);
        devicePieChart.invalidate();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllAppointments();
    }
}
