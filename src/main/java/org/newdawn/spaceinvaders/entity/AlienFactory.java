package org.newdawn.spaceinvaders.entity;

import org.newdawn.spaceinvaders.Game;

/**
 * Factory Pattern 적용
 * OCP (Open/Closed Principle) - 새로운 타입 추가 시 확장 가능
 */
public class AlienFactory {
    private final Game game;

    public AlienFactory(Game game) {
        this.game = game;
    }

    /**
     * 스테이지와 위치에 따라 적절한 Alien 생성
     */
    public AlienEntity createAlien(int stage, int row, int col, int x, int y) {
        AlienEntity.AlienType type = determineAlienType(stage, row, col);
        AlienEntity alien = new AlienEntity(game, x, y, type);
        alien.setStageMultiplier(stage);
        return alien;
    }

    /**
     * 보스 Alien 생성
     */
    public AlienEntity createBoss(int x, int y, int stage) {
        AlienEntity boss = new AlienEntity(game, x, y, AlienEntity.AlienType.BOSS);
        boss.setStageMultiplier(stage);
        return boss;
    }

    /**
     * 스테이지별 Alien 타입 결정 전략
     */
    private AlienEntity.AlienType determineAlienType(int stage, int row, int col) {
        switch (stage) {
            case 1:
                return AlienEntity.AlienType.BASIC;

            case 2:
                return row == 0 ? AlienEntity.AlienType.FAST : AlienEntity.AlienType.BASIC;

            case 3:
                if (row == 0) return AlienEntity.AlienType.FAST;
                if (row == 1) return AlienEntity.AlienType.HEAVY;
                return AlienEntity.AlienType.BASIC;

            case 4:
                if (row == 0) {
                    return (col % 2 == 0) ? AlienEntity.AlienType.FAST : AlienEntity.AlienType.SPECIAL;
                }
                if (row == 1) return AlienEntity.AlienType.HEAVY;
                return AlienEntity.AlienType.BASIC;

            case 5:
                return getRandomAlienType();

            default:
                return AlienEntity.AlienType.BASIC;
        }
    }

    private AlienEntity.AlienType getRandomAlienType() {
        double random = Math.random();
        if (random < 0.3) return AlienEntity.AlienType.FAST;
        if (random < 0.6) return AlienEntity.AlienType.HEAVY;
        if (random < 0.8) return AlienEntity.AlienType.SPECIAL;
        return AlienEntity.AlienType.BASIC;
    }
}
