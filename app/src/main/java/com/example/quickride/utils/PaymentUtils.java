package com.example.quickride.utils;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.example.quickride.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class PaymentUtils {

    public interface OnPaymentSelectedListener {
        void onPaymentSelected(String method, String accountNumber);
    }

    private static String selectedMethod = "cash";

    public static void showPaymentSelectionDialog(Context context, OnPaymentSelectedListener listener) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_payment_selection, null);
        bottomSheetDialog.setContentView(view);

        CardView cardCash = view.findViewById(R.id.cardCash);
        CardView cardJazzCash = view.findViewById(R.id.cardJazzCash);
        CardView cardEasyPaisa = view.findViewById(R.id.cardEasyPaisa);

        RadioButton rbCash = view.findViewById(R.id.rbCash);
        RadioButton rbJazzCash = view.findViewById(R.id.rbJazzCash);
        RadioButton rbEasyPaisa = view.findViewById(R.id.rbEasyPaisa);

        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        // Set initial state
        selectedMethod = "cash";
        rbCash.setChecked(true);

        cardCash.setOnClickListener(v -> {
            selectedMethod = "cash";
            rbCash.setChecked(true);
            rbJazzCash.setChecked(false);
            rbEasyPaisa.setChecked(false);
        });

        cardJazzCash.setOnClickListener(v -> {
            selectedMethod = "jazzcash";
            rbCash.setChecked(false);
            rbJazzCash.setChecked(true);
            rbEasyPaisa.setChecked(false);
        });

        cardEasyPaisa.setOnClickListener(v -> {
            selectedMethod = "easypaisa";
            rbCash.setChecked(false);
            rbJazzCash.setChecked(false);
            rbEasyPaisa.setChecked(true);
        });

        btnConfirm.setOnClickListener(v -> {
            if (selectedMethod.equals("cash")) {
                listener.onPaymentSelected("cash", null);
                bottomSheetDialog.dismiss();
            } else {
                bottomSheetDialog.dismiss();
                showAccountNumberDialog(context, selectedMethod, listener);
            }
        });

        bottomSheetDialog.show();
    }

    private static void showAccountNumberDialog(Context context, String method, OnPaymentSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_payment_input, null);
        builder.setView(view);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        EditText etAccountNumber = view.findViewById(R.id.etAccountNumber);
        Button btnPay = view.findViewById(R.id.btnPay);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        String title = method.equals("jazzcash") ? "JazzCash Account" : "EasyPaisa Account";
        tvTitle.setText(title);
        etAccountNumber.setHint("Enter Mobile Number (03xx...)");

        AlertDialog dialog = builder.create();

        btnPay.setOnClickListener(v -> {
            String number = etAccountNumber.getText().toString().trim();
            if (number.length() < 11) {
                etAccountNumber.setError("Please enter a valid mobile number");
                return;
            }

            // Simulate processing
            Toast.makeText(context, "Processing " + method + " payment...", Toast.LENGTH_SHORT).show();
            
            // In a real app, this would trigger an SDK or API call
            new android.os.Handler().postDelayed(() -> {
                Toast.makeText(context, "Payment verified successfully!", Toast.LENGTH_SHORT).show();
                listener.onPaymentSelected(method, number);
                dialog.dismiss();
            }, 2000);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
