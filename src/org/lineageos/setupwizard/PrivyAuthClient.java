/*
 * SPDX-FileCopyrightText: 2026 The GrandiOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client for the ConsciOS auth proxy (Cloudflare Worker).
 * All methods run blocking I/O and must be called from a background thread.
 */
final class PrivyAuthClient {
    private static final String TAG = "PrivyAuthClient";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    private static final String PROP_AUTH_PROXY_URL = "persist.agent.gateway_auth_url";
    private static final String DEFAULT_AUTH_PROXY_URL = "https://conscios-auth.admin-2a3.workers.dev";

    interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    static final class VerifyResult {
        final String identityToken;
        final String userId;
        final String email;

        VerifyResult(String identityToken, String userId, String email) {
            this.identityToken = identityToken;
            this.userId = userId;
            this.email = email;
        }
    }

    private PrivyAuthClient() {
    }

    /** Send OTP to the given email address via the auth proxy. */
    static void sendEmailOtp(String email, Callback<Void> callback) {
        new Thread(() -> {
            try {
                final JSONObject body = new JSONObject();
                body.put("email", email);
                final JSONObject resp = postJson(resolveProxyUrl() + "/auth/email/init", body);
                if (resp.optBoolean("success", false)) {
                    callback.onSuccess(null);
                } else {
                    callback.onError(resp.optString("error", "Failed to send code"));
                }
            } catch (Exception e) {
                Log.e(TAG, "sendEmailOtp failed", e);
                callback.onError(e.getMessage());
            }
        }, "privy-email-init").start();
    }

    /** Verify email OTP and return identity_token via the auth proxy. */
    static void verifyEmailOtp(String email, String code, Callback<VerifyResult> callback) {
        new Thread(() -> {
            try {
                final JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("code", code);
                final JSONObject resp = postJson(resolveProxyUrl() + "/auth/email/verify", body);
                final String identityToken = resp.optString("identity_token", "");
                if (TextUtils.isEmpty(identityToken)) {
                    callback.onError(resp.optString("error", "Verification failed"));
                    return;
                }
                callback.onSuccess(new VerifyResult(
                        identityToken,
                        resp.optString("user_id", ""),
                        resp.optString("email", email)));
            } catch (Exception e) {
                Log.e(TAG, "verifyEmailOtp failed", e);
                callback.onError(e.getMessage());
            }
        }, "privy-email-verify").start();
    }

    /** Get OAuth provider URL from the auth proxy. */
    static void getOAuthUrl(String provider, String redirectUri, String state,
            Callback<String> callback) {
        new Thread(() -> {
            try {
                final StringBuilder urlBuilder = new StringBuilder(resolveProxyUrl());
                urlBuilder.append("/auth/oauth/init");
                urlBuilder.append("?provider=").append(urlEncode(provider));
                urlBuilder.append("&redirect_uri=").append(urlEncode(redirectUri));
                urlBuilder.append("&state=").append(urlEncode(state));

                final JSONObject resp = getJson(urlBuilder.toString());
                final String oauthUrl = resp.optString("url", "");
                if (TextUtils.isEmpty(oauthUrl)) {
                    callback.onError(resp.optString("error", "Failed to get OAuth URL"));
                    return;
                }
                callback.onSuccess(oauthUrl);
            } catch (Exception e) {
                Log.e(TAG, "getOAuthUrl failed", e);
                callback.onError(e.getMessage());
            }
        }, "privy-oauth-init").start();
    }

    static String resolveProxyUrl() {
        final String explicit = SystemProperties.get(PROP_AUTH_PROXY_URL, "").trim();
        if (!TextUtils.isEmpty(explicit)) {
            return explicit;
        }
        return DEFAULT_AUTH_PROXY_URL;
    }

    private static JSONObject postJson(String urlStr, JSONObject body)
            throws IOException, JSONException {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(urlStr);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            final byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
            }

            return readResponse(conn);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static JSONObject getJson(String urlStr) throws IOException, JSONException {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(urlStr);
            conn.setRequestMethod("GET");
            return readResponse(conn);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpURLConnection openConnection(String urlStr) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        return conn;
    }

    private static JSONObject readResponse(HttpURLConnection conn)
            throws IOException, JSONException {
        final int status = conn.getResponseCode();
        final InputStream stream = (status >= 200 && status < 300)
                ? conn.getInputStream() : conn.getErrorStream();
        final String text = readBody(stream);
        if (status < 200 || status >= 300) {
            try {
                return new JSONObject(text);
            } catch (JSONException ignored) {
                throw new IOException("HTTP " + status + ": " + text);
            }
        }
        return new JSONObject(text);
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static String urlEncode(String value) {
        if (value == null) return "";
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }
}
