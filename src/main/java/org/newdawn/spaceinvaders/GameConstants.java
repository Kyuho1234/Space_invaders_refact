package org.newdawn.spaceinvaders;

/**
 * Game-wide constants to eliminate magic numbers.
 * This class centralizes all hardcoded values used throughout the game.
 */
public final class GameConstants {

    // Prevent instantiation
    private GameConstants() {
        throw new AssertionError("Cannot instantiate GameConstants");
    }

    // ==================== Screen Dimensions ====================

    /** Screen width in pixels */
    public static final int SCREEN_WIDTH = 1200;

    /** Screen height in pixels */
    public static final int SCREEN_HEIGHT = 900;

    // ==================== Player Movement Boundaries ====================

    /** Left boundary for player movement */
    public static final int PLAYER_LEFT_BOUND = 10;

    /** Right boundary for player movement */
    public static final int PLAYER_RIGHT_BOUND = 1150;

    /** Right boundary for alien movement */
    public static final int ALIEN_RIGHT_BOUND = 1150;

    // ==================== Player Starting Positions ====================

    /** Player 1 starting X position */
    public static final int PLAYER1_START_X = 370;

    /** Player 1 starting Y position */
    public static final int PLAYER1_START_Y = 800;

    /** Player 2 starting X position */
    public static final int PLAYER2_START_X = 450;

    /** Player 2 starting Y position */
    public static final int PLAYER2_START_Y = 800;

    // ==================== Alien Properties ====================

    /** Y position where aliens cause game over if they reach */
    public static final int ALIEN_DEATH_LINE = 750;

    /** Movement speed for BASIC type aliens (pixels/sec) */
    public static final int BASIC_ALIEN_SPEED = 75;

    /** Movement speed for FAST type aliens (pixels/sec) */
    public static final int FAST_ALIEN_SPEED = 120;

    /** Movement speed for HEAVY type aliens (pixels/sec) */
    public static final int HEAVY_ALIEN_SPEED = 50;

    /** Animation frame duration for aliens in milliseconds */
    public static final int ALIEN_ANIMATION_DURATION_MS = 150;

    // ==================== Player Health Display ====================

    /** Width of each HP segment in pixels */
    public static final int HP_SEGMENT_WIDTH = 30;

    /** Height of each HP segment in pixels */
    public static final int HP_SEGMENT_HEIGHT = 8;

    /** Gap between HP segments in pixels */
    public static final int HP_SEGMENT_GAP = 6;

    // ==================== Default Player Stats ====================

    /** Default maximum health for players */
    public static final int DEFAULT_PLAYER_MAX_HEALTH = 3;

    /** Default player movement speed in pixels/sec */
    public static final double DEFAULT_PLAYER_MOVE_SPEED = 300.0;

    /** Default firing interval in milliseconds */
    public static final long DEFAULT_FIRING_INTERVAL = 500L;
}
