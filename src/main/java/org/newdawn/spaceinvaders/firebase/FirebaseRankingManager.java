package org.newdawn.spaceinvaders.firebase;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Ranking Manager
 * SRP (Single Responsibility Principle) - Handles only ranking/leaderboard operations
 * DIP (Dependency Inversion Principle) - Depends on abstractions (FirebaseHttpClient)
 */
public class FirebaseRankingManager {
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_HIGHEST_SCORE = "highest_score";
    private static final String FIELD_STRING_VALUE = "stringValue";
    private static final String FIELD_INTEGER_VALUE = "integerValue";
    private static final String FIELD_FIELDS = "fields";
    private static final String FIELD_NAME = "name";
    private static final String DOCUMENTS = "documents";

    private final FirebaseHttpClient httpClient;
    private final String projectId;
    private final String apiKey;
    private final String documentsBase;

    public FirebaseRankingManager(FirebaseHttpClient httpClient, String projectId, String apiKey) {
        this.httpClient = httpClient;
        this.projectId = projectId;
        this.apiKey = apiKey;
        this.documentsBase = String.format(
            "https://firestore.googleapis.com/v1/projects/%s/databases/(default)/documents",
            projectId
        );
    }

    /**
     * Get top scores from leaderboard
     */
    public List<Map<String, Object>> getTopScores(int limit) {
        if (documentsBase == null) return new ArrayList<>();
        List<Map<String, Object>> rankingData = new ArrayList<>();

        try {
            JSONObject structuredQuery = buildTopScoresQuery(limit);
            JSONObject body = new JSONObject().put("structuredQuery", structuredQuery);

            String url = documentsBase + ":runQuery";
            JSONObject resWrapper = httpClient.post(url, body);

            if (resWrapper != null && resWrapper.has(DOCUMENTS)) {
                rankingData = parseTopScoresResponse(resWrapper.getJSONArray(DOCUMENTS));
            }
        } catch (Exception e) {
            System.err.println("Error in getTopScores: " + e.getMessage());
            e.printStackTrace();
        }
        return rankingData;
    }

    /**
     * Build Firestore structured query for top scores
     */
    private JSONObject buildTopScoresQuery(int limit) {
        JSONObject query = new JSONObject();

        // Select users collection
        JSONArray from = new JSONArray();
        JSONObject collectionSelector = new JSONObject();
        collectionSelector.put("collectionId", "users");
        from.put(collectionSelector);
        query.put("from", from);

        // Order by highest_score descending
        JSONArray orderBy = new JSONArray();
        JSONObject order = new JSONObject();
        JSONObject fieldRef = new JSONObject();
        fieldRef.put("fieldPath", FIELD_HIGHEST_SCORE);
        order.put("field", fieldRef);
        order.put("direction", "DESCENDING");
        orderBy.put(order);
        query.put("orderBy", orderBy);

        // Limit results
        query.put("limit", limit);

        return query;
    }

    /**
     * Parse top scores response from Firestore
     */
    private List<Map<String, Object>> parseTopScoresResponse(JSONArray documents) {
        List<Map<String, Object>> rankingData = new ArrayList<>();

        for (int i = 0; i < documents.length(); i++) {
            JSONObject doc = documents.optJSONObject(i);
            if (doc != null && doc.has("document")) {
                JSONObject docObj = doc.getJSONObject("document");
                if (docObj.has(FIELD_FIELDS)) {
                    JSONObject fields = docObj.getJSONObject(FIELD_FIELDS);
                    Map<String, Object> entry = extractRankingEntry(fields, docObj);
                    rankingData.add(entry);
                }
            }
        }

        return rankingData;
    }

    /**
     * Extract ranking entry from Firestore document fields
     */
    private Map<String, Object> extractRankingEntry(JSONObject fields, JSONObject docObj) {
        Map<String, Object> entry = new HashMap<>();

        // Extract email
        if (fields.has(FIELD_EMAIL)) {
            String email = fields.getJSONObject(FIELD_EMAIL)
                                 .optString(FIELD_STRING_VALUE, "Unknown");
            entry.put(FIELD_EMAIL, email);
        }

        // Extract highest_score
        if (fields.has(FIELD_HIGHEST_SCORE)) {
            int score = fields.getJSONObject(FIELD_HIGHEST_SCORE)
                              .optInt(FIELD_INTEGER_VALUE, 0);
            entry.put(FIELD_HIGHEST_SCORE, score);
        }

        // Extract document name (user ID)
        if (docObj.has(FIELD_NAME)) {
            String fullPath = docObj.getString(FIELD_NAME);
            String[] parts = fullPath.split("/");
            if (parts.length > 0) {
                entry.put("userId", parts[parts.length - 1]);
            }
        }

        return entry;
    }

    /**
     * Get user's ranking position (1-based)
     */
    public int getUserRanking(String localId, int userHighestScore) {
        if (localId == null || documentsBase == null) return -1;

        try {
            List<Map<String, Object>> topScores = getTopScores(100); // Get top 100
            for (int i = 0; i < topScores.size(); i++) {
                Map<String, Object> entry = topScores.get(i);
                if (localId.equals(entry.get("userId"))) {
                    return i + 1; // 1-based ranking
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Not in top 100
    }
}
