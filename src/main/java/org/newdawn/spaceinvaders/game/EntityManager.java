package org.newdawn.spaceinvaders.game;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.GameConstants;
import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.AlienFactory;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;
import org.newdawn.spaceinvaders.settings.SettingsManager;

/**
 * Manages all game entities - creation, removal, updates, and collisions.
 * Follows Single Responsibility Principle - only handles entity lifecycle.
 */
public class EntityManager {

    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> removeList = new ArrayList<>();

    private ShipEntity ship;
    private ShipEntity ship2;
    private int alienCount;

    private final Game game;
    private final AlienFactory alienFactory;
    private final EntityEventListener eventListener;

    /**
     * Listener interface for entity events
     */
    public interface EntityEventListener {
        void onAlienKilled(int score);
        void onBossKilled();
        void onPlayerHit(ShipEntity player, int damage);
        void onAllAliensKilled();
        void updateLogic();
    }

    public EntityManager(Game game, AlienFactory alienFactory, EntityEventListener eventListener) {
        this.game = game;
        this.alienFactory = alienFactory;
        this.eventListener = eventListener;
    }

    /**
     * Initialize entities for a new game
     */
    public void initEntities(int currentStage) {
        entities.clear();

        // Create player 1
        ship = new ShipEntity(game, "sprites/ship.gif",
                             GameConstants.PLAYER1_START_X, GameConstants.PLAYER1_START_Y);
        entities.add(ship);

        // Create player 2 if enabled
        boolean twoPlayerEnabled = SettingsManager.isTwoPlayerEnabled();
        if (twoPlayerEnabled) {
            ship2 = new ShipEntity(game, "sprites/ship.gif",
                                  GameConstants.PLAYER2_START_X, GameConstants.PLAYER2_START_Y);
            entities.add(ship2);
        } else {
            ship2 = null;
        }

        // Create aliens for current stage
        initAliensForStage(currentStage);
    }

    /**
     * Initialize aliens based on the current stage
     */
    private void initAliensForStage(int stage) {
        alienCount = 0;

        switch(stage) {
            case 1:
                createAlienFormation(1, 1, 350, 100, 50, 30, stage);
                break;
            case 2:
                createAlienFormation(1, 2, 300, 100, 100, 30, stage);
                break;
            case 3:
                createAlienFormation(2, 2, 250, 80, 150, 40, stage);
                break;
            case 4:
                createAlienFormation(2, 2, 250, 80, 150, 40, stage);
                break;
            case 5:
                createAlienFormation(1, 2, 200, 120, 200, 35, stage);
                createBossAlien(stage);
                break;
            default:
                createAlienFormation(1, 1, 350, 100, 50, 30, stage);
                break;
        }
    }

    /**
     * Create a formation of aliens
     */
    private void createAlienFormation(int rows, int cols, int startX, int startY,
                                     int spacingX, int spacingY, int stage) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                AlienEntity alien = alienFactory.createAlien(
                    stage, row, col,
                    startX + (col * spacingX),
                    startY + (row * spacingY)
                );
                entities.add(alien);
                alienCount++;
            }
        }
    }

    /**
     * Create boss alien
     */
    private void createBossAlien(int stage) {
        AlienEntity boss = alienFactory.createBoss(350, 100, stage);
        entities.add(boss);
        alienCount++;
    }

    /**
     * Move all entities
     */
    public void moveEntities(long delta) {
        for (Entity entity : entities) {
            entity.move(delta);
        }
    }

    /**
     * Check and handle collisions between all entities
     */
    public void checkCollisions() {
        for (int p = 0; p < entities.size(); p++) {
            for (int s = p + 1; s < entities.size(); s++) {
                Entity me = entities.get(p);
                Entity him = entities.get(s);
                if (me.collidesWith(him)) {
                    me.collidedWith(him);
                    him.collidedWith(me);
                }
            }
        }
    }

    /**
     * Remove dead entities
     */
    public void removeDeadEntities() {
        entities.removeAll(removeList);
        removeList.clear();
    }

    /**
     * Process entity logic
     */
    public void processEntityLogic() {
        for (Entity entity : entities) {
            entity.doLogic();
        }
    }

    /**
     * Remove an entity
     */
    public void removeEntity(Entity entity) {
        removeList.add(entity);
    }

    /**
     * Fire a shot from a ship
     */
    public void fireShot(Entity shooter, int playerIndex, long firingInterval, long[] fireStamps) {
        long now = System.currentTimeMillis();
        if (now - fireStamps[playerIndex] < firingInterval) return;
        fireStamps[playerIndex] = now;

        ShotEntity shot = new ShotEntity(game, "sprites/shot.gif",
                                        shooter.getX() + 10, shooter.getY() - 30);
        entities.add(shot);
    }

    /**
     * Handle alien killed event
     */
    public void notifyAlienKilled(int alienScore) {
        alienCount--;

        if (alienCount == 0) {
            eventListener.onAllAliensKilled();
            return;
        }

        // Speed up remaining aliens
        for (Entity entity : entities) {
            if (entity instanceof AlienEntity) {
                double speedIncrease = 1.02 + (0.005); // Can pass stage for difficulty
                entity.setHorizontalMovement(entity.getHorizontalMovement() * speedIncrease);
            }
        }

        eventListener.updateLogic();
    }

    /**
     * Get list of alive aliens for enemy firing
     */
    public List<AlienEntity> getAliveAliens() {
        List<AlienEntity> aliens = new ArrayList<>();
        for (Entity e : entities) {
            if (e instanceof AlienEntity) {
                aliens.add((AlienEntity) e);
            }
        }
        return aliens;
    }

    /**
     * Add enemy shot
     */
    public void addEnemyShot(int x, int y, double vx, double vy) {
        // This will be handled by inner class in Game for now
        // Could be extracted to a separate ShotFactory later
    }

    /**
     * Clear all entities
     */
    public void clearEntities() {
        entities.clear();
        removeList.clear();
    }

    // Getters
    public List<Entity> getEntities() { return entities; }
    public ShipEntity getShip() { return ship; }
    public ShipEntity getShip2() { return ship2; }
    public int getAlienCount() { return alienCount; }
    public void decrementAlienCount() { alienCount--; }
}
