package com.mobile2.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * SettingsActivity handles application settings including SMS notifications
 * and account management (deleting the account).
 * Features account-specific preferences and permission handling.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private Switch smsSwitch;
    private TextView statusText;
    private DatabaseHelper dbHelper;
    private String currentUser;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        dbHelper = new DatabaseHelper(this);
        currentUser = getIntent().getStringExtra("CURRENT_USER");
        
        // Initialize account-specific preferences
        prefs = getSharedPreferences("Settings_" + (currentUser != null ? currentUser : "default"), Context.MODE_PRIVATE);

        smsSwitch = findViewById(R.id.sms_permission_switch);
        statusText = findViewById(R.id.permission_status_text);
        Button deleteAccountButton = findViewById(R.id.delete_account_button);
        Button backButton = findViewById(R.id.back_button);

        // Load saved preference for the switch state
        boolean isSmsEnabled = prefs.getBoolean("sms_enabled", false);
        smsSwitch.setChecked(isSmsEnabled);

        // Check current permission status and update status text
        updateUI();

        // Handle switch changes
        smsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // User wants to enable notifications
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                        != PackageManager.PERMISSION_GRANTED) {
                    // Temporarily uncheck until permission is granted
                    smsSwitch.setChecked(false);
                    ActivityCompat.requestPermissions(this, 
                            new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
                } else {
                    saveSmsPreference(true);
                }
            } else {
                // User wants to disable notifications
                saveSmsPreference(false);
            }
        });

        deleteAccountButton.setOnClickListener(v -> showDeleteConfirmation());

        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Saves the SMS preference to SharedPreferences and updates the status UI.
     */
    private void saveSmsPreference(boolean enabled) {
        prefs.edit().putBoolean("sms_enabled", enabled).apply();
        updateUI();
        String msg = enabled ? "SMS Notifications Enabled" : "SMS Notifications Disabled";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Are you sure you want to delete your account? This action cannot be undone.");
        
        final EditText input = new EditText(this);
        input.setHint("Confirm Username");
        builder.setView(input);

        builder.setPositiveButton("Delete", (dialog, which) -> {
            String usernameConfirm = input.getText().toString().trim();
            if (currentUser != null && currentUser.equals(usernameConfirm)) {
                if (dbHelper.deleteUser(usernameConfirm)) {
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(this, "Username mismatch. Deletion cancelled.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Updates the status text based on permission and user preference.
     */
    private void updateUI() {
        boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
                == PackageManager.PERMISSION_GRANTED;
        
        if (hasPermission) {
            String state = smsSwitch.isChecked() ? "ACTIVE" : "OFF (User Disabled)";
            statusText.setText("Permission granted. Notifications are " + state + ".");
        } else {
            statusText.setText("Permission not granted. SMS features are disabled by the system.");
            // Sync switch if permission was revoked externally
            if (smsSwitch.isChecked()) {
                smsSwitch.setChecked(false);
                prefs.edit().putBoolean("sms_enabled", false).apply();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                smsSwitch.setChecked(true);
                saveSmsPreference(true);
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                smsSwitch.setChecked(false);
                saveSmsPreference(false);
                Toast.makeText(this, "Permission Denied.", Toast.LENGTH_LONG).show();
            }
            updateUI();
        }
    }
}
