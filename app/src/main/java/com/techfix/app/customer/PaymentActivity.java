package com.techfix.app.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techfix.app.R;
import com.techfix.app.database.DatabaseHelper;
import com.techfix.app.models.Payment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private TextView paymentTotalText, paymentDeviceText, paymentServiceText;
    private RadioGroup paymentMethodRadioGroup;
    private LinearLayout cardFieldsContainer, onlineFieldsContainer;
    private TextInputEditText cardNoEditText, expiryEditText, cvvEditText, bankUsername;
    private AutoCompleteTextView bankSelectAutoComplete;
    private MaterialButton btnSubmitPayment;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String appointmentId, deviceModel, serviceName;
    private double serviceCost;

    private final String[] BANKS = {"Commercial Bank", "Bank of Ceylon", "Hatton National Bank", "DFCC Bank"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mDbHelper = new DatabaseHelper(this);

        // Retrieve intent extras
        appointmentId = getIntent().getStringExtra("APPOINTMENT_ID");
        deviceModel = getIntent().getStringExtra("DEVICE_MODEL");
        serviceName = getIntent().getStringExtra("SERVICE_NAME");
        serviceCost = getIntent().getDoubleExtra("SERVICE_COST", 3000.0);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Bind Views
        paymentTotalText = findViewById(R.id.paymentTotalText);
        paymentDeviceText = findViewById(R.id.paymentDeviceText);
        paymentServiceText = findViewById(R.id.paymentServiceText);
        paymentMethodRadioGroup = findViewById(R.id.paymentMethodRadioGroup);
        cardFieldsContainer = findViewById(R.id.cardFieldsContainer);
        onlineFieldsContainer = findViewById(R.id.onlineFieldsContainer);
        cardNoEditText = findViewById(R.id.cardNoEditText);
        expiryEditText = findViewById(R.id.expiryEditText);
        cvvEditText = findViewById(R.id.cvvEditText);
        bankUsername = findViewById(R.id.bankUsername);
        bankSelectAutoComplete = findViewById(R.id.bankSelectAutoComplete);
        btnSubmitPayment = findViewById(R.id.btnSubmitPayment);
        progressBar = findViewById(R.id.paymentProgressBar);

        // Fill cost details
        paymentTotalText.setText("Rs. " + String.format("%,.2f", serviceCost));
        paymentDeviceText.setText("Device: " + deviceModel);
        paymentServiceText.setText("Service: " + serviceName);
        btnSubmitPayment.setText("Pay Rs. " + String.format("%,.2f", serviceCost));

        // Setup Bank selector
        ArrayAdapter<String> bankAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, BANKS);
        bankSelectAutoComplete.setAdapter(bankAdapter);
        bankSelectAutoComplete.setText(BANKS[0], false);

        // Radio group listener to show/hide dynamic fields
        paymentMethodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCash) {
                cardFieldsContainer.setVisibility(View.GONE);
                onlineFieldsContainer.setVisibility(View.GONE);
                btnSubmitPayment.setText("Confirm Cash Payment");
            } else if (checkedId == R.id.radioCard) {
                cardFieldsContainer.setVisibility(View.VISIBLE);
                onlineFieldsContainer.setVisibility(View.GONE);
                btnSubmitPayment.setText("Pay Rs. " + String.format("%,.2f", serviceCost));
            } else if (checkedId == R.id.radioOnline) {
                cardFieldsContainer.setVisibility(View.GONE);
                onlineFieldsContainer.setVisibility(View.VISIBLE);
                btnSubmitPayment.setText("Pay via Bank Portal");
            }
        });

        btnSubmitPayment.setOnClickListener(v -> handlePayment());
    }

    private void handlePayment() {
        int checkedId = paymentMethodRadioGroup.getCheckedRadioButtonId();

        // Perform mock validation based on checkout selection
        if (checkedId == R.id.radioCard) {
            String cardNo = cardNoEditText.getText().toString().trim();
            String expiry = expiryEditText.getText().toString().trim();
            String cvv = cvvEditText.getText().toString().trim();

            if (cardNo.length() != 16) {
                cardNoEditText.setError("Enter a valid 16-digit card number");
                return;
            }
            if (TextUtils.isEmpty(expiry) || !expiry.contains("/")) {
                expiryEditText.setError("Enter valid expiry (MM/YY)");
                return;
            }
            if (cvv.length() != 3) {
                cvvEditText.setError("Enter valid 3-digit CVV");
                return;
            }
        } else if (checkedId == R.id.radioOnline) {
            String username = bankUsername.getText().toString().trim();
            if (TextUtils.isEmpty(username)) {
                bankUsername.setError("Enter your banking username");
                return;
            }
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitPayment.setEnabled(false);

        // Simulation parameters
        String paymentId = UUID.randomUUID().toString();
        String customerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());

        String status = "Completed"; // Payments simulation defaults to success
        Payment payment = new Payment(paymentId, appointmentId, serviceCost, status, date);

        // Write Payment record to Firestore
        mFirestore.collection("payments").document(paymentId)
                .set(payment)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmitPayment.setEnabled(true);

                    if (task.isSuccessful()) {
                        // 1. Update the local SQLite repair history table to mark payment completed (Paid)
                        mDbHelper.insertOrUpdateHistory(
                                appointmentId, // History key
                                appointmentId,
                                customerId,
                                deviceModel,
                                serviceName,
                                "Colombo Branch", // Fallback branch name
                                date,
                                serviceCost,
                                "Completed", // Mark Paid in local database helper cache!
                                "Completed"
                        );

                        // 2. Update payment status online in appointment document
                        mFirestore.collection("appointments").document(appointmentId)
                                .update("status", "Completed") // Move to Completed status after pay
                                .addOnCompleteListener(dbTask -> {
                                    showPaymentSuccessDialog();
                                });
                    } else {
                        Toast.makeText(PaymentActivity.this, "Payment error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showPaymentSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Payment Completed!")
                .setMessage("Thank you! Your simulated transaction has been processed successfully.\n\n"
                        + "Receipt ID: #" + appointmentId.substring(0, 8).toUpperCase() + "\n"
                        + "Amount Paid: Rs. " + String.format("%,.2f", serviceCost) + "\n\n"
                        + "Please show your Ticket ID at the branch to collect your device.")
                .setPositiveButton("Finish", (dialog, which) -> {
                    dialog.dismiss();
                    // Go back to Customer Dashboard
                    Intent intent = new Intent(PaymentActivity.this, CustomerDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
