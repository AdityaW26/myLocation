package com.example.mylocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

public class FingerPrintActivity extends AppCompatActivity {

    private TextView textView;
    private CardView btnRetry;
    private ImageView imageViewStatus;
    private RelativeLayout relativeLayout;
    private ProgressBar progressBar;

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finger_print);

        initValues();

        BiometricManager manager = BiometricManager.from(this);
        switch (manager.canAuthenticate()){
            case BiometricManager.BIOMETRIC_SUCCESS:
                textView.setText("Biometric Verification");
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                textView.setText("No biometric hardware on your device");
                imageViewStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_close_24));
                imageViewStatus.setVisibility(View.VISIBLE);
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                textView.setText("No biometric enrolled on your device");
                imageViewStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_info_24));
                imageViewStatus.setVisibility(View.VISIBLE);
                break;

            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                textView.setText("Biometric security update required on your device");
                imageViewStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_info_24));
                imageViewStatus.setVisibility(View.VISIBLE);
                break;

            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:

            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                textView.setText("Verification unsupported");
                imageViewStatus.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_baseline_close_24));
                imageViewStatus.setVisibility(View.VISIBLE);
                break;

        }

        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull @NotNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Snackbar.make(relativeLayout,
                        "Something went wrong\nPlease try again", Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.white))
                        .setTextColor(getColor(R.color.black))
                        .show();
                imageViewStatus.setImageDrawable(ContextCompat.getDrawable(FingerPrintActivity.this, R.drawable.ic_baseline_close_24));
                imageViewStatus.setVisibility(View.VISIBLE);
                btnRetry.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull @NotNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Snackbar.make(relativeLayout,
                        "Verified", Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.white))
                        .setTextColor(getColor(R.color.black))
                        .show();
                imageViewStatus.setImageDrawable(ContextCompat.getDrawable(FingerPrintActivity.this, R.drawable.ic_baseline_done_24));
                imageViewStatus.setVisibility(View.VISIBLE);

                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(FingerPrintActivity.this, MainActivity.class));
                        finish();
                        progressBar.setVisibility(View.GONE);
                    }
                }, 2000);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Snackbar.make(relativeLayout,
                        "Verification failed", Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.white))
                        .setTextColor(getColor(R.color.black))
                        .show();
                imageViewStatus.setImageDrawable(ContextCompat.getDrawable(FingerPrintActivity.this, R.drawable.ic_baseline_close_24));
                imageViewStatus.setVisibility(View.VISIBLE);
            }
        });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Verification")
                .setNegativeButtonText("Cancel")
                .build();

        prompt.authenticate(info);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Biometric Verification")
                        .setNegativeButtonText("Cancel")
                        .build();
                prompt.authenticate(info);
            }
        });
    }

    private void initValues() {
        textView = findViewById(R.id.tv_biometricVerification);
        relativeLayout = findViewById(R.id.activity_fingerprint);
        imageViewStatus = findViewById(R.id.iv_fingerPrintStatus);
        imageViewStatus.setVisibility(View.GONE);
        btnRetry = findViewById(R.id.btn_retry);
        btnRetry.setVisibility(View.GONE);
        progressBar = findViewById(R.id.progressBar);

    }
}