package com.edumate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.edumate.app.R;
import com.edumate.app.databinding.ActivityLoginBinding;
import com.edumate.app.repository.FirestoreRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private FirestoreRepository firestoreRepository;
    private boolean isSignUpMode = false;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            setLoading(false);
                            showError("Google Sign-In succeeded, but ID token was missing.");
                        }
                    } catch (ApiException e) {
                        setLoading(false);
                        showError("Google sign-in failed (Code " + e.getStatusCode() + "): " + e.getMessage());
                    }
                } else {
                    setLoading(false);
                }
            });

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

        binding.btnGoogleSignIn.setOnClickListener(v -> handleGoogleSignIn());
        binding.btnGuestLogin.setOnClickListener(v -> handleGuestLogin());
    }

    private void toggleAuthMode() {
        isSignUpMode = !isSignUpMode;
        clearError();

        if (isSignUpMode) {
            binding.tvAuthTitle.setText("Create New Account");
            binding.tilName.setVisibility(View.VISIBLE);
            binding.btnSubmitAuth.setText("Create Account & Start");
            binding.btnGoogleSignIn.setText(R.string.btn_sign_up_google);
            binding.tvToggleMode.setText("Already have an account? Sign In");
        } else {
            binding.tvAuthTitle.setText("Sign In to Your Account");
            binding.tilName.setVisibility(View.GONE);
            binding.btnSubmitAuth.setText("Sign In");
            binding.btnGoogleSignIn.setText(R.string.btn_sign_in_google);
            binding.tvToggleMode.setText("Don't have an account? Create one");
        }
    }

    private void handleGoogleSignIn() {
        if (auth == null) {
            showError("Firebase is not initialized. Please ensure google-services.json is present.");
            return;
        }

        int clientIdRes = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        String webClientId = clientIdRes != 0 ? getString(clientIdRes) : "";

        if (webClientId.isEmpty()) {
            showError("Google Sign-In requires google-services.json with web client ID configured. Use Email or Guest login in the meantime.");
            return;
        }

        setLoading(true);
        clearError();

        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
            // Sign out prior client session so account picker is always presented
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        } catch (Exception e) {
            setLoading(false);
            showError("Failed to initialize Google Sign In: " + e.getMessage());
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            firestoreRepository.saveUserProfile(user, null);
                        }
                        Toast.makeText(LoginActivity.this, "Signed in with Google successfully!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Google authentication failed.";
                        showError(msg);
                    }
                });
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
        binding.btnGoogleSignIn.setEnabled(!loading);
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
