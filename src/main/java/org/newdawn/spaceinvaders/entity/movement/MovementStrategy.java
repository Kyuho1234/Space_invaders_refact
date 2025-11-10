package org.newdawn.spaceinvaders.entity.movement;

import org.newdawn.spaceinvaders.entity.AlienEntity;

/**
 * Strategy Pattern 적용
 * OCP (Open/Closed Principle) - 새로운 이동 패턴 추가 가능
 * DIP (Dependency Inversion Principle) - 추상화에 의존
 */
public interface MovementStrategy {
    /**
     * 이동 패턴 실행
     * @param alien 이동할 외계인
     * @param delta 시간 델타
     */
    void move(AlienEntity alien, long delta);

    /**
     * 이동 패턴 이름
     */
    String getName();
}
