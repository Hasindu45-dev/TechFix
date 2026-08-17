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
    private LinearLayout cardFieldsContainer;
    private TextInputEditText cardNameEditText, cardNoEditText, expiryEditText, cvvEditText;
    private MaterialButton btnSubmitPayment;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DatabaseHelper mDbHelper;

    private String appointmentId, deviceModel, serviceName;
    private double serviceCost;

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
        cardNameEditText = findViewById(R.id.cardNameEditText);
        cardNoEditText = findViewById(R.id.cardNoEditText);
        expiryEditText = findViewById(R.id.expiryEditText);
        cvvEditText = findViewById(R.id.cvvEditText);
        btnSubmitPayment = findViewById(R.id.btnSubmitPayment);
        progressBar = findViewById(R.id.paymentProgressBar);

        // Fill cost details
        paymentTotalText.setText("Rs. " + String.format("%,.2f", serviceCost));
        paymentDeviceText.setText("Device: " + deviceModel);
        paymentServiceText.setText("Service: " + serviceName);
        btnSubmitPayment.setText("Confirm Cash Payment");

        // Radio group listener to toggle Stripe fields
        paymentMethodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioCash) {
                cardFieldsContainer.setVisibility(View.GONE);
                btnSubmitPayment.setText("Confirm Cash Payment");
            } else if (checkedId == R.id.radioCard) {
                cardFieldsContainer.setVisibility(View.VISIBLE);
                btnSubmitPayment.setText("Pay via Stripe Gateway (Rs. " + String.format("%,.2f", serviceCost) + ")");
            }
        });

        btnSubmitPayment.setOnClickListener(v -> handlePayment());
    }

    private void handlePayment() {
        int checkedId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        boolean isStripeCard = (checkedId == R.id.radioCard);

        if (isStripeCard) {
            String cardName = cardNameEditText.getText() != null ? cardNameEditText.getText().toString().trim() : "";
            String cardNo = cardNoEditText.getText() != null ? cardNoEditText.getText().toString().trim() : "";
            String expiry = expiryEditText.getText() != null ? expiryEditText.getText().toString().trim() : "";
            String cvv = cvvEditText.getText() != null ? cvvEditText.getText().toString().trim() : "";

            if (TextUtils.isEmpty(cardName)) {
                cardNameEditText.setError("Cardholder Name is required");
                return;
            }
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
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmitPayment.setEnabled(false);

        // Stripe Gateway Simulation
        String paymentId = "ch_stripe_" + UUID.randomUUID().toString().substring(0, 12);
        String customerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());

        String status = "Completed";
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
                                appointmentId,
                                appointmentId,
                                customerId,
                                deviceModel,
                                serviceName,
                                "Colombo Branch",
                                date,
                                serviceCost,
                                "Completed",
                                "Completed"
                        );

                        // 2. Update payment status online in appointment document
                        mFirestore.collection("appointments").document(appointmentId)
                                .update("status", "Completed")
                                .addOnCompleteListener(dbTask -> {
                                    showPaymentSuccessDialog(isStripeCard, paymentId);
                                });
                    } else {
                        Toast.makeText(PaymentActivity.this, "Payment error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showPaymentSuccessDialog(boolean isStripe, String chargeId) {
        String methodTitle = isStripe ? "Stripe Payment Authorized!" : "Cash Order Confirmed!";
        String methodDetails = isStripe ? 
                "Stripe Charge ID: " + chargeId + "\nPayment Status: 200 OK (Succeeded)\n\n" :
                "Payment Method: Cash on Delivery / Pickup\n\n";

        new AlertDialog.Builder(this)
                .setTitle(methodTitle)
                .setMessage("Thank you! Your transaction has been processed successfully.\n\n"
                        + methodDetails
                        + "Ticket ID: #" + appointmentId.substring(0, 8).toUpperCase() + "\n"
                        + "Total Amount: Rs. " + String.format("%,.2f", serviceCost) + "\n\n"
                        + "Please present your Ticket ID at the branch to collect your device.")
                .setPositiveButton("Finish", (dialog, which) -> {
                    dialog.dismiss();
                    Intent intent = new Intent(PaymentActivity.this, CustomerDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
