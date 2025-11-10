package org.newdawn.spaceinvaders.entity.movement;

import org.newdawn.spaceinvaders.entity.AlienEntity;

/**
 * Zigzag movement pattern - alternates vertical direction periodically
 */
public class ZigzagMovement implements MovementStrategy {
    private boolean zigzagDirection = false;
    private long zigzagTimer = 0;
    private static final long ZIGZAG_INTERVAL = 800; // ms

    @Override
    public void move(AlienEntity alien, long delta) {
        zigzagTimer += delta;

        // Toggle direction at intervals
        if (zigzagTimer > ZIGZAG_INTERVAL) {
            zigzagDirection = !zigzagDirection;
            zigzagTimer = 0;
        }

        // Apply zigzag vertical movement (FAST type gets more aggressive movement)
        double verticalSpeed = zigzagDirection ? 25 : -25;
        alien.setVerticalMovement(verticalSpeed);
    }

    @Override
    public String getName() {
        return "zigzag";
    }
}
