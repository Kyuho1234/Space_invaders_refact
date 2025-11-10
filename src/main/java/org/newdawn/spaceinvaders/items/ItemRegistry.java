package org.newdawn.spaceinvaders.items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 게임 내 모든 아이템을 관리하는 레지스트리 (저장소).
 * 상점 UI에서 아이템 정보에 쉽게 접근할 수 있게 해줍니다.
 */
public class ItemRegistry {
    
    // 아이템 ID를 키로, GameItem 객체를 값으로 저장할 맵
    private static final Map<String, GameItem> ALL_ITEMS;

    // 클래스가 로드될 때 모든 아이템을 미리 초기화합니다.
    static {
        Map<String, GameItem> items = new HashMap<>();
        
        // 1. "추가 목숨" 아이템 등록
        items.put("PLUS_LIFE", new PlusLifeItem()); 
        
        // 2. 다른 아이템을 추가할 때 여기에 넣습니다.
        // items.put("LASER_UPGRADE", new LaserUpgradeItem());
        items.put("DOUBLE_SCORE", new DoubleScoreItem());

        items.put("INVINCIBILITY", new InvincibilityItem());

        items.put("DUAL_FIRE", new DualFireItem());
        
        // 외부에서 수정할 수 없도록 불변(Immutable) 맵으로 설정
        ALL_ITEMS = Collections.unmodifiableMap(items);
    }

    /**
     * 등록된 모든 아이템 목록을 반환합니다.
     */
    public static Map<String, GameItem> getAllItems() {
        return ALL_ITEMS;
    }
    
    /**
     * ID를 사용하여 특정 아이템을 가져옵니다.
     */
    public static GameItem getItemById(String id) {
        return ALL_ITEMS.get(id);
    }
}