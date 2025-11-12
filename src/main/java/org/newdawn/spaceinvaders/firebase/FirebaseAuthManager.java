package org.newdawn.spaceinvaders.firebase;

import org.json.JSONObject;
import java.io.IOException;

/**
 * Firebase 인증 관련 기능만 담당
 * SRP (Single Responsibility Principle) 적용
 */
public class FirebaseAuthManager {
    // String constants
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_RETURN_SECURE_TOKEN = "returnSecureToken";

    private final FirebaseHttpClient httpClient;
    private final String apiKey;

    private String idToken;
    private String refreshToken;
    private String localId;
    private String email;
    private long expiresAtMs;

    public FirebaseAuthManager(FirebaseHttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
    }

    public boolean isLoggedIn() {
        return idToken != null;
    }

    public String getCurrentUserEmail() {
        return email;
    }

    public String getUid() {
        return localId;
    }

    public String getIdToken() {
        return idToken;
    }

    public void signOut() {
        idToken = null;
        refreshToken = null;
        localId = null;
        email = null;
        expiresAtMs = 0L;
    }

    public boolean signInWithEmailPassword(String email, String password) {
        try {
            JSONObject body = new JSONObject()
                    .put(FIELD_EMAIL, email)
                    .put("password", password)
                    .put(FIELD_RETURN_SECURE_TOKEN, true);

            String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;
            JSONObject res = httpClient.post(url, body);

            if (res == null) return false;
            applyAuthResponse(res);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean signUpWithEmailPassword(String email, String password) {
        try {
            JSONObject body = new JSONObject()
                    .put(FIELD_EMAIL, email)
                    .put("password", password)
                    .put(FIELD_RETURN_SECURE_TOKEN, true);

            String url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey;
            JSONObject res = httpClient.post(url, body);

            if (res == null) return false;
            applyAuthResponse(res);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDisplayName(String displayName) {
        if (!isLoggedIn()) return false;

        try {
            JSONObject body = new JSONObject()
                    .put("idToken", idToken)
                    .put("displayName", displayName)
                    .put(FIELD_RETURN_SECURE_TOKEN, true);

            String url = "https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + apiKey;
            JSONObject res = httpClient.post(url, body);
            return res != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void applyAuthResponse(JSONObject res) {
        this.idToken = res.optString("idToken", null);
        this.refreshToken = res.optString("refreshToken", null);
        this.localId = res.optString("localId", null);
        this.email = res.optString(FIELD_EMAIL, null);

        long expiresInSec = 0L;
        try {
            expiresInSec = Long.parseLong(res.optString("expiresIn", "0"));
        } catch (Exception ignore) {}

        this.expiresAtMs = (expiresInSec > 0) ? System.currentTimeMillis() + expiresInSec * 1000L : 0L;
    }
}
