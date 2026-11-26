package com.example.grocery;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.grocery.utils.FirebaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etRegEmail, etRegPassword, etRegConfirmPassword;
    private MaterialButton btnRegisterSubmit;
    private TextView tvBackToLogin;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etRegConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        firebaseHelper = new FirebaseHelper();

        btnRegisterSubmit.setOnClickListener(v -> registerNewUser());

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void registerNewUser() {
        String email = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String confirmPassword = etRegConfirmPassword.getText() != null ? etRegConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegEmail.setError("Enter a valid email address");
            etRegEmail.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etRegPassword.setError("Password must be at least 6 characters");
            etRegPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etRegConfirmPassword.setError("Passwords do not match");
            etRegConfirmPassword.requestFocus();
            return;
        }

        btnRegisterSubmit.setEnabled(false);
        btnRegisterSubmit.setText("Creating Account...");

        firebaseHelper.registerUser(email, password, new FirebaseHelper.ActionCallback() {
            @Override
            public void onSuccess() {
                String uid = firebaseHelper.getCurrentUserId();
                if (uid != null) {
                    Map<String, Object> userProfile = new HashMap<>();
                    userProfile.put("userId", uid);
                    userProfile.put("email", email);
                    userProfile.put("createdAt", System.currentTimeMillis());
                    FirebaseDatabase.getInstance().getReference("Users").child(uid).setValue(userProfile);
                }

                Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                btnRegisterSubmit.setEnabled(true);
                btnRegisterSubmit.setText("Create Account");
                Toast.makeText(RegisterActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
