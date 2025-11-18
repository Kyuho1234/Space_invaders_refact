package org.newdawn.spaceinvaders.items;

/**
 * "추가 목숨" 아이템을 나타내는 클래스입니다.
 * GameItem 추상 클래스를 상속받아 아이템의 고유 정보를 정의합니다.
 */
public class PlusLifeItem extends GameItem {

    // 💡 아이템의 고유 정보들을 상수로 정의합니다.
    private static final String ITEM_ID = "PLUS_LIFE";
    private static final String ITEM_NAME = "Extra Life";
    // 💡 클릭 시 사용자에게 보여줄 아이템 상세 설명
    private static final String ITEM_DESC = "Grants one additional life to the player. A must-have for tough levels!";
    // 💡 사용할 이미지 파일명
    private static final String ITEM_IMAGE = "item_plusLife.png";
    // 💡 상점에서 판매할 아이템 가격 (1000 pts로 설정)
    private static final int ITEM_PRICE = 1000; 

    /**
     * PlusLifeItem의 생성자입니다.
     * 부모 클래스(GameItem)의 생성자를 호출하여 아이템의 기본 속성과 가격을 초기화합니다.
     */
    public PlusLifeItem() {
        // 부모 생성자 호출: super(id, name, description, imageFileName, price)
        super(ITEM_ID, ITEM_NAME, ITEM_DESC, ITEM_IMAGE, ITEM_PRICE);
    }
    
    // 이 클래스에 다른 특별한 메서드가 필요하다면 추가할 수 있습니다.
    // 예를 들어, 나중에 게임 로직이 구현되면 아래 주석 부분을 추가할 수 있습니다.

}