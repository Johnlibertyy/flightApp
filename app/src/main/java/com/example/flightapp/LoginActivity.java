package com.example.flightapp;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.SignInButton;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, signUpButton;
    private SignInButton googleSignInButton;
    private GoogleSignInClient mGoogleSignInClient;
    private static final String TAG = "LoginActivity";
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Google Sign-In result launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    } else {
                        Log.d(TAG, "Google Sign-In was cancelled or failed");
                        Toast.makeText(this, "Google Sign-In was cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // UI References
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.emailLoginButton);
        signUpButton = findViewById(R.id.SignUpButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);

        // Login button logic
        loginButton.setOnClickListener(v -> loginWithEmail());

        // Sign up button logic
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Google Sign-In button logic
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void loginWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Invalid email address");
            emailEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        goToMain();
                    } else {
                        String errorMessage = "Login failed. Please try again.";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            String msg = task.getException().getMessage();
                            if (msg.contains("password") || msg.contains("INVALID_PASSWORD")) {
                                errorMessage = "Wrong password. Please try again.";
                            } else if (msg.contains("no user record") || msg.contains("EMAIL_NOT_FOUND")) {
                                errorMessage = "No account found with this email.";
                            }
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            // Google Sign In was successful
            GoogleSignInAccount account = task.getResult(ApiException.class);
            Log.d(TAG, "Google sign in successful: " + account.getEmail());
            
            // Show welcome message
            String welcomeMessage = "Welcome " + (account.getDisplayName() != null ? account.getDisplayName() : "User") + "!";
            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();
            
            // Add a small delay before navigating to prevent crash
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                goToMain();
            }, 1000); // 1 second delay
            
        } catch (ApiException e) {
            // Google Sign In failed
            Log.w(TAG, "Google sign in failed", e);
            String errorMessage = "Google sign in failed";
            if (e.getStatusCode() == 12501) {
                errorMessage = "Google sign in was cancelled";
            } else if (e.getStatusCode() == 7) {
                errorMessage = "Network error. Please check your internet connection";
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }
}
