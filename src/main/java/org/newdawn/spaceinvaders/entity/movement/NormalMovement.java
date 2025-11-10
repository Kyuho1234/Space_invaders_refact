package org.newdawn.spaceinvaders.entity.movement;

import org.newdawn.spaceinvaders.entity.AlienEntity;

/**
 * Normal movement pattern - horizontal movement only
 */
public class NormalMovement implements MovementStrategy {
    @Override
    public void move(AlienEntity alien, long delta) {
        // Basic horizontal-only movement
        alien.setVerticalMovement(0);
    }

    @Override
    public String getName() {
        return "normal";
    }
}
