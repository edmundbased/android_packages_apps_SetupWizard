/*
 * SPDX-FileCopyrightText: 2026 The BasedOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.settingslib.Utils;
import com.basedos.privybridge.PrivyBridge;
import com.basedos.privybridge.PrivyBridgeCallback;
import com.basedos.privybridge.PrivyBridgeResult;
import com.basedos.privybridge.PrivyBridgeConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AgentSetupActivity extends BaseSetupWizardActivity {

    private static final String TAG = "AgentSetup";

    private static final String[] EMOJI_OPTIONS = {
        // Smileys & faces
        "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01",
        "\uD83D\uDE06", "\uD83D\uDE05", "\uD83D\uDE02", "\uD83E\uDD23",
        "\uD83D\uDE0A", "\uD83D\uDE07", "\uD83D\uDE42", "\uD83D\uDE43",
        "\uD83D\uDE09", "\uD83D\uDE0C", "\uD83D\uDE0D", "\uD83E\uDD70",
        "\uD83D\uDE18", "\uD83D\uDE17", "\uD83D\uDE1A", "\uD83D\uDE0B",
        "\uD83D\uDE1B", "\uD83D\uDE1C", "\uD83E\uDD2A", "\uD83D\uDE1D",
        "\uD83E\uDD11", "\uD83E\uDD17", "\uD83E\uDD2D", "\uD83E\uDD2B",
        "\uD83E\uDD14", "\uD83E\uDD28", "\uD83D\uDE10", "\uD83D\uDE11",
        "\uD83D\uDE36", "\uD83D\uDE0F", "\uD83D\uDE12", "\uD83D\uDE44",
        "\uD83D\uDE2C", "\uD83E\uDD25", "\uD83D\uDE0E", "\uD83E\uDD13",
        "\uD83E\uDDD0", "\uD83E\uDD78", "\uD83E\uDD29", "\uD83E\uDD73",
        "\uD83D\uDE15", "\uD83D\uDE1F", "\uD83D\uDE32", "\uD83D\uDE33",
        "\uD83E\uDD7A", "\uD83D\uDE26", "\uD83D\uDE28", "\uD83D\uDE30",
        "\uD83D\uDE25", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE31",
        "\uD83D\uDE16", "\uD83D\uDE23", "\uD83D\uDE1E", "\uD83D\uDE24",
        "\uD83D\uDE21", "\uD83D\uDE20", "\uD83E\uDD2C", "\uD83D\uDE08",
        "\uD83D\uDC7F", "\uD83D\uDC80", "\u2620\uFE0F", "\uD83D\uDCA9",
        "\uD83E\uDD21", "\uD83D\uDC7B", "\uD83D\uDC7D", "\uD83D\uDC7E",
        "\uD83E\uDD16", "\uD83D\uDE3A", "\uD83D\uDE38", "\uD83D\uDE39",
        "\uD83D\uDE3B", "\uD83D\uDE3C", "\uD83D\uDE3D", "\uD83D\uDE40",
        // Gestures & people
        "\uD83D\uDC4B", "\uD83E\uDD1A", "\uD83D\uDD90\uFE0F", "\u270B",
        "\uD83D\uDD96", "\uD83D\uDC4C", "\uD83E\uDD0C", "\uD83E\uDD0F",
        "\u270C\uFE0F", "\uD83E\uDD1E", "\uD83E\uDD1F", "\uD83E\uDD18",
        "\uD83D\uDC4D", "\uD83D\uDC4E", "\u270A",       "\uD83D\uDC4A",
        "\uD83D\uDC4F", "\uD83D\uDE4C", "\uD83D\uDC50", "\uD83E\uDD32",
        "\uD83D\uDE4F", "\uD83D\uDCAA", "\uD83E\uDDB5", "\uD83E\uDDB6",
        "\uD83E\uDDD1", "\uD83D\uDC69", "\uD83D\uDC68", "\uD83E\uDDD2",
        "\uD83D\uDC76", "\uD83E\uDDD3", "\uD83D\uDC74", "\uD83D\uDC75",
        "\uD83E\uDDD9", "\uD83E\uDDDA", "\uD83E\uDDDB", "\uD83E\uDDDC",
        "\uD83E\uDDDD", "\uD83E\uDDDE", "\uD83E\uDDDF", "\uD83E\uDDB8",
        "\uD83E\uDDB9", "\uD83E\uDD77", "\uD83E\uDD20", "\uD83E\uDD34",
        "\uD83D\uDC78", "\uD83D\uDC82", "\uD83D\uDC77", "\uD83D\uDC70",
        // Animals
        "\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDC2D", "\uD83D\uDC39",
        "\uD83D\uDC30", "\uD83E\uDD8A", "\uD83D\uDC3B", "\uD83D\uDC3C",
        "\uD83D\uDC28", "\uD83D\uDC2F", "\uD83E\uDD81", "\uD83D\uDC2E",
        "\uD83D\uDC37", "\uD83D\uDC38", "\uD83D\uDC35", "\uD83D\uDE48",
        "\uD83D\uDE49", "\uD83D\uDE4A", "\uD83D\uDC12", "\uD83D\uDC14",
        "\uD83D\uDC27", "\uD83D\uDC26", "\uD83D\uDC24", "\uD83E\uDD86",
        "\uD83E\uDD85", "\uD83E\uDD89", "\uD83E\uDDA4", "\uD83E\uDD9A",
        "\uD83E\uDD9C", "\uD83D\uDC19", "\uD83D\uDC1A", "\uD83D\uDC0C",
        "\uD83E\uDD8B", "\uD83D\uDC1B", "\uD83D\uDC1C", "\uD83D\uDC1D",
        "\uD83D\uDC22", "\uD83D\uDC0D", "\uD83E\uDD8E", "\uD83D\uDC0A",
        "\uD83D\uDC33", "\uD83D\uDC2C", "\uD83D\uDC1F", "\uD83D\uDC20",
        "\uD83E\uDD88", "\uD83E\uDD8D", "\uD83E\uDDA7", "\uD83D\uDC18",
        "\uD83E\uDD93", "\uD83E\uDD8F", "\uD83D\uDC2A", "\uD83E\uDD92",
        "\uD83E\uDD95", "\uD83E\uDD96", "\uD83E\uDDA5", "\uD83E\uDD94",
        // Nature & weather
        "\uD83C\uDF38", "\uD83C\uDF39", "\uD83C\uDF3A", "\uD83C\uDF3B",
        "\uD83C\uDF3C", "\uD83C\uDF37", "\uD83C\uDF31", "\uD83C\uDF32",
        "\uD83C\uDF33", "\uD83C\uDF34", "\uD83C\uDF35", "\uD83C\uDF3E",
        "\uD83C\uDF3F", "\u2618\uFE0F", "\uD83C\uDF40", "\uD83C\uDF41",
        "\uD83C\uDF42", "\uD83C\uDF43", "\uD83C\uDF44", "\uD83E\uDEB5",
        "\uD83C\uDF1E", "\uD83C\uDF1D", "\uD83C\uDF1B", "\uD83C\uDF1C",
        "\uD83C\uDF1A", "\uD83C\uDF19", "\u2B50",       "\uD83C\uDF1F",
        "\u2728",       "\uD83D\uDCAB", "\u2600\uFE0F", "\u26C5",
        "\uD83C\uDF24\uFE0F", "\uD83C\uDF25\uFE0F", "\uD83C\uDF26\uFE0F", "\uD83C\uDF08",
        "\u2744\uFE0F", "\uD83C\uDF0A", "\uD83D\uDD25", "\u26A1",
        "\uD83C\uDF00", "\uD83C\uDF2A\uFE0F", "\uD83C\uDF0B", "\uD83C\uDF0C",
        // Food & drink
        "\uD83C\uDF4E", "\uD83C\uDF4F", "\uD83C\uDF4A", "\uD83C\uDF4B",
        "\uD83C\uDF4C", "\uD83C\uDF49", "\uD83C\uDF47", "\uD83C\uDF53",
        "\uD83C\uDF48", "\uD83C\uDF51", "\uD83E\uDD5D", "\uD83C\uDF45",
        "\uD83E\uDD51", "\uD83C\uDF55", "\uD83C\uDF54", "\uD83C\uDF5F",
        "\uD83C\uDF2D", "\uD83C\uDF2E", "\uD83C\uDF2F", "\uD83E\uDD59",
        "\uD83C\uDF5D", "\uD83C\uDF63", "\uD83C\uDF5C", "\uD83C\uDF5B",
        "\uD83C\uDF5A", "\uD83C\uDF59", "\uD83C\uDF58", "\uD83C\uDF70",
        "\uD83C\uDF82", "\uD83C\uDF66", "\uD83C\uDF69", "\uD83C\uDF6A",
        "\u2615",       "\uD83C\uDF75", "\uD83E\uDDC3", "\uD83E\uDD64",
        "\uD83C\uDF7A", "\uD83C\uDF77", "\uD83E\uDD42", "\uD83C\uDF78",
        // Travel & places
        "\uD83D\uDE80", "\uD83D\uDEF8", "\u2708\uFE0F", "\uD83D\uDE81",
        "\uD83D\uDE82", "\uD83D\uDE97", "\uD83D\uDE95", "\uD83D\uDE8C",
        "\uD83D\uDEB2", "\uD83D\uDEF5", "\u26F5",       "\uD83D\uDEA2",
        "\uD83C\uDFD4\uFE0F", "\uD83C\uDFD6\uFE0F", "\uD83C\uDFDC\uFE0F", "\uD83C\uDFDD\uFE0F",
        "\uD83C\uDFDE\uFE0F", "\uD83C\uDFDF\uFE0F", "\uD83C\uDFE0", "\uD83C\uDFF0",
        "\uD83D\uDDFC", "\uD83D\uDDFD", "\u26EA",       "\uD83D\uDD4C",
        "\uD83C\uDF05", "\uD83C\uDF04", "\uD83C\uDF03", "\uD83C\uDF06",
        "\uD83C\uDF07", "\uD83C\uDF09", "\uD83C\uDF01", "\uD83C\uDF0D",
        // Objects & tech
        "\uD83D\uDCBB", "\uD83D\uDCF1", "\u260E\uFE0F", "\uD83D\uDCFA",
        "\uD83D\uDCF7", "\uD83D\uDCF8", "\uD83C\uDFA5", "\uD83D\uDCFD\uFE0F",
        "\uD83D\uDCBF", "\uD83D\uDCE0", "\uD83D\uDCA1", "\uD83D\uDD26",
        "\uD83D\uDCDA", "\uD83D\uDCD6", "\uD83D\uDCDD", "\u270F\uFE0F",
        "\uD83D\uDD2C", "\uD83D\uDD2D", "\uD83D\uDCE1", "\uD83D\uDD11",
        "\uD83D\uDD12", "\uD83D\uDD13", "\uD83D\uDEE1\uFE0F", "\u2699\uFE0F",
        "\uD83D\uDD27", "\uD83D\uDD28", "\u2692\uFE0F", "\uD83E\uDDEA",
        "\uD83E\uDDEB", "\uD83E\uDDEC", "\uD83E\uDDED", "\uD83E\uDDAF",
        // Activities & sports
        "\u26BD",       "\uD83C\uDFC0", "\uD83C\uDFC8", "\u26BE",
        "\uD83C\uDFBE", "\uD83C\uDFD0", "\uD83C\uDFC9", "\uD83C\uDFB1",
        "\uD83C\uDFAE", "\uD83C\uDFB2", "\uD83C\uDFAF", "\uD83C\uDFB3",
        "\uD83C\uDFB5", "\uD83C\uDFB6", "\uD83C\uDFA4", "\uD83C\uDFB8",
        "\uD83C\uDFB9", "\uD83C\uDFBA", "\uD83C\uDFBB", "\uD83C\uDFAC",
        "\uD83C\uDFAD", "\uD83C\uDFA8", "\uD83C\uDFAA", "\uD83C\uDFAB",
        // Symbols & hearts
        "\u2764\uFE0F", "\uD83E\uDE77", "\uD83E\uDD0D", "\uD83D\uDDA4",
        "\uD83D\uDC9C", "\uD83D\uDC99", "\uD83D\uDC9A", "\uD83D\uDC9B",
        "\uD83E\uDDE1", "\uD83D\uDC95", "\uD83D\uDC96", "\uD83D\uDC97",
        "\uD83D\uDC93", "\uD83D\uDC9E", "\uD83D\uDC9D", "\uD83D\uDC98",
        "\u2728",       "\uD83D\uDC8E", "\uD83D\uDD2E", "\uD83E\uDDE0",
        "\u267E\uFE0F", "\u262E\uFE0F", "\u262F\uFE0F", "\u2622\uFE0F",
        "\u2B50",       "\uD83C\uDF1F", "\uD83D\uDCAF", "\u2705",
        "\u274C",       "\u2753",       "\u2757",       "\uD83D\uDCAE",
    };

    private EditText mAgentNameInput;
    private EditText mAgentCreatureInput;
    private RadioGroup mPersonalityGroup;
    private EditText mUserNameInput;
    private EditText mUserInterestsInput;
    private RadioGroup mResponseStyleGroup;
    private EditText mEmojiCustomInput;

    // Gateway auth
    private TextView mGatewayAuthStatus;
    private LinearLayout mGatewayEmailRow;
    private EditText mGatewayEmailInput;
    private Button mGatewaySendCodeButton;
    private LinearLayout mGatewayOtpRow;
    private EditText mGatewayOtpInput;
    private Button mGatewayVerifyCodeButton;
    private Button mGatewayGoogleLoginButton;

    private String mSelectedEmoji = EMOJI_OPTIONS[0];
    private boolean mUsingCustomEmoji;
    private final List<TextView> mEmojiViews = new ArrayList<>();
    private View mSelectedEmojiView;
    private int mAccentColor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getGlifLayout().setDescriptionText(getString(R.string.agent_setup_summary));

        mAgentNameInput = findViewById(R.id.agent_name_input);
        mAgentCreatureInput = findViewById(R.id.agent_creature_input);
        mPersonalityGroup = findViewById(R.id.personality_radio_group);
        mUserNameInput = findViewById(R.id.user_name_input);
        mUserInterestsInput = findViewById(R.id.user_interests_input);
        mResponseStyleGroup = findViewById(R.id.response_style_radio_group);
        mEmojiCustomInput = findViewById(R.id.agent_emoji_custom_input);

        // Gateway auth views
        mGatewayAuthStatus = findViewById(R.id.gateway_auth_status);
        mGatewayEmailRow = findViewById(R.id.gateway_email_row);
        mGatewayEmailInput = findViewById(R.id.gateway_email_input);
        mGatewaySendCodeButton = findViewById(R.id.gateway_send_code_button);
        mGatewayOtpRow = findViewById(R.id.gateway_otp_row);
        mGatewayOtpInput = findViewById(R.id.gateway_otp_input);
        mGatewayVerifyCodeButton = findViewById(R.id.gateway_verify_code_button);
        mGatewayGoogleLoginButton = findViewById(R.id.gateway_google_login_button);

        mAgentNameInput.setText(getString(R.string.agent_default_name));
        mPersonalityGroup.check(R.id.radio_casual);
        mResponseStyleGroup.check(R.id.radio_style_match);

        mAccentColor = Utils.getColorAccentDefaultColor(this);
        buildEmojiGrid();
        setupCustomEmojiInput();

        // Gateway auth listeners
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

    // ── Gateway Email OTP (via Privy SDK) ───────────────────────────────

    private void sendEmailCode() {
        final String email = mGatewayEmailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !email.contains("@")) {
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        mGatewaySendCodeButton.setEnabled(false);
        mGatewaySendCodeButton.setText("Sending\u2026");

        if (!ensurePrivyInitialized()) {
            Toast.makeText(this, "Auth not available yet — try again later",
                    Toast.LENGTH_LONG).show();
            mGatewaySendCodeButton.setEnabled(true);
            mGatewaySendCodeButton.setText(R.string.agent_gateway_send_code);
            return;
        }
        PrivyBridge.getInstance().sendEmailOtp(email, new PrivyBridgeCallback<>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    mGatewayOtpRow.setVisibility(View.VISIBLE);
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
                    Toast.makeText(AgentSetupActivity.this,
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
                    Toast.makeText(AgentSetupActivity.this,
                            "Verification failed: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ── Google OAuth (via Privy SDK) ─────────────────────────────────────

    private void launchGoogleLogin() {
        mGatewayGoogleLoginButton.setEnabled(false);

        if (!ensurePrivyInitialized()) {
            mGatewayGoogleLoginButton.setEnabled(true);
            Toast.makeText(this, "Auth not available yet — try again later",
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
                    Toast.makeText(AgentSetupActivity.this,
                            "Sign-in failed: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ── Privy login success handler ──────────────────────────────────────

    private void onPrivyLoginSuccess(PrivyBridgeResult result) {
        Log.i(TAG, "Privy login success: userId=" + result.userId
                + ", email=" + result.email);

        // Persist Privy user info to Settings.Secure for AgentConsoleApp
        Settings.Secure.putInt(getContentResolver(), "agent_gateway_logged_in", 1);
        if (result.userId != null) {
            Settings.Secure.putString(getContentResolver(),
                    "agent_privy_user_id", result.userId);
        }
        if (result.email != null) {
            Settings.Secure.putString(getContentResolver(),
                    "agent_privy_email", result.email);
        }

        // Exchange identity token for gateway access + refresh tokens
        if (result.hasIdentityToken()) {
            try {
                final AgentGatewayAuthController.ExchangeResult exchange =
                        AgentGatewayAuthController.exchangeOrDirect(
                                result.identityToken);
                AgentGatewayAuthController.persistSession(exchange);
                Log.i(TAG, "Gateway session persisted after Privy login");
            } catch (Exception e) {
                Log.e(TAG, "Gateway token exchange failed", e);
                runOnUiThread(() -> Toast.makeText(AgentSetupActivity.this,
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
            Toast.makeText(AgentSetupActivity.this,
                    "Signed in", Toast.LENGTH_SHORT).show();
        });
    }

    // ── Gateway status ──────────────────────────────────────────────────

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
            mGatewayAuthStatus.setText(getString(R.string.agent_gateway_logged_in)
                    + " (" + user.email + ")");
        } else {
            mGatewayAuthStatus.setText(R.string.agent_gateway_logged_in);
        }
        setLoginUiVisible(false);
    }

    private void setLoginUiVisible(boolean visible) {
        final int vis = visible ? View.VISIBLE : View.GONE;
        if (mGatewayEmailRow != null) mGatewayEmailRow.setVisibility(vis);
        if (mGatewayGoogleLoginButton != null) mGatewayGoogleLoginButton.setVisibility(vis);
        if (!visible && mGatewayOtpRow != null) {
            mGatewayOtpRow.setVisibility(View.GONE);
        }
    }

    // ── Emoji grid ──────────────────────────────────────────────────────

    private void buildEmojiGrid() {
        final GridLayout grid = findViewById(R.id.emoji_grid);
        grid.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        final int cellSizePx = dpToPx(40);
        final int radiusPx = dpToPx(10);

        for (int i = 0; i < EMOJI_OPTIONS.length; i++) {
            final String emoji = EMOJI_OPTIONS[i];
            final TextView tv = new TextView(this);
            tv.setText(emoji);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            tv.setGravity(Gravity.CENTER);
            tv.setClickable(true);
            tv.setFocusable(false);

            final GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(radiusPx);
            bg.setColor(0x00000000);
            bg.setStroke(0, 0x00000000);
            tv.setBackground(bg);

            final GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSizePx;
            params.height = cellSizePx;
            params.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED);
            tv.setLayoutParams(params);

            tv.setOnClickListener(v -> selectGridEmoji(emoji, tv));
            grid.addView(tv);
            mEmojiViews.add(tv);

            if (i == 0) {
                selectGridEmoji(emoji, tv);
            }
        }
    }

    private void setupCustomEmojiInput() {
        mEmojiCustomInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                final String custom = s.toString().trim();
                if (!TextUtils.isEmpty(custom)) {
                    deselectGrid();
                    mSelectedEmoji = custom;
                    mUsingCustomEmoji = true;
                } else if (mUsingCustomEmoji) {
                    mUsingCustomEmoji = false;
                    if (!mEmojiViews.isEmpty()) {
                        selectGridEmoji(EMOJI_OPTIONS[0], mEmojiViews.get(0));
                    }
                }
            }
        });
    }

    private void selectGridEmoji(String emoji, TextView view) {
        deselectGrid();
        mSelectedEmoji = emoji;
        mSelectedEmojiView = view;
        mUsingCustomEmoji = false;

        final GradientDrawable bg = (GradientDrawable) view.getBackground();
        bg.setColor(ColorStateList.valueOf(mAccentColor).withAlpha(50).getDefaultColor());
        bg.setStroke(dpToPx(2), mAccentColor);

        if (mEmojiCustomInput.getText().length() > 0) {
            mEmojiCustomInput.setText("");
        }
    }

    private void deselectGrid() {
        if (mSelectedEmojiView != null) {
            final GradientDrawable prevBg =
                    (GradientDrawable) mSelectedEmojiView.getBackground();
            prevBg.setColor(0x00000000);
            prevBg.setStroke(0, 0x00000000);
            mSelectedEmojiView = null;
        }
    }

    // ── Setup wizard navigation ─────────────────────────────────────────

    @Override
    protected void onNextPressed() {
        writeWorkspaceFiles();
        super.onNextPressed();
    }

    private void writeWorkspaceFiles() {
        final String agentName = getAgentName();
        final String agentCreature = getAgentCreature();
        final String agentEmoji = mSelectedEmoji;
        final String userName = getUserName();
        final String userInterests = getUserInterests();
        final String[] personality = getPersonality();
        final String commStyle = getResponseStyle();
        final String timezone = TimeZone.getDefault().getID();
        final String language = Locale.getDefault().getDisplayLanguage();

        final String identityMd = buildIdentityMd(agentName, agentCreature, agentEmoji,
                personality[0], personality[1]);
        final String userMd = buildUserMd(userName, timezone, language, commStyle, userInterests);

        Settings.Secure.putString(getContentResolver(),
                "agent_wizard_identity", identityMd);
        Settings.Secure.putString(getContentResolver(),
                "agent_wizard_user", userMd);
        Settings.Secure.putInt(getContentResolver(),
                "agent_wizard_complete", 1);

        Log.i(TAG, "Agent workspace data written to Settings.Secure");
    }

    private String getAgentName() {
        String name = mAgentNameInput.getText().toString().trim();
        return TextUtils.isEmpty(name) ? getString(R.string.agent_default_name) : name;
    }

    private String getAgentCreature() {
        String creature = mAgentCreatureInput.getText().toString().trim();
        return TextUtils.isEmpty(creature) ? "AI agent, living on a phone" : creature;
    }

    private String getUserName() {
        String name = mUserNameInput.getText().toString().trim();
        return TextUtils.isEmpty(name) ? "(Not yet known)" : name;
    }

    private String getUserInterests() {
        String interests = mUserInterestsInput.getText().toString().trim();
        return TextUtils.isEmpty(interests)
                ? "(You're learning about a person, not building a dossier. Keep it natural.)"
                : interests;
    }

    private String[] getPersonality() {
        int checkedId = mPersonalityGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_professional) {
            return new String[]{
                "Professional \u2014 efficient, focused, and to the point",
                "Clear, measured, authoritative"
            };
        } else if (checkedId == R.id.radio_warm) {
            return new String[]{
                "Warm \u2014 thoughtful, empathetic, always looking out for you",
                "Gentle, supportive, encouraging"
            };
        } else if (checkedId == R.id.radio_playful) {
            return new String[]{
                "Playful \u2014 sharp humor, personality, and a bit of edge",
                "Quick, clever, irreverent"
            };
        } else {
            return new String[]{
                "Casual \u2014 like talking to a smart friend who happens to live in your phone",
                "Warm, relaxed, conversational"
            };
        }
    }

    private String getResponseStyle() {
        int checkedId = mResponseStyleGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_style_brief) {
            return "Prefers brief, direct responses. Don't over-explain.";
        } else if (checkedId == R.id.radio_style_detailed) {
            return "Prefers detailed, thorough responses. Context is appreciated.";
        } else {
            return "Match the user's style. Mirror their energy and verbosity.";
        }
    }

    private static String buildIdentityMd(String name, String creature, String emoji,
            String vibe, String voice) {
        return "# Identity\n\n"
                + "## Name\n" + name + "\n\n"
                + "## Creature\n" + creature + "\n\n"
                + "## Vibe\n" + vibe + "\n\n"
                + "## Emoji\n" + emoji + "\n\n"
                + "## Avatar\n(Describe what you'd look like if you could be seen)\n\n"
                + "## Voice\n" + voice + "\n\n"
                + "## One-liner\n(How would you introduce yourself in one sentence?)\n\n"
                + "---\n\n"
                + "_This isn't just metadata. It's the start of figuring out who you are._\n"
                + "_Fill these in during your first conversation or whenever inspiration "
                + "strikes._\n"
                + "_Change them whenever they stop feeling right._\n";
    }

    private static String buildUserMd(String name, String timezone, String language,
            String commStyle, String interests) {
        final String callThem = "(Not yet known)".equals(name) ? "(Not yet known)" : name;
        return "# User\n\n"
                + "## Name\n" + name + "\n\n"
                + "## What to call them\n" + callThem + "\n\n"
                + "## Pronouns\n(Not yet known)\n\n"
                + "## Timezone\n" + timezone + "\n\n"
                + "## Language\n" + language + "\n\n"
                + "## Notes\n" + interests + "\n\n"
                + "## Context\n(What are they working on? What do they care about? "
                + "What's going on in their life?)\n\n"
                + "## Communication Style\n" + commStyle + "\n\n"
                + "## Important People\n(Names that come up. Relationships. "
                + "Don't be weird about it \u2014 just enough to be helpful.)\n\n"
                + "## Routines\n(Morning patterns, work hours, recurring things. "
                + "Stuff that helps you anticipate.)\n";
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_agent;
    }

    @Override
    protected int getTitleResId() {
        return R.string.setup_agent;
    }

    @Override
    protected int getIconResId() {
        return R.drawable.ic_agent;
    }
}
