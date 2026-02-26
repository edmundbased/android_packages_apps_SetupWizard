/*
 * SPDX-FileCopyrightText: 2026 The BasedOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Deep-link callback for setup wizard managed-gateway auth (Google OAuth flow).
 */
public class AgentGatewayAuthCallbackActivity extends Activity {
    private static final String TAG = "SwGatewayCallback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        final Uri data = intent != null ? intent.getData() : null;
        if (data == null) {
            finish();
            return;
        }

        final Map<String, String> fragment = parseFragmentParams(data);
        final String error = firstNonEmpty(
                data.getQueryParameter("error"),
                data.getQueryParameter("error_code"),
                fragment.get("error"));
        if (!TextUtils.isEmpty(error)) {
            Toast.makeText(this, "Login failed: " + error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final String callbackState = firstNonEmpty(
                data.getQueryParameter("state"),
                fragment.get("state"));
        if (!AgentGatewayAuthController.validateAndConsumeState(callbackState)) {
            Toast.makeText(this, "Login failed: invalid state", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final String skipped = firstNonEmpty(
                data.getQueryParameter("skipped"),
                fragment.get("skipped"));
        if ("1".equals(skipped) || "true".equalsIgnoreCase(skipped)) {
            Toast.makeText(this, "Cloud login skipped", Toast.LENGTH_SHORT).show();
            final Intent next = new Intent(this, AgentSetupActivity.class);
            next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(next);
            finish();
            return;
        }

        final String identityToken = firstNonEmpty(
                data.getQueryParameter("identity_token"),
                data.getQueryParameter("identityToken"),
                fragment.get("identity_token"),
                fragment.get("identityToken"),
                fragment.get("id_token"));

        new Thread(() -> {
            try {
                if (TextUtils.isEmpty(identityToken)) {
                    throw new IllegalStateException("Missing callback tokens");
                }

                // Try gateway exchange; fall back to direct auth if gateway
                // doesn't support the exchange endpoint
                final AgentGatewayAuthController.ExchangeResult result =
                        AgentGatewayAuthController.exchangeOrDirect(identityToken);

                AgentGatewayAuthController.persistSession(result);
                Settings.Secure.putInt(getContentResolver(), "agent_gateway_logged_in", 1);
                runOnUiThread(() -> {
                    final boolean isDirect = identityToken.equals(result.accessToken);
                    final String msg = isDirect
                            ? "Signed in (direct auth)" : "Cloud account connected";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    final Intent next = new Intent(this, AgentSetupActivity.class);
                    next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(next);
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Gateway auth callback failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Cloud login failed", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }, "setup-gateway-auth").start();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private static Map<String, String> parseFragmentParams(Uri uri) {
        final Map<String, String> out = new HashMap<>();
        if (uri == null) {
            return out;
        }
        final String fragment = uri.getFragment();
        if (TextUtils.isEmpty(fragment)) {
            return out;
        }
        final String[] parts = fragment.split("&");
        for (String part : parts) {
            if (TextUtils.isEmpty(part)) {
                continue;
            }
            final int eq = part.indexOf('=');
            final String key;
            final String value;
            if (eq >= 0) {
                key = decode(part.substring(0, eq));
                value = decode(part.substring(eq + 1));
            } else {
                key = decode(part);
                value = "";
            }
            if (!TextUtils.isEmpty(key)) {
                out.put(key, value);
            }
        }
        return out;
    }

    private static String decode(String raw) {
        if (raw == null) {
            return "";
        }
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return raw;
        }
    }
}
