package org.newdawn.spaceinvaders.items;

/**
 * "더블 스코어" 아이템을 나타내는 클래스입니다.
 * 10초 동안 획득 점수를 2배로 증가시키는 효과를 가집니다.
 * GameItem 추상 클래스를 상속받아 아이템의 고유 정보를 정의합니다.
 */
public class DoubleScoreItem extends GameItem {

    // 💡 아이템의 고유 정보들을 상수로 정의합니다.
    private static final String ITEM_ID = "DOUBLE_SCORE";
    private static final String ITEM_NAME = "Double Score (10s)";
    // 💡 클릭 시 사용자에게 보여줄 아이템 상세 설명
    private static final String ITEM_DESC = "Grants 2x score multiplier for 10 seconds. Essential for high score runs!";
    // 💡 사용할 이미지 파일명
    private static final String ITEM_IMAGE = "item_double_score.png"; 
    // 💡 상점에서 판매할 아이템 가격 (2000 pts로 설정)
    private static final int ITEM_PRICE = 2000; 

    /**
     * DoubleScoreItem의 생성자입니다.
     * 부모 클래스(GameItem)의 생성자를 호출하여 아이템의 기본 속성과 가격을 초기화합니다.
     */
    public DoubleScoreItem() {
        // 부모 생성자 호출: super(id, name, description, imageFileName, price)
        super(ITEM_ID, ITEM_NAME, ITEM_DESC, ITEM_IMAGE, ITEM_PRICE);
    }

}