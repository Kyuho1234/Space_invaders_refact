package org.newdawn.spaceinvaders.entity.movement;

import org.newdawn.spaceinvaders.entity.AlienEntity;

/**
 * Boss movement pattern - enhanced wave movement with higher amplitude
 */
public class BossMovement implements MovementStrategy {
    private double timeAccumulator = 0;
    private double originalY = -1;
    private static final double WAVE_FREQUENCY = 0.0075; // 1.5x faster than normal
    private static final double WAVE_AMPLITUDE = 50; // Larger amplitude

    @Override
    public void move(AlienEntity alien, long delta) {
        // Initialize original Y on first call
        if (originalY < 0) {
            originalY = alien.getYDouble();
        }

        timeAccumulator += delta;

        // Calculate enhanced wave Y position for boss
        double bossWaveY = originalY + Math.sin(timeAccumulator * WAVE_FREQUENCY) * WAVE_AMPLITUDE;

        // Keep boss within bounds
        if (bossWaveY > 400) bossWaveY = 400;
        if (bossWaveY < originalY - WAVE_AMPLITUDE) bossWaveY = originalY - WAVE_AMPLITUDE;

        // More aggressive transition for boss
        double currentY = alien.getYDouble();
        double dy = (bossWaveY - currentY) * 1.2;
        alien.setVerticalMovement(dy);
    }

    @Override
    public String getName() {
        return "boss";
    }
}
