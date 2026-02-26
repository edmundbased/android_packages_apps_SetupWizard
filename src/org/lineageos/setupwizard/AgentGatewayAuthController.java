/*
 * SPDX-FileCopyrightText: 2026 The BasedOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Shared helper for managed gateway auth during setup wizard.
 *
 * Supports two native login flows (no browser-based auth portal):
 *   1. Email OTP — fully in-app via PrivyAuthClient → auth proxy → Privy
 *   2. Google OAuth — opens browser for Google consent only, callback returns here
 */
final class AgentGatewayAuthController {
    private static final String TAG = "SwGatewayAuth";

    private static final String PROP_BASE_URL = "persist.agent.llm_base_url";
    private static final String PROP_API_KEY = "persist.agent.llm_api_key";
    private static final String PROP_API_KEY_SOURCE = "persist.agent.llm_api_key_source";
    private static final String PROP_GATEWAY_REFRESH_TOKEN = "persist.agent.gateway_refresh_token";
    private static final String PROP_GATEWAY_DEVICE_ID = "persist.agent.gateway_device_id";
    private static final String PROP_GATEWAY_ACCOUNT_ID = "persist.agent.gateway_account_id";
    private static final String PROP_AUTH_STATE = "persist.agent.gateway_auth_state";
    private static final String DEFAULT_BASE_URL = "https://ai-gateway.vercel.sh/v1";

    private static final String RUNTIME_DIR = "/data/misc/agent/runtime";
    private static final String API_KEY_FILE = RUNTIME_DIR + "/llm_api_key";
    private static final String REFRESH_FILE = RUNTIME_DIR + "/gateway_refresh_token";
    private static final String STATE_FILE = RUNTIME_DIR + "/gateway_auth_state";

    static final String CALLBACK_SCHEME = "basedos-setup";
    static final String CALLBACK_HOST = "auth";
    static final String CALLBACK_PATH = "/callback";

    static final class ExchangeResult {
        final String accessToken;
        final String refreshToken;
        final String accountId;

        ExchangeResult(String accessToken, String refreshToken, String accountId) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accountId = accountId;
        }
    }

    private AgentGatewayAuthController() {
    }

    // ── Email OTP flow ──────────────────────────────────────────────────

    /** Send OTP to email via the auth proxy. Callback runs on a background thread. */
    static void sendEmailOtp(String email, PrivyAuthClient.Callback<Void> callback) {
        PrivyAuthClient.sendEmailOtp(email, callback);
    }

    /**
     * Verify email OTP via the auth proxy, then exchange the identity token
     * for gateway credentials and persist the session.
     */
    static void verifyEmailOtp(String email, String code,
            PrivyAuthClient.Callback<ExchangeResult> callback) {
        PrivyAuthClient.verifyEmailOtp(email, code, new PrivyAuthClient.Callback<>() {
            @Override
            public void onSuccess(PrivyAuthClient.VerifyResult result) {
                try {
                    final ExchangeResult exchange = exchangeOrDirect(result.identityToken);
                    persistSession(exchange);
                    callback.onSuccess(exchange);
                } catch (Exception e) {
                    Log.e(TAG, "Token exchange after email verify failed", e);
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    // ── Google OAuth flow ───────────────────────────────────────────────

    /**
     * Start Google OAuth: get the OAuth URL from the auth proxy and open it in browser.
     * The callback will arrive at AgentGatewayAuthCallbackActivity via deep link.
     */
    static void initiateGoogleOAuth(Activity activity, String sourceTag) {
        final String state = UUID.randomUUID().toString().replace("-", "");
        SystemProperties.set(PROP_AUTH_STATE, state);
        try {
            writeFile(STATE_FILE, state);
        } catch (IOException ignored) {
        }

        final String redirectUri = getRedirectUri();
        PrivyAuthClient.getOAuthUrl("google", redirectUri, state,
                new PrivyAuthClient.Callback<>() {
            @Override
            public void onSuccess(String oauthUrl) {
                activity.runOnUiThread(() -> {
                    try {
                        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl));
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to open OAuth URL", e);
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Failed to get OAuth URL: " + message);
            }
        });
    }

    // ── Shared helpers ──────────────────────────────────────────────────

    static String getRedirectUri() {
        return CALLBACK_SCHEME + "://" + CALLBACK_HOST + CALLBACK_PATH;
    }

    static boolean hasStoredSession() {
        return !TextUtils.isEmpty(readFileValue(API_KEY_FILE))
                && !TextUtils.isEmpty(readFileValue(REFRESH_FILE));
    }

    static String getStoredAccountId() {
        return SystemProperties.get(PROP_GATEWAY_ACCOUNT_ID, "");
    }

    /**
     * Try exchanging the identity token via the gateway. If the gateway doesn't
     * support the exchange endpoint, fall back to using the identity token directly.
     */
    static ExchangeResult exchangeOrDirect(String identityToken) throws IOException {
        try {
            return exchangeIdentityToken(identityToken);
        } catch (Exception e) {
            Log.w(TAG, "Gateway exchange failed, using direct auth: " + e.getMessage());
            return new ExchangeResult(identityToken, identityToken, "");
        }
    }

    static ExchangeResult exchangeIdentityToken(String identityToken)
            throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            final URL url = new URL(resolveGatewayApiRoot() + "/auth/privy/exchange");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20_000);
            connection.setReadTimeout(20_000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            final JSONObject payload = new JSONObject();
            payload.put("identityToken", identityToken);
            final byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }

            final int status = connection.getResponseCode();
            final String response = readBody(status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream());
            if (status < 200 || status >= 300) {
                throw new IOException("Gateway exchange failed HTTP " + status + ": " + response);
            }
            final JSONObject parsed = new JSONObject(response);
            final String accessToken = parsed.optString("accessToken", "");
            final String refreshToken = parsed.optString("refreshToken", "");
            final String accountId = parsed.optString("accountId", "");
            if (TextUtils.isEmpty(accessToken) || TextUtils.isEmpty(refreshToken)) {
                throw new IOException("Gateway exchange missing tokens");
            }
            return new ExchangeResult(accessToken, refreshToken, accountId);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static void persistSession(ExchangeResult result) throws IOException {
        if (result == null || TextUtils.isEmpty(result.accessToken)
                || TextUtils.isEmpty(result.refreshToken)) {
            throw new IOException("Missing gateway token payload");
        }
        writeFile(API_KEY_FILE, result.accessToken);
        writeFile(REFRESH_FILE, result.refreshToken);
        SystemProperties.set(PROP_API_KEY_SOURCE, "file");
        SystemProperties.set(PROP_API_KEY, "");
        SystemProperties.set(PROP_GATEWAY_REFRESH_TOKEN, "");
        SystemProperties.set(PROP_GATEWAY_DEVICE_ID, "");
        SystemProperties.set(PROP_GATEWAY_ACCOUNT_ID,
                result.accountId != null ? result.accountId : "");
    }

    static boolean validateAndConsumeState(String callbackState) {
        final String returnedState = callbackState != null ? callbackState.trim() : "";
        final String expectedFileState = readFileValue(STATE_FILE);
        final String expectedPropState = SystemProperties.get(PROP_AUTH_STATE, "").trim();
        deleteIfExists(STATE_FILE);
        SystemProperties.set(PROP_AUTH_STATE, "");
        if (TextUtils.isEmpty(returnedState)) {
            return false;
        }
        if (!TextUtils.isEmpty(expectedFileState) && returnedState.equals(expectedFileState)) {
            return true;
        }
        return !TextUtils.isEmpty(expectedPropState) && returnedState.equals(expectedPropState);
    }

    private static String resolveGatewayApiRoot() {
        String baseUrl = SystemProperties.get(PROP_BASE_URL, DEFAULT_BASE_URL);
        if (TextUtils.isEmpty(baseUrl)) {
            baseUrl = DEFAULT_BASE_URL;
        }
        baseUrl = baseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl;
        }
        return baseUrl + "/v1";
    }

    private static void writeFile(String path, String value) throws IOException {
        final File file = new File(path);
        final File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create " + parent.getAbsolutePath());
        }
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.write('\n');
            out.flush();
        }
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    private static void deleteIfExists(String path) {
        final File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        final StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private static String readFileValue(String path) {
        final File file = new File(path);
        if (!file.exists()) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
            final String line = reader.readLine();
            return line != null ? line.trim() : "";
        } catch (IOException e) {
            return "";
        }
    }
}
