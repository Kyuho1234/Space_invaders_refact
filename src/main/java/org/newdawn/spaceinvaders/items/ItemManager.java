package org.newdawn.spaceinvaders.items;

import org.newdawn.spaceinvaders.firebase.FirebaseManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Item usage/stack management & buff runtime logic.
 *
 * Game.java는 이 매니저를 통해 아이템 수량을 갱신/사용하고,
 * 현재 활성화된 버프 상태(무적/더블스코어/연사강화)를 조회할 수 있습니다.
 */
public class ItemManager {

    /** Game이 효과를 처리할 때 참고할 수 있는 결과 타입 */
    public enum Effect { NONE, AMMO_BOOST, DOUBLE_SCORE, INVINCIBILITY, PLUS_LIFE }

    // 표준 슬롯 순서 (좌측 패널과 동일): 0=AMMO, 1=DOUBLE_SCORE, 2=INVINCIBILITY, 3=PLUS_LIFE
    public static final String ID_AMMO = "ammo";                 // item_ammo_boost.png
    public static final String ID_DOUBLE_SCORE = "double_score";  // item_double_score.png
    public static final String ID_INVINCIBILITY = "invincibility";// item_invincibility.png
    public static final String ID_PLUS_LIFE = "plus_life";        // item_plusLife.png

    private final FirebaseManager firebase; // 선택 사용 (소비 동기화 등 향후 확장용)

    // 보유 수량: key는 논리 id (위 상수 4종) 또는 Registry의 id를 소문자로 매핑
    private final Map<String, Integer> counts = new HashMap<>();

    // 버프 지속시간(ms)
    private long ammoBoostUntil = 0L;
    private long doubleScoreUntil = 0L;
    private long invincibleUntil = 0L;

    // 버프 파라미터
    private static final long DURATION_AMMO_MS = 10_000;       // 10s
    private static final long DURATION_DOUBLE_SCORE_MS = 10_000; // 10s
    private static final long DURATION_INVINCIBLE_MS = 5_000;    // 5s

    // 런타임 파라미터 (Game이 참조)
    private static final double FIRE_RATE_MULTIPLIER = 0.6;   // 연사간격 x0.6 (빨라짐)
    private static final double SCORE_MULTIPLIER = 2.0;       // 점수 2배

    public ItemManager(FirebaseManager firebase) {
        this.firebase = firebase;
        // 기본 0으로 초기화
        counts.put(ID_AMMO, 0);
        counts.put(ID_DOUBLE_SCORE, 0);
        counts.put(ID_INVINCIBILITY, 0);
        counts.put(ID_PLUS_LIFE, 0);
    }

    /** purchased itemId 리스트(예: Firestore에서 가져온 값)를 내부 카운트로 환산 */
    public void setCountsFromPurchased(List<String> purchasedItemIds) {
        // reset
        counts.replaceAll((k, v) -> 0);
        if (purchasedItemIds == null) return;
        for (String raw : purchasedItemIds) {
            String key = normalizeId(raw);
            if (key == null) continue;
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
    }

    /** 특정 아이템을 +1 (상점 구매 직후 UI 반영 등에 사용) */
    public void addOne(String itemId) {
        String k = normalizeId(itemId);
        if (k == null) return;
        counts.put(k, counts.getOrDefault(k, 0) + 1);
    }

    /** 보유 수량 조회 (논리 id) */
    public int getCount(String itemId) {
        String k = normalizeId(itemId);
        if (k == null) return 0;
        return counts.getOrDefault(k, 0);
    }

    /** 좌측 패널과 동일한 순서의 카운트 배열 반환 [ammo, doubleScore, invincibility, plusLife] */
    public int[] getCountsArray() {
        return new int[] {
                counts.getOrDefault(ID_AMMO, 0),
                counts.getOrDefault(ID_DOUBLE_SCORE, 0),
                counts.getOrDefault(ID_INVINCIBILITY, 0),
                counts.getOrDefault(ID_PLUS_LIFE, 0)
        };
    }

    public Effect use(String itemId) {
        String k = normalizeId(itemId);
        if (k == null) return Effect.NONE;
        int have = counts.getOrDefault(k, 0);
        if (have <= 0) return Effect.NONE;

        // 1. Firebase 연동이 필요하고 로그인되어 있다면, 먼저 DB에서 삭제를 시도합니다.
        boolean dbDeleteSuccess = true;
        if (firebase != null && firebase.isLoggedIn()) {
            // deletePurchasedItem이 실패하면 false를 반환합니다.
            dbDeleteSuccess = firebase.deletePurchasedItem(k); 
        }

        if (dbDeleteSuccess) {
            // 2. DB 삭제가 성공(또는 연동 필요 없음)한 경우에만 내부 카운트 감소
            counts.put(k, have - 1);
            
            // 3. 효과 적용
            long now = System.currentTimeMillis();
            if (ID_AMMO.equals(k)) {
                ammoBoostUntil = Math.max(ammoBoostUntil, now) + DURATION_AMMO_MS;
                return Effect.AMMO_BOOST;
            }
            if (ID_DOUBLE_SCORE.equals(k)) {
                doubleScoreUntil = Math.max(doubleScoreUntil, now) + DURATION_DOUBLE_SCORE_MS;
                return Effect.DOUBLE_SCORE;
            }
            if (ID_INVINCIBILITY.equals(k)) {
                invincibleUntil = Math.max(invincibleUntil, now) + DURATION_INVINCIBLE_MS;
                return Effect.INVINCIBILITY;
            }
            if (ID_PLUS_LIFE.equals(k)) {
                return Effect.PLUS_LIFE;
            }
        } else {
            // 4. DB 삭제 실패 시, 아이템은 소모되지 않은 것으로 처리
            System.err.println("[ItemManager] Item DB deletion failed. Usage cancelled.");
        }

        return Effect.NONE;
    }

    /**
     * 매 프레임 호출(선택). 현재 구현은 절대시간 비교로 충분하므로 비워도 됨.
     * 필요 시 tick 기반 로직으로 확장 가능.
     */
    public void update(long deltaMs) {
        // no-op (유지보수용 후크)
    }

    // ===== Game에서 조회할 런타임 상태 =====

    public boolean isInvincible() {
        return System.currentTimeMillis() < invincibleUntil;
    }

    public boolean isDoubleScoreActive() {
        return System.currentTimeMillis() < doubleScoreUntil;
    }

    public boolean isAmmoBoostActive() {
        return System.currentTimeMillis() < ammoBoostUntil;
    }

    /** 점수 배수 (기본 1.0, 더블스코어 중이면 2.0) */
    public double currentScoreMultiplier() {
        return isDoubleScoreActive() ? SCORE_MULTIPLIER : 1.0;
    }

    /** 발사 간격 배수 (기본 1.0, 연사강화 중이면 0.6) — Game의 쿨다운 계산에 곱해 사용 */
    public double currentFireRateMultiplier() {
        return isAmmoBoostActive() ? FIRE_RATE_MULTIPLIER : 1.0;
    }

    // ===== ID 정규화/매핑 =====

    /** 다양한 표기(itemId/파일명/표시명)를 논리 id로 정규화 */
    public static String normalizeId(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return null;

        // Check if already a valid ID
        if (isValidItemId(s)) return s;

        // Handle image filenames
        if (isImageFile(s)) return normalizeFromImageFilename(s);

        // Handle keywords
        return normalizeFromKeyword(s);
    }

    private static boolean isValidItemId(String s) {
        return ID_AMMO.equals(s) || ID_DOUBLE_SCORE.equals(s) ||
               ID_INVINCIBILITY.equals(s) || ID_PLUS_LIFE.equals(s);
    }

    private static boolean isImageFile(String s) {
        return s.endsWith(".png") || s.endsWith(".gif") ||
               s.endsWith(".jpg") || s.endsWith(".jpeg");
    }

    private static String normalizeFromImageFilename(String s) {
        if (s.contains("item_ammo_boost")) return ID_AMMO;
        if (s.contains("item_double_score")) return ID_DOUBLE_SCORE;
        if (s.contains("item_invincibility")) return ID_INVINCIBILITY;
        if (s.contains("item_pluslife")) return ID_PLUS_LIFE;
        return null;
    }

    private static String normalizeFromKeyword(String s) {
        if (s.contains("ammo")) return ID_AMMO;
        if (s.contains("double") || s.contains("score")) return ID_DOUBLE_SCORE;
        if (s.contains("invinc") || s.contains("shield")) return ID_INVINCIBILITY;
        if (s.contains("life")) return ID_PLUS_LIFE;
        return null;
    }

    // ===== 편의: Registry에서 초기 카탈로그를 뽑아오고 싶을 때 (선택) =====

    /** ItemRegistry에서 아이템 id들을 받아 기본 카운트를 초기화 (존재하는 키만 유지) */
    public void alignWithRegistryKeys() {
        try {
            Map<String, GameItem> all = ItemRegistry.getAllItems(); // 가정: Registry가 전체 목록을 제공
            List<String> keys = new ArrayList<>(all.keySet());
            Map<String, Integer> newCounts = new HashMap<>();
            for (String k : keys) {
                String norm = normalizeId(k);
                if (norm != null) {
                    newCounts.put(norm, counts.getOrDefault(norm, 0));
                }
            }
            // 필수 4종은 항상 유지
            for (String base : new String[]{ID_AMMO, ID_DOUBLE_SCORE, ID_INVINCIBILITY, ID_PLUS_LIFE}) {
                newCounts.putIfAbsent(base, counts.getOrDefault(base, 0));
            }
            counts.clear();
            counts.putAll(newCounts);
        } catch (Throwable ignore) {
            // Registry API 변화에 대비해 실패해도 무시
        }
    }
}
