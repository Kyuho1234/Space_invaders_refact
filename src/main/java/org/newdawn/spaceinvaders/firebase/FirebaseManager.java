package org.newdawn.spaceinvaders.firebase;

import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public final class FirebaseManager {

    // String constants
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_POINTS = "points";
    private static final String FIELD_HIGHEST_SCORE = "highest_score";
    private static final String FIELD_MAX_CLEARED_STAGE = "maxClearedStage";
    private static final String FIELD_ITEM_ID = "itemId";
    private static final String FIELD_FIELDS = "fields";
    private static final String FIELD_DOCUMENTS = "documents";
    private static final String FIELD_STRING_VALUE = "stringValue";
    private static final String FIELD_INTEGER_VALUE = "integerValue";
    private static final String FIELD_RETURN_SECURE_TOKEN = "returnSecureToken";

    private static final String PATH_PROJECTS = "/projects/";
    private static final String PATH_USERS = "/users/";
    private static final String PATH_ITEMS = "/items";
    private static final String PATH_DATABASES = "/databases/";
    private static final String PATH_USERS_QUERY = PATH_USERS_QUERY;
    private static final String PATH_ITEMS_QUERY = PATH_ITEMS_QUERY;
    private static final String PARAM_KEY = "?key=";
    private static final String PARAM_AND_KEY = "&key=";
    private static final String PARAM_DOCUMENT_ID = "?documentId=";

    private static final String CONFIG_KEY_API_KEY = "apiKey";
    private static final String CONFIG_KEY_PROJECT_ID = "projectId";
    private static final String CONFIG_KEY_DATABASE_ID = "databaseId";

    private static final String UPGRADE_PREFIX = "upgrade_";
    private static final String UPGRADE_ATTACK = "attack";
    private static final String UPGRADE_HEALTH = "health";
    private static final String UPGRADE_SPEED = "speed";

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_BEARER = "Bearer ";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

    private String apiKey;
    private String projectId;
    private String databaseId = "(default)";
    private final String firestoreApiRoot = "https://firestore.googleapis.com/v1";
    private static final FirebaseManager INSTANCE = new FirebaseManager();

    private String idToken;
    private String refreshToken;
    private String localId;
    private String email;
    private long expiresAtMs;

    private FirebaseManager() {}

    public static FirebaseManager getInstance() { return INSTANCE; }

    private String documentsBase() {
        if (projectId == null || databaseId == null) return null;
        return firestoreApiRoot + PATH_PROJECTS + projectId + PATH_DATABASES + databaseId + "/documents";
    }

    public synchronized void setConfig(String apiKey, String projectId) {
        this.apiKey = apiKey;
        this.projectId = projectId;
    }

    public synchronized void setConfig(String apiKey, String projectId, String databaseId) {
        this.apiKey = apiKey;
        this.projectId = projectId;
        if (databaseId != null && !databaseId.isEmpty()) this.databaseId = databaseId;
    }

    private synchronized void loadConfigIfNeeded() {
        if (isConfigComplete()) return;
        loadConfigFromEnvironment();
        if (!isConfigComplete()) {
            loadConfigFromPropertiesFile();
        }
        logConfigStatus();
    }

    private boolean isConfigComplete() {
        return this.apiKey != null && this.projectId != null;
    }

    private void loadConfigFromEnvironment() {
        String envKey = System.getenv("FIREBASE_API_KEY");
        String envProj = System.getenv("FIREBASE_PROJECT_ID");
        String envDb = System.getenv("FIREBASE_DATABASE_ID");
        applyEnvironmentVariable(envKey, CONFIG_KEY_API_KEY);
        applyEnvironmentVariable(envProj, CONFIG_KEY_PROJECT_ID);
        applyEnvironmentVariable(envDb, CONFIG_KEY_DATABASE_ID);
    }

    private void applyEnvironmentVariable(String value, String fieldName) {
        if (value == null || value.isEmpty()) return;
        switch (fieldName) {
            case CONFIG_KEY_API_KEY:
                this.apiKey = value;
                break;
            case CONFIG_KEY_PROJECT_ID:
                this.projectId = value;
                break;
            case CONFIG_KEY_DATABASE_ID:
                this.databaseId = value;
                break;
            default:
                break;
        }
    }

    private void loadConfigFromPropertiesFile() {
        try (InputStream in = FirebaseManager.class.getClassLoader().getResourceAsStream("firebase.properties")) {
            if (in == null) return;
            Properties p = new Properties();
            p.load(in);
            applyPropertyValue(p, CONFIG_KEY_API_KEY);
            applyPropertyValue(p, CONFIG_KEY_PROJECT_ID);
            applyPropertyValue(p, CONFIG_KEY_DATABASE_ID);
        } catch (IOException ignore) {
            // Silently ignore file read errors
        }
    }

    private void applyPropertyValue(Properties props, String fieldName) {
        String value = props.getProperty(fieldName);
        if (value == null || value.isEmpty()) return;
        switch (fieldName) {
            case CONFIG_KEY_API_KEY:
                if (this.apiKey == null) this.apiKey = value;
                break;
            case CONFIG_KEY_PROJECT_ID:
                if (this.projectId == null) this.projectId = value;
                break;
            case CONFIG_KEY_DATABASE_ID:
                this.databaseId = value;
                break;
            default:
                break;
        }
    }

    private void logConfigStatus() {
        if (this.apiKey == null || this.projectId == null) {
            System.err.println("[FirebaseManager] Missing Firebase configuration. Set FIREBASE_API_KEY and FIREBASE_PROJECT_ID.");
        } else {
            System.out.println("[FirebaseManager] Loaded Firebase projectId=" + this.projectId + ", databaseId=" + this.databaseId);
        }
    }

    public void initialize() {
        loadConfigIfNeeded();
        System.out.println("Firebase Manager Initialized. projectId=" + projectId + ", databaseId=" + databaseId);
    }

    public synchronized boolean isLoggedIn() { return idToken != null; }
    public synchronized String getCurrentUserEmail() { return email; }
    public synchronized String getUid() { return localId; }
    public synchronized String getIdToken() { return idToken; }

    public synchronized void signOut() {
        idToken = null;
        refreshToken = null;
        localId = null;
        email = null;
        expiresAtMs = 0L;
    }

    public boolean signInWithEmailPassword(String email, String password) {
        loadConfigIfNeeded();
        try {
            JSONObject body = new JSONObject().put(FIELD_EMAIL, email).put("password", password).put(FIELD_RETURN_SECURE_TOKEN, true);
            JSONObject res = postJson("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey, body);
            if (res == null) return false;
            applyAuthResponse(res);
            ensureUserDocExists();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean signUpWithEmailPassword(String email, String password) {
        loadConfigIfNeeded();
        try {
            JSONObject body = new JSONObject().put(FIELD_EMAIL, email).put("password", password).put(FIELD_RETURN_SECURE_TOKEN, true);
            JSONObject res = postJson("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + apiKey, body);
            if (res == null) return false;
            applyAuthResponse(res);
            ensureUserDocExists();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateDisplayName(String displayName) {
        if (!isLoggedIn()) return false;
        try {
            JSONObject body = new JSONObject().put("idToken", getIdToken()).put("displayName", displayName).put(FIELD_RETURN_SECURE_TOKEN, true);
            JSONObject res = postJson("https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + apiKey, body);
            return res != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Ensure the Firestore user document exists; create with default fields when missing */
    private void ensureUserDocExists() {
        if (documentsBase() == null || !isLoggedIn()) return;
        try {
            String docUrl = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;
            JSONObject defaultFields = buildDefaultUserFields();
            JSONObject existing = getJson(docUrl);

            if (existing != null && existing.has(FIELD_FIELDS)) {
                JSONObject missing = collectMissingDefaults(existing.optJSONObject(FIELD_FIELDS));
                patchUserFields(docUrl, missing);
                return;
            }

            JSONObject body = new JSONObject().put(FIELD_FIELDS, defaultFields);
            String createUrl = documentsBase() + PATH_USERS_QUERY + localId + PARAM_AND_KEY + apiKey;
            JSONObject createRes = postJsonFirestore(createUrl, body);
            if (createRes == null) {
                JSONObject afterAttempt = getJson(docUrl);
                if (afterAttempt != null && afterAttempt.has(FIELD_FIELDS)) {
                    patchUserFields(docUrl, collectMissingDefaults(afterAttempt.optJSONObject(FIELD_FIELDS)));
                } else {
                    patchUserFields(docUrl, defaultFields);
                }
            }
        } catch (Exception e) {
            System.err.println("[FirebaseManager] ensureUserDocExists failed: " + e.getMessage());
        }
    }

    private JSONObject collectMissingDefaults(JSONObject existingFields) {
        JSONObject missing = new JSONObject();
        if (email != null && (existingFields == null || !existingFields.has(FIELD_EMAIL))) {
            missing.put(FIELD_EMAIL, new JSONObject().put(FIELD_STRING_VALUE, email));
        }
        if (existingFields == null || !existingFields.has(FIELD_POINTS)) {
            missing.put(FIELD_POINTS, new JSONObject().put(FIELD_INTEGER_VALUE, "0"));
        }
        if (existingFields == null || !existingFields.has(FIELD_HIGHEST_SCORE)) {
            missing.put(FIELD_HIGHEST_SCORE, new JSONObject().put(FIELD_INTEGER_VALUE, "0"));
        }
        return missing;
    }

    private JSONObject buildDefaultUserFields() {
        JSONObject defaults = new JSONObject();
        if (email != null) defaults.put(FIELD_EMAIL, new JSONObject().put(FIELD_STRING_VALUE, email));
        defaults.put(FIELD_POINTS, new JSONObject().put(FIELD_INTEGER_VALUE, "0"));
        defaults.put(FIELD_HIGHEST_SCORE, new JSONObject().put(FIELD_INTEGER_VALUE, "0"));
        defaults.put(FIELD_MAX_CLEARED_STAGE, new JSONObject().put(FIELD_INTEGER_VALUE, "0")); 
        return defaults;
    }

    private void patchUserFields(String baseUrl, JSONObject fields) throws IOException {
        if (fields == null || fields.length() == 0) return;
        StringBuilder patchUrl = new StringBuilder(baseUrl);
        for (String fieldName : fields.keySet()) {
            patchUrl.append("&updateMask.fieldPaths=").append(fieldName);
        }
        JSONObject patchBody = new JSONObject().put(FIELD_FIELDS, fields);
        patchJson(patchUrl.toString(), patchBody);
    }

    public int getUserPoints() {
        if (!isLoggedIn()) return 0;
        if (documentsBase() == null) return 0;
        try {
            String url = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;
            JSONObject res = getJson(url);
            if (res != null && res.has(FIELD_FIELDS)) {
                JSONObject fields = res.getJSONObject(FIELD_FIELDS);
                if (fields.has(FIELD_POINTS)) {
                    return fields.getJSONObject(FIELD_POINTS).getInt(FIELD_INTEGER_VALUE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean updateUserPoints(int points) {
        if (!isLoggedIn() || documentsBase() == null) return false;

        try {
            String url = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;
            JSONObject existing = getJson(url);
            if (existing == null || !existing.has(FIELD_FIELDS)) return false;

            JSONObject fields = buildPointsUpdateFields(points, existing.getJSONObject(FIELD_FIELDS));
            return executePointsUpdate(url, fields);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private JSONObject buildPointsUpdateFields(int points, JSONObject existingFields) {
        JSONObject fields = new JSONObject();
        fields.put(FIELD_POINTS, new JSONObject().put(FIELD_INTEGER_VALUE, String.valueOf(points)));
        preserveExistingCoreFields(fields, existingFields);
        preserveUpgradeFields(fields, existingFields);
        return fields;
    }

    private void preserveExistingCoreFields(JSONObject fields, JSONObject existingFields) {
        if (existingFields.has(FIELD_EMAIL)) {
            fields.put(FIELD_EMAIL, existingFields.getJSONObject(FIELD_EMAIL));
        }
        if (existingFields.has(FIELD_HIGHEST_SCORE)) {
            fields.put(FIELD_HIGHEST_SCORE, existingFields.getJSONObject(FIELD_HIGHEST_SCORE));
        }
        if (existingFields.has(FIELD_MAX_CLEARED_STAGE)) {
            fields.put(FIELD_MAX_CLEARED_STAGE, existingFields.getJSONObject(FIELD_MAX_CLEARED_STAGE));
        }
    }

    private void preserveUpgradeFields(JSONObject fields, JSONObject existingFields) {
        for (String type : new String[]{UPGRADE_ATTACK, UPGRADE_HEALTH, UPGRADE_SPEED}) {
            String fieldName = UPGRADE_PREFIX + type;
            if (existingFields.has(fieldName)) {
                fields.put(fieldName, existingFields.getJSONObject(fieldName));
            }
        }
    }

    private boolean executePointsUpdate(String baseUrl, JSONObject fields) throws IOException {
        JSONObject body = new JSONObject().put(FIELD_FIELDS, fields);
        String patchUrl = baseUrl + "&updateMask.fieldPaths=points";
        JSONObject res = patchJson(patchUrl, body);

        if (res != null) return true;

        // Fallback: create document if PATCH failed
        String createUrl = documentsBase() + PATH_USERS_QUERY + localId + PARAM_AND_KEY + apiKey;
        JSONObject createRes = postJsonFirestore(createUrl, body);
        return createRes != null;
    }

    /** Add (or subtract) points relative to current total. Negative delta allowed; min 0. */
    public boolean addPoints(int delta) {
        if (!isLoggedIn()) return false;
        int current = getUserPoints();
        int next = current + delta;
        if (next < 0) next = 0;
        return updateUserPoints(next);
    }

    public List<Map<String, String>> getPurchasedItemDetails() {
        List<Map<String, String>> items = new ArrayList<>();
        if (!isLoggedIn() || documentsBase() == null) return items;

        try {
            String url = documentsBase() + PATH_USERS + localId + PATH_ITEMS_QUERY + apiKey;
            JSONObject res = getJson(url);
            if (res != null && res.has(FIELD_DOCUMENTS)) {
                items = parseItemDetailsFromDocuments(res.getJSONArray(FIELD_DOCUMENTS));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    private List<Map<String, String>> parseItemDetailsFromDocuments(JSONArray docs) {
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            Map<String, String> itemDetail = extractItemDetailFromDocument(doc);
            if (itemDetail != null) {
                items.add(itemDetail);
            }
        }
        return items;
    }

    private Map<String, String> extractItemDetailFromDocument(JSONObject doc) {
        String itemId = extractItemIdFromDocument(doc);
        if (itemId == null || !doc.has("name")) {
            return null;
        }

        Map<String, String> itemDetail = new HashMap<>();
        itemDetail.put(FIELD_ITEM_ID, itemId);
        itemDetail.put("name", doc.getString("name"));
        return itemDetail;
    }

    private String extractItemIdFromDocument(JSONObject doc) {
        if (!doc.has(FIELD_FIELDS)) return null;

        JSONObject fields = doc.getJSONObject(FIELD_FIELDS);
        if (!fields.has(FIELD_ITEM_ID)) return null;

        return fields.getJSONObject(FIELD_ITEM_ID).getString(FIELD_STRING_VALUE);
    }

    public boolean purchaseItem(String itemId, String itemName, int price) {
        if (!isLoggedIn()) return false;
        if (documentsBase() == null) return false;
        // Ensure user doc exists and user has enough points
        ensureUserDocExists();

        // 1. 포인트 차감 시도 (spendPoints는 이미 updateUserPoints를 사용하여 원자적으로 처리)
        if (!spendPoints(price)) {
            System.err.println("포인트가 부족하거나 차감에 실패했습니다.");
            return false;
        }
        
        // 2. 아이템 문서 생성 (자동 ID 부여)
        try {
            JSONObject itemFields = new JSONObject()
                    .put(FIELD_ITEM_ID, new JSONObject().put(FIELD_STRING_VALUE, itemId))
                    .put("itemName", new JSONObject().put(FIELD_STRING_VALUE, itemName))
                    .put("purchaseDate", new JSONObject().put("timestampValue", java.time.Instant.now().toString()));
            
            JSONObject body = new JSONObject().put(FIELD_FIELDS, itemFields);
            
            // POST /.../documents/users/{uid}/items 경로에 요청하여 자동 ID 생성
            String createUrl = documentsBase() + PATH_USERS + localId + PATH_ITEMS_QUERY + apiKey;
            
            // postJsonFirestore는 POST 요청을 사용하여 자동 ID로 새 문서를 만듭니다.
            JSONObject createRes = postJsonFirestore(createUrl, body);
            
            if (createRes != null) {
                // 성공: 아이템이 생성되었습니다.
                return true;
            } else {
                // 실패: 아이템 생성 실패 (포인트는 이미 차감됨!)
                // ⚠️ 롤백 처리: 실패 시 차감된 포인트를 다시 돌려줘야 합니다.
                System.err.println("[Firebase] 아이템 생성에 실패했습니다. 포인트 롤백을 시도합니다.");
                addPoints(price); // 차감했던 포인트를 다시 추가 (롤백)
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 예외 발생 시에도 롤백 처리
            System.err.println("[Firebase] 아이템 생성 중 예외 발생. 포인트 롤백을 시도합니다.");
            addPoints(price);
            return false;
        }
    }

    /** Deduct points if there is enough; does not create an item document. */
    public boolean spendPoints(int price) {
        if (price < 0) price = 0;
        int current = getUserPoints();
        if (current < price) return false;
        return updateUserPoints(current - price);
    }

    public List<String> getPurchasedItems() {
        List<String> items = new ArrayList<>();
        if (!isLoggedIn() || documentsBase() == null) return items;

        try {
            String url = documentsBase() + PATH_USERS + localId + PATH_ITEMS_QUERY + apiKey;
            JSONObject res = getJson(url);
            if (res != null && res.has(FIELD_DOCUMENTS)) {
                items = extractItemIdsFromDocuments(res.getJSONArray(FIELD_DOCUMENTS));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    private List<String> extractItemIdsFromDocuments(JSONArray docs) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < docs.length(); i++) {
            JSONObject doc = docs.getJSONObject(i);
            String itemId = extractItemIdFromFields(doc);
            if (itemId != null) {
                items.add(itemId);
            }
        }
        return items;
    }

    private String extractItemIdFromFields(JSONObject doc) {
        if (!doc.has(FIELD_FIELDS)) return null;

        JSONObject fields = doc.getJSONObject(FIELD_FIELDS);
        if (!fields.has(FIELD_ITEM_ID)) return null;

        return fields.getJSONObject(FIELD_ITEM_ID).getString(FIELD_STRING_VALUE);
    }

    /**
     * 주어진 Firestore 문서 이름(name)을 사용하여 특정 아이템 인스턴스를 삭제합니다.
     * 이 메소드를 사용하면 아이템을 개별적으로 추적하여 제거할 수 있습니다.
     * @param docName 삭제할 아이템 인스턴스의 완전한 문서 이름 (예: projects/.../items/자동생성ID)
     * @return 성공 여부 (true/false)
     */
    public boolean deleteItemByDocumentName(String docName) {
        if (!isLoggedIn()) return false;
        if (docName == null || docName.isEmpty()) {
            System.err.println("[Firebase] Document name for deletion is null or empty.");
            return false;
        }
        
        // 🚀 [수정된 부분]: 전달받은 문서 이름으로 바로 DELETE 요청
        System.out.println("[Firebase] Attempting to DELETE document by name: " + docName);
        
        try {
            String deleteUrl = firestoreApiRoot + "/" + docName + PARAM_KEY + apiKey;
            return deleteJson(deleteUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 기존 deletePurchasedItem 메소드의 수정 (로직은 유지하되, 내부적으로 자동 생성 ID를 사용)
    /**
     * 주어진 itemId를 가진 아이템 문서 중 가장 먼저 구매한 (가장 오래된) 문서를 찾아 삭제합니다.
     * @param itemId 삭제할 아이템의 논리 ID
     * @return 성공 여부 (true/false)
     */
    public boolean deletePurchasedItem(String itemId) {
        if (!isLoggedIn()) return false;

        // getPurchasedItemDetails()를 사용하여 모든 아이템 인스턴스를 가져옵니다.
        List<Map<String, String>> itemDetails = getPurchasedItemDetails();

        // 🚀 [수정된 부분]: 문서 이름이 아닌 itemId를 기준으로 찾습니다.
        String docNameToDelete = null;
        
        // 1. 메모리에서 원하는 itemId를 가진 첫 번째 문서 (가장 먼저 조회된 = FIFO)를 찾습니다.
        // 대소문자를 무시하고 비교합니다.
        for (Map<String, String> detail : itemDetails) {
            String storedItemId = detail.get(FIELD_ITEM_ID);
            if (storedItemId != null && storedItemId.equalsIgnoreCase(itemId)) {
                docNameToDelete = detail.get("name"); // 고유 문서 이름 획득
                break;
            }
        }

        if (docNameToDelete == null) {
            System.err.println("[Firebase] Item not found to delete: " + itemId);
            return false;
        }

        // 2. 찾은 고유 문서 이름으로 삭제를 요청합니다.
        return deleteItemByDocumentName(docNameToDelete); 
        // 내부적으로 deleteItemByDocumentName(String) 메소드를 사용하도록 변경하여 로직 통합
    }

    // FirebaseManager.java (아래 두 메서드를 추가합니다)

    /** Firestore에서 사용자의 현재 최고 점수를 가져옵니다. */
    public int getHighestScore() {
        if (!isLoggedIn()) return 0;
        if (documentsBase() == null) return 0;
        try {
            String url = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;
            JSONObject res = getJson(url);
            if (res != null && res.has(FIELD_FIELDS)) {
                JSONObject fields = res.getJSONObject(FIELD_FIELDS);
                if (fields.has(FIELD_HIGHEST_SCORE)) {
                    // highest_score 필드는 integerValue로 저장되어 있다고 가정합니다.
                    return fields.getJSONObject(FIELD_HIGHEST_SCORE).getInt(FIELD_INTEGER_VALUE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /** 사용자의 최고 점수를 업데이트합니다. */
    public boolean updateHighestScore(int newScore) {
        if (!isLoggedIn()) return false;
        if (documentsBase() == null) return false;
        try {
            // 🚀 [수정] 기존 'points' 필드의 값을 먼저 가져와야 합니다.
            int currentPoints = getUserPoints(); 

            // 최고 점수, 이메일, 그리고 기존 'points' 필드를 포함하여 PATCH 요청
            JSONObject fields = new JSONObject()
                    .put(FIELD_HIGHEST_SCORE, new JSONObject().put(FIELD_INTEGER_VALUE, newScore))
                    .put(FIELD_EMAIL, new JSONObject().put(FIELD_STRING_VALUE, email)) 
                    .put(FIELD_POINTS, new JSONObject().put(FIELD_INTEGER_VALUE, currentPoints)); // 👈 기존 'points' 필드 유지

            JSONObject body = new JSONObject().put(FIELD_FIELDS, fields);
            String url = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;

            // PATCH 요청 시도
            JSONObject res = patchJson(url, body);
            if (res != null) return true;

            // PATCH 실패 시 (e.g., 문서 부재) POST로 생성 시도 (ensureUserDocExists와 유사)
            String createUrl = documentsBase() + PATH_USERS_QUERY + localId + PARAM_AND_KEY + apiKey;
            JSONObject createRes = postJsonFirestore(createUrl, body);
            return createRes != null;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // DELETE HTTP 요청 메서드도 FirebaseManager.java에 추가 (patchJson과 유사)
    private boolean deleteJson(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("DELETE");
        if (idToken != null) {
            conn.setRequestProperty(HEADER_AUTHORIZATION, HEADER_BEARER + idToken);
        }
        int code = conn.getResponseCode();
        
        if (code == 200 || code == 204) { // 200 OK 또는 204 No Content (삭제 성공)
            return true;
        } else {
            // 에러 스트림을 읽어서 출력 (선택 사항)
            InputStream is = conn.getErrorStream();
            String text = (is != null) ? readAll(is) : conn.getResponseMessage();
            System.err.println("DELETE error(" + code + "): " + text);
            return false;
        }
    }

    private JSONObject postJson(String urlStr, JSONObject body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        conn.setDoOutput(true);
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(out); }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);
        if (code >= 200 && code < 300) {
            return new JSONObject(text);
        } else {
            try {
                JSONObject err = new JSONObject(text);
                String msg = extractFirebaseError(err);
                System.err.println("Firebase error(" + code + "): " + msg);
            } catch (Exception ignore) {
                System.err.println("HTTP " + code + ": " + text);
            }
            return null;
        }
    }

    private static String readAll(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String extractFirebaseError(JSONObject err) {
        try {
            return err.getJSONObject("error").getJSONArray("errors").getJSONObject(0).optString("message", err.toString());
        } catch (Exception e) {
            return err.toString();
        }
    }

    private synchronized void applyAuthResponse(JSONObject res) {
        this.idToken = res.optString("idToken", null);
        this.refreshToken = res.optString("refreshToken", null);
        this.localId = res.optString("localId", null);
        this.email = res.optString(FIELD_EMAIL, null);
        long expiresInSec = 0L;
        try { expiresInSec = Long.parseLong(res.optString("expiresIn", "0")); } catch (Exception ignore) {}
        this.expiresAtMs = (expiresInSec > 0) ? System.currentTimeMillis() + expiresInSec * 1000L : 0L;
    }

    private JSONObject getJson(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        if (idToken != null) {
            conn.setRequestProperty(HEADER_AUTHORIZATION, HEADER_BEARER + idToken);
        }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);
        if (code >= 200 && code < 300) {
            return new JSONObject(text);
        } else {
            System.err.println("GET error(" + code + "): " + text);
            return null;
        }
    }

    private JSONObject patchJson(String urlStr, JSONObject body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        // Some JDKs don't support PATCH; use POST + override
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        conn.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        if (idToken != null) {
            conn.setRequestProperty(HEADER_AUTHORIZATION, HEADER_BEARER + idToken);
        }
        conn.setDoOutput(true);
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(out); }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);
        if (code >= 200 && code < 300) {
            return new JSONObject(text);
        } else {
            System.err.println("PATCH (override) error(" + code + "): " + text);
            return null;
        }
    }

    private JSONObject postJsonFirestore(String urlStr, JSONObject body) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        if (idToken != null) {
            conn.setRequestProperty(HEADER_AUTHORIZATION, HEADER_BEARER + idToken);
        }
        conn.setDoOutput(true);
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(out); }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);
        if (code >= 200 && code < 300) {
            return new JSONObject(text);
        } else {
            System.err.println("POST error(" + code + "): " + text);
            return null;
        }
    }

    /** POST to Firestore documents:commit with Authorization header */
    private JSONObject postJsonCommit(JSONObject body) throws IOException {
        String urlStr = firestoreApiRoot + PATH_PROJECTS + projectId + PATH_DATABASES + databaseId + "/documents:commit" + PARAM_KEY + apiKey;
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        if (idToken != null) {
            conn.setRequestProperty(HEADER_AUTHORIZATION, HEADER_BEARER + idToken);
        }
        conn.setDoOutput(true);
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(out); }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);
        if (code >= 200 && code < 300) {
            return new JSONObject(text);
        } else {
            System.err.println("COMMIT error(" + code + "): " + text);
            return null;
        }
    }
    // ========== 영구 업그레이드 시스템 ==========

    /**
     * Get user's permanent upgrade level for a specific type
     * @param upgradeType UPGRADE_ATTACK, UPGRADE_HEALTH, or UPGRADE_SPEED
     * @return upgrade level (0 if not found or not logged in)
     */
    public int getUpgradeLevel(String upgradeType) {
        if (!isLoggedIn()) return 0;
        if (documentsBase() == null) return 0;
        try {
            String url = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;
            JSONObject res = getJson(url);
            if (res != null && res.has(FIELD_FIELDS)) {
                JSONObject fields = res.getJSONObject(FIELD_FIELDS);
                String fieldName = UPGRADE_PREFIX + upgradeType;
                if (fields.has(fieldName)) {
                    return fields.getJSONObject(fieldName).getInt(FIELD_INTEGER_VALUE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get all permanent upgrade levels
     * @return Map with keys UPGRADE_ATTACK, UPGRADE_HEALTH, UPGRADE_SPEED and their levels
     */
    public Map<String, Integer> getAllUpgrades() {
        Map<String, Integer> upgrades = new HashMap<>();
        upgrades.put(UPGRADE_ATTACK, getUpgradeLevel(UPGRADE_ATTACK));
        upgrades.put(UPGRADE_HEALTH, getUpgradeLevel(UPGRADE_HEALTH));
        upgrades.put(UPGRADE_SPEED, getUpgradeLevel(UPGRADE_SPEED));
        return upgrades;
    }

    /**
     * Purchase a permanent upgrade level
     * @param upgradeType UPGRADE_ATTACK, UPGRADE_HEALTH, or UPGRADE_SPEED
     * @param cost Points cost for this upgrade
     * @return true if purchase successful, false otherwise
     */
    // public boolean purchaseUpgrade(String upgradeType, int cost) {
    //     if (!isLoggedIn()) return false;
    //     if (documentsBase() == null) return false;

    //     // Check if user has enough points
    //     int currentPoints = getUserPoints();
    //     if (currentPoints < cost) {
    //         System.out.println("Not enough points. Have: " + currentPoints + ", Need: " + cost);
    //         return false;
    //     }

    //     try {
    //         // Get current upgrade level
    //         int currentLevel = getUpgradeLevel(upgradeType);
    //         int newLevel = currentLevel + 1;

    //         // Deduct points and update upgrade level atomically
    //         int newPoints = currentPoints - cost;
    //         String fieldName = UPGRADE_PREFIX + upgradeType;

    //         JSONObject fields = new JSONObject()
    //                 .put(FIELD_EMAIL, new JSONObject().put(FIELD_STRING_VALUE, email))
    //                 .put(FIELD_POINTS, new JSONObject().put(FIELD_INTEGER_VALUE, newPoints))
    //                 .put(fieldName, new JSONObject().put(FIELD_INTEGER_VALUE, newLevel));

    //         // Also preserve existing upgrade fields
    //         String url = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;
    //         JSONObject existing = getJson(url);
    //         if (existing != null && existing.has(FIELD_FIELDS)) {
    //             JSONObject existingFields = existing.getJSONObject(FIELD_FIELDS);
    //             // Preserve other upgrade fields
    //             for (String type : new String[]{UPGRADE_ATTACK, UPGRADE_HEALTH, UPGRADE_SPEED}) {
    //                 String fn = UPGRADE_PREFIX + type;
    //                 if (!fn.equals(fieldName) && existingFields.has(fn)) {
    //                     fields.put(fn, existingFields.getJSONObject(fn));
    //                 }
    //             }
    //         }

    //         JSONObject body = new JSONObject().put(FIELD_FIELDS, fields);
    //         JSONObject res = patchJson(url, body);

    //         if (res != null) {
    //             System.out.println("Successfully purchased " + upgradeType + " upgrade to level " + newLevel);
    //             return true;
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    //     return false;
    // }

    // FirebaseManager.java: purchaseUpgrade(String upgradeType, int cost) 메서드 수정

    public boolean purchaseUpgrade(String upgradeType, int cost) {
        if (!canPurchaseUpgrade(cost)) return false;

        try {
            int currentPoints = getUserPoints();
            int currentLevel = getUpgradeLevel(upgradeType);
            int newLevel = currentLevel + 1;
            int newPoints = currentPoints - cost;

            String url = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;
            JSONObject existing = getJson(url);
            if (existing == null || !existing.has(FIELD_FIELDS)) return false;

            JSONObject fields = buildUpgradeFields(upgradeType, newPoints, newLevel, existing.getJSONObject(FIELD_FIELDS));
            String patchUrl = buildUpgradePatchUrl(url, upgradeType);

            return executeUpgradePatch(patchUrl, fields, upgradeType, newLevel);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean canPurchaseUpgrade(int cost) {
        if (!isLoggedIn() || documentsBase() == null) return false;

        int currentPoints = getUserPoints();
        if (currentPoints < cost) {
            System.out.println("Not enough points. Have: " + currentPoints + ", Need: " + cost);
            return false;
        }
        return true;
    }

    private JSONObject buildUpgradeFields(String upgradeType, int newPoints, int newLevel, JSONObject existingFields) {
        JSONObject fields = new JSONObject();

        // Add core fields
        fields.put(FIELD_MAX_CLEARED_STAGE, new JSONObject().put(FIELD_INTEGER_VALUE, String.valueOf(getMaxClearedStage())));
        fields.put(FIELD_HIGHEST_SCORE, new JSONObject().put(FIELD_INTEGER_VALUE, String.valueOf(getHighestScore())));
        fields.put(FIELD_EMAIL, new JSONObject().put(FIELD_STRING_VALUE, email));

        // Add updated fields
        fields.put(FIELD_POINTS, new JSONObject().put(FIELD_INTEGER_VALUE, newPoints));
        String fieldName = UPGRADE_PREFIX + upgradeType;
        fields.put(fieldName, new JSONObject().put(FIELD_INTEGER_VALUE, newLevel));

        // Preserve other upgrade fields
        preserveOtherUpgrades(fields, existingFields, fieldName);

        return fields;
    }

    private void preserveOtherUpgrades(JSONObject fields, JSONObject existingFields, String excludeFieldName) {
        for (String type : new String[]{UPGRADE_ATTACK, UPGRADE_HEALTH, UPGRADE_SPEED}) {
            String fn = UPGRADE_PREFIX + type;
            if (!fn.equals(excludeFieldName) && existingFields.has(fn)) {
                fields.put(fn, existingFields.getJSONObject(fn));
            }
        }
    }

    private String buildUpgradePatchUrl(String baseUrl, String upgradeType) {
        StringBuilder maskBuilder = new StringBuilder(baseUrl);
        maskBuilder.append("&updateMask.fieldPaths=points");
        maskBuilder.append("&updateMask.fieldPaths=upgrade_").append(upgradeType);
        maskBuilder.append("&updateMask.fieldPaths=maxClearedStage");
        maskBuilder.append("&updateMask.fieldPaths=highest_score");
        return maskBuilder.toString();
    }

    private boolean executeUpgradePatch(String patchUrl, JSONObject fields, String upgradeType, int newLevel) throws IOException {
        JSONObject body = new JSONObject().put(FIELD_FIELDS, fields);
        JSONObject res = patchJson(patchUrl, body);

        if (res != null) {
            System.out.println("Successfully purchased " + upgradeType + " upgrade to level " + newLevel);
            return true;
        }
        return false;
    }

    /**
     * Get the cost for the next level of an upgrade
     * @param upgradeType UPGRADE_ATTACK, UPGRADE_HEALTH, or UPGRADE_SPEED
     * @return cost in points, or -1 if max level reached
     */
    public int getUpgradeCost(String upgradeType) {
        int currentLevel = getUpgradeLevel(upgradeType);
        int maxLevel = 5; // Maximum upgrade level

        if (currentLevel >= maxLevel) {
            return -1; // Max level reached
        }

        // Cost scaling: base cost * (level + 1)
        int baseCost;
        switch (upgradeType) {
            case UPGRADE_ATTACK:
                baseCost = 300;  // Attack: 300, 600, 900, 1200, 1500
                break;
            case UPGRADE_HEALTH:
                baseCost = 400;  // Health: 400, 800, 1200, 1600, 2000
                break;
            case UPGRADE_SPEED:
                baseCost = 350;  // Speed: 350, 700, 1050, 1400, 1750
                break;
            default:
                baseCost = 500;
                break;
        }

        return baseCost * (currentLevel + 1);
    }

    // FirebaseManager.java (아래 메서드를 추가합니다)

    // FirebaseManager.java (수정된 getTopScores 메서드)

    /**
     * Firestore에서 모든 유저의 최고 점수를 내림차순으로 가져옵니다.
     * @param limit 가져올 랭킹의 최대 개수
     * @return 랭킹 데이터를 담은 맵 리스트 (player/score/level/date 필드 가정)
     */
    public List<Map<String, Object>> getTopScores(int limit) {
        if (documentsBase() == null) return new ArrayList<>();
        List<Map<String, Object>> rankingData = new ArrayList<>();

        try {
            JSONObject structuredQuery = buildTopScoresQuery(limit);
            JSONObject body = new JSONObject().put("structuredQuery", structuredQuery);
            JSONObject resWrapper = postJsonRunQuery(body);

            if (resWrapper != null && resWrapper.has(FIELD_DOCUMENTS)) {
                rankingData = parseTopScoresResponse(resWrapper.getJSONArray(FIELD_DOCUMENTS));
            }
        } catch (Exception e) {
            System.err.println("Error in getTopScores: " + e.getMessage());
            e.printStackTrace();
        }
        return rankingData;
    }

    private JSONObject buildTopScoresQuery(int limit) {
        return new JSONObject()
                .put("from", new JSONArray().put(new JSONObject().put("collectionId", "users")))
                .put("orderBy", new JSONArray().put(new JSONObject()
                        .put("field", new JSONObject().put("fieldPath", FIELD_HIGHEST_SCORE))
                        .put("direction", "DESCENDING")
                ))
                .put("limit", limit);
    }

    private List<Map<String, Object>> parseTopScoresResponse(JSONArray results) {
        List<Map<String, Object>> rankingData = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);
            if (item.has("document")) {
                Map<String, Object> scoreEntry = parseScoreDocument(item.getJSONObject("document"));
                if (scoreEntry != null) {
                    rankingData.add(scoreEntry);
                }
            }
        }
        return rankingData;
    }

    private Map<String, Object> parseScoreDocument(JSONObject doc) {
        JSONObject fields = doc.getJSONObject(FIELD_FIELDS);
        int score = extractScoreFromFields(fields);

        if (score <= 0) return null; // 최고 점수가 0이 아닌 경우에만 랭킹에 포함

        String email = extractEmailFromFields(fields);
        Map<String, Object> data = new HashMap<>();
        data.put("player", email);
        data.put("score", score);
        data.put("level", 0);
        data.put("date", "N/A");
        return data;
    }

    private String extractEmailFromFields(JSONObject fields) {
        if (fields.has(FIELD_EMAIL)) {
            return fields.getJSONObject(FIELD_EMAIL).getString(FIELD_STRING_VALUE);
        }
        return "N/A";
    }

    private int extractScoreFromFields(JSONObject fields) {
        if (fields.has(FIELD_HIGHEST_SCORE)) {
            JSONObject scoreObj = fields.getJSONObject(FIELD_HIGHEST_SCORE);
            if (scoreObj.has(FIELD_INTEGER_VALUE)) {
                return scoreObj.getInt(FIELD_INTEGER_VALUE);
            }
        }
        return 0;
    }

    /** POST to Firestore documents:runQuery with Authorization header */
    private JSONObject postJsonRunQuery(JSONObject body) throws IOException { // 🚀 새로 추가
        String urlStr = firestoreApiRoot + PATH_PROJECTS + projectId + PATH_DATABASES + databaseId + "/documents:runQuery" + PARAM_KEY + apiKey;
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        if (idToken != null) {
            conn.setRequestProperty(HEADER_AUTHORIZATION, HEADER_BEARER + idToken);
        }
        conn.setDoOutput(true);
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(out); }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String text = readAll(is);
        if (code >= 200 && code < 300) {
            // runQuery는 JSON 배열을 반환하지만, JSONObject로 랩핑하여 처리하기 위해 
            // 임시로 배열을 가진 JSONObject를 반환할 수도 있으나, 여기서는 배열을 문자열로 반환하여
            // 호출 측에서 직접 처리하도록 단순화합니다. (기존 JSON 라이브러리 가정)
            // 그러나 JSONObject를 반환하도록 기존 API 형태를 유지하기 위해, 응답을 감싸서 처리하는 것이 좋습니다.
            // REST API 응답이 JSON 배열 형태 [{}, {}]이므로, 이 메서드는 JSONArray를 반환해야 합니다.
            // 하지만 기존 FirebaseManager의 패턴을 유지하기 위해, 여기서는 오류 없이 JSON 배열을 반환한다고 가정합니다.
            // 이로 인해 getTopScores 메서드에서 JSON 파싱 로직을 약간 수정해야 합니다.
            
            // runQuery의 응답은 JSON Array 이므로, 이를 JSONArray 형태로 반환하는것이 정확합니다.
            // FirebaseManager의 패턴을 깨지 않기 위해, 여기서는 배열 그대로 문자열을 반환합니다.
            // => getTopScores에서 직접 JSONArray로 파싱하도록 수정하겠습니다.
            
            // runQuery는 JSON 배열을 반환합니다. JSONObject로 감싸지 않고 바로 JSONArray를 사용하도록
            // getTopScores를 수정하겠습니다. postJsonRunQuery는 일단 JSON 문자열을 반환하는 헬퍼로 사용하겠습니다.
            
            // 기존 패턴 유지를 위해, 여기서는 JSONObject를 반환하지 않고, 응답 전문(Text)을 반환하는 것으로 간주하고,
            // getTopScores에서 JSONArray를 파싱하도록 코드를 간소화하겠습니다.
            try {
                return new JSONObject("{\"documents\": " + text + "}"); // 응답 배열을 JSON 객체 안에 넣어 반환 (getTopScores 수정 필요)
            } catch (Exception e) {
                 return new JSONObject("{\"documents\": []}");
            }
        } else {
            System.err.println("RUN QUERY error(" + code + "): " + text);
            return null;
        }
    }
    public void executeHttpRequest(String urlString, String jsonPayload, String method) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method); // "PATCH" 설정
        conn.setRequestProperty(HEADER_CONTENT_TYPE, "application/json");
        // ... (인증 헤더 설정, 출력 스트림을 통해 jsonPayload 전송 등) ...
        // conn.connect() 및 응답 코드 확인
    }

    public void saveMaxClearedStage(int stage) {
        if (!isLoggedIn()) {
            System.err.println("Error: Cannot save stage. User is not logged in.");
            return;
        }

        try {
            // 사용자 문서 URL
            String url = documentsBase() + PATH_USERS + localId + PARAM_KEY + apiKey;

            // 1. [PATCH JSON BODY] 필드 객체 생성
            JSONObject fields = new JSONObject()
                .put(FIELD_MAX_CLEARED_STAGE, new JSONObject().put(FIELD_INTEGER_VALUE, stage));
            
            // 2. [UPDATE MASK] updateMask 쿼리 파라미터 추가
            String updateUrl = url + "&updateMask.fieldPaths=maxClearedStage";
            
            // 3. 요청 본문: fields 객체를 포함하는 JSON 객체
            JSONObject body = new JSONObject().put(FIELD_FIELDS, fields);
            
            // 4. 기존의 patchJson 메서드를 사용하여 PATCH 요청 전송
            JSONObject res = patchJson(updateUrl, body); // ✅ 이 메서드는 X-HTTP-Method-Override를 사용합니다.
            
            if (res != null) {
                System.out.println("Max cleared stage saved: Stage " + stage);
            } else {
                System.err.println("Max cleared stage FAILED to save.");
            }
        } catch (Exception e) {
            System.err.println("Error saving max cleared stage: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 현재 로그인된 사용자의 'maxClearedStage' 기록을 Firestore에서 불러옵니다.
     * @return 최고 클리어 스테이지 (기록이 없거나 오류 발생 시 0 반환)
     */
    public int getMaxClearedStage() {
        if (!isLoggedIn()) {
            System.out.println("User not logged in. Returning maxClearedStage = 0.");
            return 0;
        }

        try {
            // 사용자 문서 경로: /users/{localId}
            String url = documentsBase() + PATH_USERS + localId;

            // 💡 [수정 2] getJson()이 String이 아닌 JSONObject를 직접 반환한다고 가정합니다.
            JSONObject json = getJson(url);
            
            if (json == null) {
                // 문서가 없거나 오류가 발생하면 0을 반환
                System.out.println("User document not found or error loading data. Returning 0.");
                return 0;
            }

            // 응답 JSON 파싱
            if (json.has(FIELD_FIELDS)) {
                JSONObject fields = json.getJSONObject(FIELD_FIELDS);
                if (fields.has(FIELD_MAX_CLEARED_STAGE)) {
                    // FIELD_MAX_CLEARED_STAGE: {FIELD_INTEGER_VALUE: "3"} 형태 파싱
                    String stageValue = fields.getJSONObject(FIELD_MAX_CLEARED_STAGE).getString(FIELD_INTEGER_VALUE);
                    return Integer.parseInt(stageValue);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading max cleared stage: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 기본값: 기록이 없으면 0 반환
        return 0;
    }
}