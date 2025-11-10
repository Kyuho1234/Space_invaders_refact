package org.newdawn.spaceinvaders.firebase;

import org.json.JSONObject;
import java.io.IOException;

/**
 * Firebase User Data Manager
 * SRP (Single Responsibility Principle) - Handles only user data operations
 * DIP (Dependency Inversion Principle) - Depends on abstractions (FirebaseHttpClient)
 */
public class FirebaseUserManager {
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_POINTS = "points";
    private static final String FIELD_HIGHEST_SCORE = "highest_score";
    private static final String FIELD_STRING_VALUE = "stringValue";
    private static final String FIELD_INTEGER_VALUE = "integerValue";
    private static final String FIELD_FIELDS = "fields";
    private static final String FIELD_MAX_CLEARED_STAGE = "max_cleared_stage";

    private final FirebaseHttpClient httpClient;
    private final String projectId;
    private final String apiKey;
    private final String documentsBase;

    public FirebaseUserManager(FirebaseHttpClient httpClient, String projectId, String apiKey) {
        this.httpClient = httpClient;
        this.projectId = projectId;
        this.apiKey = apiKey;
        this.documentsBase = String.format(
            "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents",
            projectId
        );
    }

    /**
     * Update user points in Firestore
     */
    public boolean updateUserPoints(String localId, int points) {
        if (localId == null || documentsBase == null) return false;

        try {
            String url = documentsBase + "/users/" + localId + "?key=" + apiKey;
            JSONObject existing = httpClient.get(url);
            if (existing == null || !existing.has(FIELD_FIELDS)) return false;

            JSONObject fields = buildPointsUpdateFields(points, existing.getJSONObject(FIELD_FIELDS));
            return executePointsUpdate(url, fields);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Build fields JSON for points update
     */
    private JSONObject buildPointsUpdateFields(int points, JSONObject existingFields) {
        JSONObject fields = new JSONObject();

        // Preserve email
        if (existingFields.has(FIELD_EMAIL)) {
            fields.put(FIELD_EMAIL, existingFields.getJSONObject(FIELD_EMAIL));
        }

        // Update points
        JSONObject pointsValue = new JSONObject();
        pointsValue.put(FIELD_INTEGER_VALUE, points);
        fields.put(FIELD_POINTS, pointsValue);

        // Update highest_score if current points are higher
        int currentHighest = 0;
        if (existingFields.has(FIELD_HIGHEST_SCORE)) {
            currentHighest = existingFields.getJSONObject(FIELD_HIGHEST_SCORE)
                                          .optInt(FIELD_INTEGER_VALUE, 0);
        }

        int newHighest = Math.max(currentHighest, points);
        JSONObject highestValue = new JSONObject();
        highestValue.put(FIELD_INTEGER_VALUE, newHighest);
        fields.put(FIELD_HIGHEST_SCORE, highestValue);

        return fields;
    }

    /**
     * Execute points update with PATCH request
     */
    private boolean executePointsUpdate(String url, JSONObject fields) throws IOException {
        JSONObject body = new JSONObject();
        body.put(FIELD_FIELDS, fields);

        JSONObject res = httpClient.patch(url, body);
        return res != null;
    }

    /**
     * Get user's highest score
     */
    public int getUserHighestScore(String localId) {
        if (localId == null || documentsBase == null) return 0;

        try {
            String url = documentsBase + "/users/" + localId + "?key=" + apiKey;
            JSONObject doc = httpClient.get(url);
            if (doc != null && doc.has(FIELD_FIELDS)) {
                JSONObject fields = doc.getJSONObject(FIELD_FIELDS);
                if (fields.has(FIELD_HIGHEST_SCORE)) {
                    return fields.getJSONObject(FIELD_HIGHEST_SCORE)
                                 .optInt(FIELD_INTEGER_VALUE, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get user's current points
     */
    public int getUserPoints(String localId) {
        if (localId == null || documentsBase == null) return 0;

        try {
            String url = documentsBase + "/users/" + localId + "?key=" + apiKey;
            JSONObject doc = httpClient.get(url);
            if (doc != null && doc.has(FIELD_FIELDS)) {
                JSONObject fields = doc.getJSONObject(FIELD_FIELDS);
                if (fields.has(FIELD_POINTS)) {
                    return fields.getJSONObject(FIELD_POINTS)
                                 .optInt(FIELD_INTEGER_VALUE, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get user's max cleared stage
     */
    public int getUserMaxClearedStage(String localId) {
        if (localId == null || documentsBase == null) return 0;

        try {
            String url = documentsBase + "/users/" + localId + "?key=" + apiKey;
            JSONObject doc = httpClient.get(url);
            if (doc != null && doc.has(FIELD_FIELDS)) {
                JSONObject fields = doc.getJSONObject(FIELD_FIELDS);
                if (fields.has(FIELD_MAX_CLEARED_STAGE)) {
                    return fields.getJSONObject(FIELD_MAX_CLEARED_STAGE)
                                 .optInt(FIELD_INTEGER_VALUE, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Update user's max cleared stage
     */
    public boolean updateMaxClearedStage(String localId, int stage) {
        if (localId == null || documentsBase == null) return false;

        try {
            String url = documentsBase + "/users/" + localId + "?key=" + apiKey;
            JSONObject existing = httpClient.get(url);
            if (existing == null || !existing.has(FIELD_FIELDS)) return false;

            JSONObject fields = existing.getJSONObject(FIELD_FIELDS);

            // Only update if new stage is higher
            int currentMax = 0;
            if (fields.has(FIELD_MAX_CLEARED_STAGE)) {
                currentMax = fields.getJSONObject(FIELD_MAX_CLEARED_STAGE)
                                   .optInt(FIELD_INTEGER_VALUE, 0);
            }

            if (stage > currentMax) {
                JSONObject stageValue = new JSONObject();
                stageValue.put(FIELD_INTEGER_VALUE, stage);
                fields.put(FIELD_MAX_CLEARED_STAGE, stageValue);

                JSONObject body = new JSONObject();
                body.put(FIELD_FIELDS, fields);

                JSONObject res = httpClient.patch(url, body);
                return res != null;
            }
            return true; // Already have higher or equal stage cleared
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
