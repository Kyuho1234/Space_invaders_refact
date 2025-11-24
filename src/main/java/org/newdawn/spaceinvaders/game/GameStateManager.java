package org.newdawn.spaceinvaders.game;

import org.newdawn.spaceinvaders.GameConstants;
import org.newdawn.spaceinvaders.firebase.FirebaseManager;
import org.newdawn.spaceinvaders.settings.SettingsManager;
import java.util.logging.Logger;
import java.util.logging.Level;
/**
 * Manages all game state including scores, stages, health, and game flow states.
 * Follows Single Responsibility Principle - only handles game state.
 */
public class GameStateManager {

    // Score and stage state
    private int currentStage = 1;
    private int score = 0;
    private int finalScore = 0;
    private int alienKillPoints = 10;
    private boolean newHighScoreAchieved = false;
    private int maxClearedStage = 0;

    // Player health state
    private int playerMaxHealth = GameConstants.DEFAULT_PLAYER_MAX_HEALTH;
    private int playerHealth = playerMaxHealth;
    private int player2MaxHealth = GameConstants.DEFAULT_PLAYER_MAX_HEALTH;
    private int player2Health = player2MaxHealth;

    // Game flow state
    private boolean waitingForKeyPress = true;
    private boolean pausePromptActive = false;
    private boolean stageSelectActive = false;
    private int selectedStage = 1;
    private String message = "";

    // Player movement and shooting state
    private double moveSpeed = 300;
    private long firingInterval = 500;
    private final long[] fireStamps = new long[]{0L, 0L};

    // Enemy firing state
    private long enemyLastFire = 0;
    private long enemyFiringInterval = 1200;

    private final FirebaseManager firebaseManager;
    private static final Logger logger = Logger.getLogger(GameStateManager.class.getName());

    public GameStateManager(FirebaseManager firebaseManager) {
        this.firebaseManager = firebaseManager;
        loadMaxClearedStage();
    }

    /**
     * Load max cleared stage from Firebase
     */
    private void loadMaxClearedStage() {
        if (firebaseManager != null && firebaseManager.isLoggedIn()) {
            maxClearedStage = firebaseManager.getMaxClearedStage();
        } else {
            maxClearedStage = 0;
        }
    }

    /**
     * Apply permanent upgrades from Firebase
     */
    public void applyPermanentUpgrades() {
        if (firebaseManager == null || !firebaseManager.isLoggedIn()) {
            resetToDefaults();
            return;
        }

        int attackLevel = firebaseManager.getUpgradeLevel("attack");
        int healthLevel = firebaseManager.getUpgradeLevel("health");
        int speedLevel = firebaseManager.getUpgradeLevel("speed");

        // Apply upgrades
        firingInterval = (long)(500 * Math.pow(0.85, attackLevel));
        playerMaxHealth = 3 + healthLevel;
        playerHealth = playerMaxHealth;

        if (SettingsManager.isTwoPlayerEnabled()) {
            player2MaxHealth = 3 + healthLevel;
            player2Health = player2MaxHealth;
        }

        moveSpeed = 300 * Math.pow(1.12, speedLevel);

        logger.info("[Permanent Upgrades Applied]");
        // {0}: 레벨, {1}: 상세 수치
        logger.log(Level.INFO, "  Attack Level {0}: Fire Interval = {1}ms", new Object[]{attackLevel, firingInterval});
        logger.log(Level.INFO, "  Health Level {0}: Max HP = {1}", new Object[]{healthLevel, playerMaxHealth});
        logger.log(Level.INFO, "  Speed Level {0}: Move Speed = {1}", new Object[]{speedLevel, moveSpeed});
    }

    /**
     * Reset to default values when not logged in
     */
    private void resetToDefaults() {
        moveSpeed = 300;
        firingInterval = 500;
        playerMaxHealth = 3;
        playerHealth = playerMaxHealth;
        player2MaxHealth = 3;
        player2Health = player2MaxHealth;
    }

    /**
     * Reset game state for new game
     */
    public void resetForNewGame() {
        pausePromptActive = false;
        waitingForKeyPress = false;
    }

    /**
     * Add score with multiplier
     */
    public void addScore(int baseScore, double multiplier) {
        score += (int)Math.round(baseScore * currentStage * multiplier);
    }

    /**
     * Handle player death
     */
    public void handleDeath() {
        pausePromptActive = false;
        finalScore = score;

        // Save progress
        if (currentStage > maxClearedStage) {
            if (firebaseManager != null && firebaseManager.isLoggedIn()) {
                firebaseManager.saveMaxClearedStage(currentStage - 1);
                // {0}: 저장된 스테이지 번호
                logger.log(Level.INFO, "DEATH: Saved *previous* stage {0} as max.", (currentStage - 1));
            }
        }

        message = "Oh no! They got you, try again?";
        waitingForKeyPress = false;
        stageSelectActive = true;
        selectedStage = maxClearedStage + 1;

        score = 0;
        playerHealth = playerMaxHealth;
        if (SettingsManager.isTwoPlayerEnabled()) {
            player2Health = player2MaxHealth;
        }
    }

    /**
     * Handle stage victory
     */
    public void handleVictory(boolean isFinalStage) {
        pausePromptActive = false;
        finalScore = score;

        if (isFinalStage) {
            handleFinalStageVictory();
        } else {
            handleIntermediateStageVictory();
        }
    }

    private void handleFinalStageVictory() {
        message = "Congratulations! All stages completed! Final Score: " + finalScore;
        waitingForKeyPress = true;
        score = 0;
        currentStage = 1;
    }

    private void handleIntermediateStageVictory() {
        if (currentStage > maxClearedStage) {
            maxClearedStage = currentStage;
            if (firebaseManager != null && firebaseManager.isLoggedIn()) {
                firebaseManager.saveMaxClearedStage(maxClearedStage);
            }
        }

        // Award stage bonus
        int stageBonus = currentStage * 100;
        if (firebaseManager != null && firebaseManager.isLoggedIn()) {
            firebaseManager.addPoints(stageBonus);
        }

        waitingForKeyPress = false;
        stageSelectActive = true;
        selectedStage = maxClearedStage + 1;
    }

    /**
     * Save score as points to Firebase
     */
    public void saveScoreAsPoints() {
        if (firebaseManager == null || !firebaseManager.isLoggedIn() || score <= 0) {
            newHighScoreAchieved = false;
            return;
        }

        int currentHighestScore = firebaseManager.getHighestScore();
        boolean newHigh = false;

        if (score > currentHighestScore) {
            if (firebaseManager.updateHighestScore(score)) {
                newHigh = true;
            }
        }

        int currentPoints = firebaseManager.getUserPoints();
        int newPoints = currentPoints + score;
        firebaseManager.updateUserPoints(newPoints);

        newHighScoreAchieved = newHigh;

        // {0}: 획득 점수, {1}: 누적 포인트
        logger.log(Level.INFO, "Score: {0} saved as points. Total Points: {1}", new Object[]{score, newPoints});
        if (newHigh) {
                // {0}: 신기록 점수
                logger.log(Level.INFO, "🎉 NEW HIGH SCORE ACHIEVED: {0}", score);

        }
    }

    /**
     * Damage player 1
     */
    public boolean damagePlayer1(int damage) {
        if (playerHealth <= 0) return false;

        playerHealth -= Math.max(1, damage);
        if (playerHealth <= 0) {
            playerHealth = 0;
            return true; // Player died
        }
        return false;
    }

    /**
     * Damage player 2
     */
    public boolean damagePlayer2(int damage) {
        if (player2Health <= 0) return false;

        player2Health -= Math.max(1, damage);
        if (player2Health <= 0) {
            player2Health = 0;
            return true; // Player died
        }
        return false;
    }

    /**
     * Check if game is over (all players dead)
     */
    public boolean isGameOver(boolean isTwoPlayerMode) {
        if (isTwoPlayerMode) {
            return playerHealth <= 0 && player2Health <= 0;
        }
        return playerHealth <= 0;
    }

    /**
     * Heal player (for item use)
     */
    public void healPlayer1() {
        if (playerHealth > 0) {
            playerHealth = Math.min(playerMaxHealth, playerHealth + 1);
        }
    }

    /**
     * Heal player 2 (for item use)
     */
    public void healPlayer2() {
        if (player2Health > 0) {
            player2Health = Math.min(player2MaxHealth, player2Health + 1);
        }
    }

    // Getters and setters
    public int getCurrentStage() { return currentStage; }
    public void setCurrentStage(int stage) { this.currentStage = stage; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getFinalScore() { return finalScore; }

    public int getAlienKillPoints() { return alienKillPoints; }

    public boolean isNewHighScoreAchieved() { return newHighScoreAchieved; }

    public int getMaxClearedStage() { return maxClearedStage; }
    public void setMaxClearedStage(int stage) { this.maxClearedStage = stage; }

    public int getPlayerMaxHealth() { return playerMaxHealth; }
    public int getPlayerHealth() { return playerHealth; }

    public int getPlayer2MaxHealth() { return player2MaxHealth; }
    public int getPlayer2Health() { return player2Health; }

    public boolean isWaitingForKeyPress() { return waitingForKeyPress; }
    public void setWaitingForKeyPress(boolean waiting) { this.waitingForKeyPress = waiting; }

    public boolean isPausePromptActive() { return pausePromptActive; }
    public void setPausePromptActive(boolean active) { this.pausePromptActive = active; }

    public boolean isStageSelectActive() { return stageSelectActive; }
    public void setStageSelectActive(boolean active) { this.stageSelectActive = active; }

    public int getSelectedStage() { return selectedStage; }
    public void setSelectedStage(int stage) { this.selectedStage = stage; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public double getMoveSpeed() { return moveSpeed; }

    public long getFiringInterval() { return firingInterval; }

    public long[] getFireStamps() { return fireStamps; }

    public long getEnemyLastFire() { return enemyLastFire; }
    public void setEnemyLastFire(long time) { this.enemyLastFire = time; }

    public long getEnemyFiringInterval() { return enemyFiringInterval; }
}
