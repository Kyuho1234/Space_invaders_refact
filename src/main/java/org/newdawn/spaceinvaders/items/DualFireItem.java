package org.newdawn.spaceinvaders.items;

/**
 * "탄환 추가" 아이템을 나타내는 클래스입니다.
 * 플레이어의 탄환 수를 증가시키는 효과를 가집니다.
 * GameItem 추상 클래스를 상속받아 아이템의 고유 정보를 정의합니다.
 */
public class DualFireItem extends GameItem {

    // 💡 아이템의 고유 정보들을 상수로 정의합니다.
    private static final String ITEM_ID = "DUAL_FIRE";
    private static final String ITEM_NAME = "Dual Fire Drone (10s)";
    // 💡 클릭 시 사용자에게 보여줄 아이템 상세 설명
    private static final String ITEM_DESC = "Grants a temporary sidekick drone for 10 seconds that fires parallel shots to your main cannon, significantly boosting firepower!";
    // 💡 사용할 이미지 파일명
    private static final String ITEM_IMAGE = "item_ammo_boost.png"; 
    // 💡 상점에서 판매할 아이템 가격 (800 pts로 설정)
    private static final int ITEM_PRICE = 5000; 

    /**
     * AmmoBoostItem의 생성자입니다.
     * 부모 클래스(GameItem)의 생성자를 호출하여 아이템의 기본 속성과 가격을 초기화합니다.
     */
    public DualFireItem() {
        // 부모 생성자 호출: super(id, name, description, imageFileName, price)
        super(ITEM_ID, ITEM_NAME, ITEM_DESC, ITEM_IMAGE, ITEM_PRICE);
    }
    
    /*
    @Override
    public void applyEffect(Game game) {
        // 실제 게임 객체에 탄환을 추가하는 로직을 여기에 구현
        // game.getPlayer().addAmmo(20); 
    }
    */
}