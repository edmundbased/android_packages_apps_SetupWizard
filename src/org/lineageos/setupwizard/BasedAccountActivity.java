/*
 * SPDX-FileCopyrightText: 2026 The BasedOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.basedos.privybridge.PrivyBridge;
import com.basedos.privybridge.PrivyBridgeCallback;
import com.basedos.privybridge.PrivyBridgeConfig;
import com.basedos.privybridge.PrivyBridgeResult;

public class BasedAccountActivity extends BaseSetupWizardActivity {

    private static final String TAG = "BasedAccount";

    private TextView mGatewayAuthStatus;
    private LinearLayout mGatewayLoginForm;
    private EditText mGatewayEmailInput;
    private Button mGatewaySendCodeButton;
    private LinearLayout mGatewayOtpRow;
    private EditText mGatewayOtpInput;
    private Button mGatewayVerifyCodeButton;
    private Button mGatewayGoogleLoginButton;
    private LinearLayout mGatewayOrDivider;
    private LinearLayout mGatewaySuccessContainer;
    private TextView mGatewaySuccessLabel;
    private TextView mGatewaySuccessEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.based_account_summary));

        // Override default tint so the logo mark keeps its brand colors
        final Drawable icon = getDrawable(R.drawable.ic_account);
        if (icon != null) {
            getGlifLayout().setIcon(icon.mutate());
        }

        mGatewayAuthStatus = findViewById(R.id.gateway_auth_status);
        mGatewayLoginForm = findViewById(R.id.gateway_login_form);
        mGatewayEmailInput = findViewById(R.id.gateway_email_input);
        mGatewaySendCodeButton = findViewById(R.id.gateway_send_code_button);
        mGatewayOtpRow = findViewById(R.id.gateway_otp_row);
        mGatewayOtpInput = findViewById(R.id.gateway_otp_input);
        mGatewayVerifyCodeButton = findViewById(R.id.gateway_verify_code_button);
        mGatewayGoogleLoginButton = findViewById(R.id.gateway_google_login_button);
        mGatewayOrDivider = findViewById(R.id.gateway_or_divider);
        mGatewaySuccessContainer = findViewById(R.id.gateway_success_container);
        mGatewaySuccessLabel = findViewById(R.id.gateway_success_label);
        mGatewaySuccessEmail = findViewById(R.id.gateway_success_email);

        mGatewaySendCodeButton.setOnClickListener(v -> sendEmailCode());
        mGatewayVerifyCodeButton.setOnClickListener(v -> verifyEmailCode());
        mGatewayGoogleLoginButton.setOnClickListener(v -> launchGoogleLogin());

        refreshGatewayAuthStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshGatewayAuthStatus();
    }

    /**
     * Lazily initialize PrivyBridge. SetupWizard runs as UID 1000 (system),
     * where WebView is blocked. If the Privy SDK tries to create a WebView
     * during init, we catch the exception and disable Privy-based auth.
     */
    private boolean ensurePrivyInitialized() {
        if (PrivyBridge.isInitialized()) {
            return true;
        }
        try {
            PrivyBridgeConfig config =
                    PrivyBridgeConfig.fromSystemProperties("basedos-setup");
            PrivyBridge.init(this, config);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Cannot init PrivyBridge (WebView blocked in system process)", e);
            return false;
        }
    }

    // -- Email OTP (via Privy SDK) --

    private void sendEmailCode() {
        final String email = mGatewayEmailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        mGatewaySendCodeButton.setEnabled(false);
        mGatewaySendCodeButton.setText("Sending\u2026");

        if (!ensurePrivyInitialized()) {
            Toast.makeText(this, "Auth not available yet \u2014 try again later",
                    Toast.LENGTH_LONG).show();
            mGatewaySendCodeButton.setEnabled(true);
            mGatewaySendCodeButton.setText(R.string.agent_gateway_send_code);
            return;
        }
        PrivyBridge.getInstance().sendEmailOtp(email, new PrivyBridgeCallback<>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    revealOtpRow();
                    mGatewaySendCodeButton.setEnabled(true);
                    mGatewaySendCodeButton.setText(R.string.agent_gateway_send_code);
                    mGatewayAuthStatus.setText(R.string.agent_gateway_code_sent);
                    mGatewayOtpInput.requestFocus();
                });
            }

            @Override
            public void onError(String errorCode, String message) {
                runOnUiThread(() -> {
                    mGatewaySendCodeButton.setEnabled(true);
                    mGatewaySendCodeButton.setText(R.string.agent_gateway_send_code);
                    Toast.makeText(BasedAccountActivity.this,
                            "Failed to send code: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void verifyEmailCode() {
        final String email = mGatewayEmailInput.getText().toString().trim();
        final String code = mGatewayOtpInput.getText().toString().trim();

        if (TextUtils.isEmpty(code) || code.length() < 6) {
            Toast.makeText(this, "Enter the 6-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        mGatewayVerifyCodeButton.setEnabled(false);
        mGatewayVerifyCodeButton.setText("Verifying\u2026");

        if (!ensurePrivyInitialized()) {
            mGatewayVerifyCodeButton.setEnabled(true);
            mGatewayVerifyCodeButton.setText(R.string.agent_gateway_verify_code);
            return;
        }
        PrivyBridge.getInstance().loginWithEmailOtp(email, code,
                new PrivyBridgeCallback<>() {
            @Override
            public void onSuccess(PrivyBridgeResult result) {
                onPrivyLoginSuccess(result);
            }

            @Override
            public void onError(String errorCode, String message) {
                runOnUiThread(() -> {
                    mGatewayVerifyCodeButton.setEnabled(true);
                    mGatewayVerifyCodeButton.setText(R.string.agent_gateway_verify_code);
                    Toast.makeText(BasedAccountActivity.this,
                            "Verification failed: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // -- Google OAuth (via Privy SDK) --

    private void launchGoogleLogin() {
        mGatewayGoogleLoginButton.setEnabled(false);

        if (!ensurePrivyInitialized()) {
            mGatewayGoogleLoginButton.setEnabled(true);
            Toast.makeText(this, "Auth not available yet \u2014 try again later",
                    Toast.LENGTH_LONG).show();
            return;
        }
        PrivyBridge.getInstance().loginWithOAuth("google",
                new PrivyBridgeCallback<>() {
            @Override
            public void onSuccess(PrivyBridgeResult result) {
                onPrivyLoginSuccess(result);
            }

            @Override
            public void onError(String errorCode, String message) {
                runOnUiThread(() -> {
                    mGatewayGoogleLoginButton.setEnabled(true);
                    Toast.makeText(BasedAccountActivity.this,
                            "Sign-in failed: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // -- Privy login success handler --

    private void onPrivyLoginSuccess(PrivyBridgeResult result) {
        Log.i(TAG, "Privy login success: userId=" + result.userId
                + ", email=" + result.email);

        Settings.Secure.putInt(getContentResolver(), "agent_gateway_logged_in", 1);
        if (result.userId != null) {
            Settings.Secure.putString(getContentResolver(),
                    "agent_privy_user_id", result.userId);
        }
        if (result.email != null) {
            Settings.Secure.putString(getContentResolver(),
                    "agent_privy_email", result.email);
        }

        if (result.hasIdentityToken()) {
            try {
                final AgentGatewayAuthController.ExchangeResult exchange =
                        AgentGatewayAuthController.exchangeOrDirect(
                                result.identityToken);
                AgentGatewayAuthController.persistSession(exchange);
                Log.i(TAG, "Gateway session persisted after Privy login");
            } catch (Exception e) {
                Log.e(TAG, "Gateway token exchange failed", e);
                runOnUiThread(() -> Toast.makeText(BasedAccountActivity.this,
                        "Gateway login failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
                return;
            }
        }

        runOnUiThread(() -> {
            mGatewayVerifyCodeButton.setEnabled(true);
            mGatewayVerifyCodeButton.setText(R.string.agent_gateway_verify_code);
            mGatewayGoogleLoginButton.setEnabled(true);
            refreshGatewayAuthStatus();
            Toast.makeText(BasedAccountActivity.this,
                    "Signed in", Toast.LENGTH_SHORT).show();
        });
    }

    // -- OTP row animation --

    private void revealOtpRow() {
        if (mGatewayOtpRow.getVisibility() == View.VISIBLE) {
            return;
        }
        mGatewayOtpRow.setAlpha(0f);
        mGatewayOtpRow.setTranslationY(dpToPx(-12));
        mGatewayOtpRow.setVisibility(View.VISIBLE);
        mGatewayOtpRow.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    // -- Gateway status --

    private void refreshGatewayAuthStatus() {
        if (mGatewayAuthStatus == null) {
            return;
        }
        final boolean hasSession = PrivyBridge.isInitialized()
                && PrivyBridge.getInstance().isAuthenticated();
        final boolean hasStoredToken = new java.io.File(
                "/data/misc/agent/runtime/llm_api_key").exists();
        if (!hasSession && !hasStoredToken) {
            mGatewayAuthStatus.setText(R.string.agent_gateway_logged_out);
            setLoginUiVisible(true);
            return;
        }
        final PrivyBridgeResult user = hasSession
                ? PrivyBridge.getInstance().getCurrentUser() : null;
        if (user != null && user.email != null) {
            mGatewayAuthStatus.setText(getString(R.string.agent_gateway_logged_in));
            showSuccessState(user.email);
        } else {
            mGatewayAuthStatus.setText(R.string.agent_gateway_logged_in);
            showSuccessState(null);
        }
        setLoginUiVisible(false);
    }

    private void setLoginUiVisible(boolean visible) {
        final int vis = visible ? View.VISIBLE : View.GONE;
        if (mGatewayLoginForm != null) mGatewayLoginForm.setVisibility(vis);
        if (!visible && mGatewayOtpRow != null) {
            mGatewayOtpRow.setVisibility(View.GONE);
        }
        if (mGatewaySuccessContainer != null) {
            mGatewaySuccessContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        }
    }

    private void showSuccessState(@Nullable String email) {
        if (mGatewaySuccessContainer == null) {
            return;
        }
        mGatewaySuccessContainer.setVisibility(View.VISIBLE);
        if (mGatewaySuccessEmail != null) {
            if (email != null) {
                mGatewaySuccessEmail.setText(email);
                mGatewaySuccessEmail.setVisibility(View.VISIBLE);
            } else {
                mGatewaySuccessEmail.setVisibility(View.GONE);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_based_account;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_based_account;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_account;
    }
}
