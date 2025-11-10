package org.newdawn.spaceinvaders.entity.movement;

import org.newdawn.spaceinvaders.entity.AlienEntity;

/**
 * Wave movement pattern - moves in a sine wave pattern
 */
public class WaveMovement implements MovementStrategy {
    private double timeAccumulator = 0;
    private double originalY = -1;
    private static final double WAVE_FREQUENCY = 0.005;
    private static final double WAVE_AMPLITUDE = 30;

    @Override
    public void move(AlienEntity alien, long delta) {
        // Initialize original Y on first call
        if (originalY < 0) {
            originalY = alien.getYDouble();
        }

        timeAccumulator += delta;

        // Calculate sine wave Y position
        double waveY = originalY + Math.sin(timeAccumulator * WAVE_FREQUENCY) * WAVE_AMPLITUDE;

        // Limit wave movement to stay within screen bounds
        if (waveY > 500) waveY = 500;
        if (waveY < originalY - WAVE_AMPLITUDE) waveY = originalY - WAVE_AMPLITUDE;

        // Smooth transition to wave position
        double currentY = alien.getYDouble();
        double dy = (waveY - currentY) * 1.5;
        alien.setVerticalMovement(dy);
    }

    @Override
    public String getName() {
        return "wave";
    }
}
