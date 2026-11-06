package com.edumate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.edumate.app.databinding.ActivityLoginBinding;
import com.edumate.app.repository.FirestoreRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirestoreRepository firestoreRepository;
    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            auth = FirebaseAuth.getInstance();
        } catch (Throwable t) {
            auth = null;
        }
        firestoreRepository = FirestoreRepository.getInstance();

        // If user is already authenticated, skip login and launch MainActivity
        if (auth != null && auth.getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.tvToggleMode.setOnClickListener(v -> toggleAuthMode());

        binding.btnSubmitAuth.setOnClickListener(v -> {
            if (isSignUpMode) {
                handleSignUp();
            } else {
                handleSignIn();
            }
        });

        binding.btnGuestLogin.setOnClickListener(v -> handleGuestLogin());
    }

    private void toggleAuthMode() {
        isSignUpMode = !isSignUpMode;
        clearError();

        if (isSignUpMode) {
            binding.tvAuthTitle.setText("Create New Account");
            binding.tilName.setVisibility(View.VISIBLE);
            binding.btnSubmitAuth.setText("Create Account & Start");
            binding.tvToggleMode.setText("Already have an account? Sign In");
        } else {
            binding.tvAuthTitle.setText("Sign In to Your Account");
            binding.tilName.setVisibility(View.GONE);
            binding.btnSubmitAuth.setText("Sign In");
            binding.tvToggleMode.setText("Don't have an account? Create one");
        }
    }

    private void handleSignIn() {
        String email = getFieldText(binding.etEmail);
        String password = getFieldText(binding.etPassword);

        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        if (auth == null) {
            showError("Firebase is not initialized. Please ensure google-services.json is present.");
            return;
        }

        setLoading(true);
        clearError();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            firestoreRepository.saveUserProfile(user, null);
                        }
                        Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                        showError(msg);
                    }
                });
    }

    private void handleSignUp() {
        String name = getFieldText(binding.etName);
        String email = getFieldText(binding.etEmail);
        String password = getFieldText(binding.etPassword);

        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Please enter your name");
            return;
        } else {
            binding.tilName.setError(null);
        }

        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        if (auth == null) {
            showError("Firebase is not initialized. Please ensure google-services.json is present.");
            return;
        }

        setLoading(true);
        clearError();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest update = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(update).addOnCompleteListener(profileTask -> {
                                setLoading(false);
                                firestoreRepository.saveUserProfile(user, null);
                                Toast.makeText(LoginActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                navigateToMain();
                            });
                        } else {
                            setLoading(false);
                            navigateToMain();
                        }
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                        showError(msg);
                    }
                });
    }

    private void handleGuestLogin() {
        if (auth == null) {
            showError("Firebase is not initialized. Please ensure google-services.json is present.");
            return;
        }

        setLoading(true);
        clearError();

        auth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            firestoreRepository.saveUserProfile(user, null);
                        }
                        Toast.makeText(LoginActivity.this, "Signed in as Guest", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Guest login failed.";
                        showError(msg);
                    }
                });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email cannot be empty");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Invalid email address format");
            return false;
        }
        binding.tilEmail.setError(null);
        return true;
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password cannot be empty");
            return false;
        }
        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            return false;
        }
        binding.tilPassword.setError(null);
        return true;
    }

    private String getFieldText(android.widget.EditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private void setLoading(boolean loading) {
        binding.pbAuthLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmitAuth.setEnabled(!loading);
        binding.btnGuestLogin.setEnabled(!loading);
        binding.tvToggleMode.setEnabled(!loading);
    }

    private void showError(String msg) {
        binding.tvAuthError.setVisibility(View.VISIBLE);
        binding.tvAuthError.setText(msg);
    }

    private void clearError() {
        binding.tvAuthError.setVisibility(View.GONE);
        binding.tvAuthError.setText("");
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
