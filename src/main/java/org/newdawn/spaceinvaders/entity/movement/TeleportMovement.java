package org.newdawn.spaceinvaders.entity.movement;

import org.newdawn.spaceinvaders.entity.AlienEntity;

/**
 * Teleport movement pattern - periodically teleports to random positions
 */
public class TeleportMovement implements MovementStrategy {
    private long teleportTimer = 0;
    private static final long TELEPORT_INTERVAL = 2500; // 2.5 seconds
    private static final double MIN_X = 50;
    private static final double MAX_X = 750;
    private static final double MIN_Y = 50;
    private static final double MAX_Y = 500;

    @Override
    public void move(AlienEntity alien, long delta) {
        teleportTimer += delta;

        if (teleportTimer > TELEPORT_INTERVAL) {
            // Teleport to random position within bounds
            double currentY = alien.getYDouble();
            double newX = MIN_X + Math.random() * (MAX_X - MIN_X);
            double newY = currentY + (Math.random() * 60 - 30); // Within ±30 pixels of current Y

            // Keep within screen bounds
            if (newX < 10) newX = 10;
            if (newX > 750) newX = 750;
            if (newY < MIN_Y) newY = MIN_Y;
            if (newY > MAX_Y) newY = MAX_Y;

            // Instant teleport
            alien.setX(newX);
            alien.setY(newY);

            // Reverse horizontal direction occasionally
            if (Math.random() < 0.3) {
                alien.setHorizontalMovement(-alien.getHorizontalMovement());
            }

            teleportTimer = 0;
        }

        // No vertical movement between teleports
        alien.setVerticalMovement(0);
    }

    @Override
    public String getName() {
        return "teleport";
    }
}
