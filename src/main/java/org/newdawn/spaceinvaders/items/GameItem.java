package org.newdawn.spaceinvaders.items;

/**
 * 모든 게임 아이템의 기본이 되는 추상 클래스.
 * 이름, 설명, 이미지 파일명 등의 기본 정보와 함께 아이템의 판매 가격을 정의합니다.
 */
public abstract class GameItem {

    // 아이템의 고유 ID (선택 사항이지만 관리 용이)
    protected final String id;
    // 아이템 이름 (상점 표시용)
    protected final String name;
    // 아이템 설명 (클릭 시 표시할 메시지)
    protected final String description;
    // 아이템 이미지 파일명
    protected final String imageFileName;
    // 💡 아이템 가격 (새로 추가됨)
    protected final int price; 

    /**
     * GameItem의 생성자
     * @param id 아이템 고유 ID
     * @param name 아이템 이름
     * @param description 아이템 설명
     * @param imageFileName 이미지 파일명
     * @param price 아이템 판매 가격 (점수 또는 재화)
     */
    public GameItem(String id, String name, String description, String imageFileName, int price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageFileName = imageFileName;
        this.price = price; // 가격 초기화
    }

    // --- Getter 메서드 ---
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageFileName() {
        return imageFileName;
    }
    
    // 💡 가격 Getter 메서드 추가
    public int getPrice() {
        return price;
    }

    /**
     * 추후에 아이템의 실제 효과를 구현하기 위한 추상 메서드.
     * @param game 게임 객체 또는 플레이어 객체
     **/
}