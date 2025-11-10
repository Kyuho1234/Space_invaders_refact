package org.newdawn.spaceinvaders.items;

/**
 * "무적" 아이템을 나타내는 클래스입니다.
 * 5초 동안 플레이어에게 무적 상태를 부여하는 효과를 가집니다.
 * GameItem 추상 클래스를 상속받아 아이템의 고유 정보를 정의합니다.
 */
public class InvincibilityItem extends GameItem {

    // 💡 아이템의 고유 ID들을 상수로 정의합니다.
    private static final String ITEM_ID = "INVINCIBILITY";
    private static final String ITEM_NAME = "Invincibility (5s)";
    // 💡 클릭 시 사용자에게 보여줄 아이템 상세 설명
    private static final String ITEM_DESC = "Grants temporary invincibility for 5 seconds. Use it to survive tough situations!";
    // 💡 사용할 이미지 파일명
    private static final String ITEM_IMAGE = "item_invincibility.png"; 
    // 💡 상점에서 판매할 아이템 가격 (3000 pts로 설정)
    private static final int ITEM_PRICE = 3000; 

    /**
     * InvincibilityItem의 생성자입니다.
     * 부모 클래스(GameItem)의 생성자를 호출하여 아이템의 기본 속성과 가격을 초기화합니다.
     */
    public InvincibilityItem() {
        // 부모 생성자 호출: super(id, name, description, imageFileName, price)
        super(ITEM_ID, ITEM_NAME, ITEM_DESC, ITEM_IMAGE, ITEM_PRICE);
    }
    
    /*
    @Override
    public void applyEffect(Game game) {
        // 실제 게임 객체에 5초 동안 무적 상태를 설정하는 로직을 여기에 구현
        // game.getPlayer().setInvincible(5000); // 5000ms = 5초
    }
    */
}