package org.newdawn.spaceinvaders.entity;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import org.newdawn.spaceinvaders.Game;
import org.newdawn.spaceinvaders.GameConstants;
import org.newdawn.spaceinvaders.Sprite;
import org.newdawn.spaceinvaders.SpriteStore;
import org.newdawn.spaceinvaders.entity.movement.*;

/**
 * An entity which represents one of our space invader aliens.
 * 
 * @author Kevin Glass
 */
public class AlienEntity extends Entity {
	/** Alien type enum */
	public enum AlienType {
		BASIC, FAST, HEAVY, SPECIAL, BOSS
	}

	/** Movement type constant */
	private static final String MOVEMENT_NORMAL = "normal";
	private static final String MOVEMENT_ZIGZAG = "zigzag";
	private static final String MOVEMENT_WAVE = "wave";
	private static final String MOVEMENT_TELEPORT = "teleport";

	/** The speed at which the alient moves horizontally */
	private double moveSpeed = 75;
	/** The game in which the entity exists */
	private Game game;
	/** The animation frames */
	private Sprite[] frames = new Sprite[4];
	/** The time since the last frame change took place */
	private long lastFrameChange;
	/** The frame duration in milliseconds, i.e. how long any given frame of animation lasts */
	private long frameDuration = 250;
	/** The current frame of animation being displayed */
	private int frameNumber;
	/** Movement strategy using Strategy Pattern (OCP, DIP) */
	private MovementStrategy movementStrategy;
	/** Movement pattern type */
	private String movementType = MOVEMENT_NORMAL;
	/** Original Y position for wave calculations */
	private double originalY;
	/** Boss properties */
	private boolean isBoss = false;
	private int health = 1;
	private double stageMultiplier = 1.0;
	/** Alien type and properties */
	private AlienType alienType = AlienType.BASIC;
	private int baseHealth = 1;
	private int scoreValue = 10;
	/** Firing properties */
	private double firingProbability = 1.0; // 발사 확률 배수 (1.0 = 기본)
	private int shotCount = 1; // 한 번에 발사하는 탄환 수
	private double shotSpreadAngle = 0; // 발사 각도 (라디안, 0 = 직선)
	/** Health bar animation */
	private long lastHitTime = 0;
	private boolean showDamageEffect = false;

	/**
	 * Create a new alien entity
	 *
	 * @param game The game in which this entity is being created
	 * @param x The intial x location of this alien
	 * @param y The intial y location of this alient
	 */
	public AlienEntity(Game game,int x,int y) {
		this(game, x, y, AlienType.BASIC);
	}

	/**
	 * Create a new alien entity with specified type
	 *
	 * @param game The game in which this entity is being created
	 * @param x The intial x location of this alien
	 * @param y The intial y location of this alien
	 * @param type The type of alien to create
	 */
	public AlienEntity(Game game,int x,int y, AlienType type) {
		super("sprites/alien.gif",x,y);

		this.game = game;
		this.originalY = y;
		this.alienType = type;

		// Setup alien properties based on type
		setupAlienType(type);

		// Setup animation frames with appropriate colors
		setupAnimationFrames();

		// Setup movement strategy based on type (Strategy Pattern)
		setupMovementStrategy();

		dx = -moveSpeed;
	}

	/**
	 * Setup alien properties based on type
	 */
	private void setupAlienType(AlienType type) {
		switch (type) {
			case BASIC:
				baseHealth = 1;
				scoreValue = 10;
				moveSpeed = GameConstants.BASIC_ALIEN_SPEED;
				movementType = MOVEMENT_NORMAL;
				firingProbability = 1.0; // 기본 발사 확률
				shotCount = 1;
				shotSpreadAngle = 0;
				break;

			case FAST:
				baseHealth = 1;
				scoreValue = 20;
				moveSpeed = GameConstants.FAST_ALIEN_SPEED;
				movementType = MOVEMENT_ZIGZAG;
				frameDuration = GameConstants.ALIEN_ANIMATION_DURATION_MS; // Faster animation
				firingProbability = 0.7; // 빠르지만 덜 쏨
				shotCount = 1;
				shotSpreadAngle = 0;
				break;

			case HEAVY:
				baseHealth = 2;
				scoreValue = 30;
				moveSpeed = GameConstants.HEAVY_ALIEN_SPEED;
				movementType = MOVEMENT_NORMAL;
				frameDuration = 400; // Slower animation
				firingProbability = 1.5; // 느리지만 더 자주 쏨
				shotCount = 1;
				shotSpreadAngle = 0;
				break;

			case SPECIAL:
				baseHealth = 1;
				scoreValue = 25;
				moveSpeed = 60;
				movementType = MOVEMENT_TELEPORT;
				firingProbability = 1.2; // 약간 더 자주 쏨
				shotCount = 2; // 2발 동시 발사
				shotSpreadAngle = Math.PI / 12; // 15도 각도
				break;

			case BOSS:
				baseHealth = 5;
				scoreValue = 100;
				moveSpeed = 90;
				movementType = MOVEMENT_WAVE;
				isBoss = true;
				firingProbability = 2.5; // 매우 공격적
				shotCount = 3; // 3발 동시 발사 (부채꼴)
				shotSpreadAngle = Math.PI / 6; // 30도 각도
				break;
		}
		health = baseHealth;
	}

	/**
	 * Setup movement strategy based on alien type (Strategy Pattern - OCP, DIP)
	 */
	private void setupMovementStrategy() {
		switch (movementType) {
			case MOVEMENT_ZIGZAG:
				movementStrategy = new ZigzagMovement();
				break;
			case MOVEMENT_WAVE:
				movementStrategy = new WaveMovement();
				break;
			case MOVEMENT_TELEPORT:
				movementStrategy = new TeleportMovement();
				break;
			case MOVEMENT_NORMAL:
			default:
				movementStrategy = new NormalMovement();
				break;
		}

		// Boss uses special boss movement strategy
		if (isBoss) {
			movementStrategy = new BossMovement();
		}
	}

	/**
	 * Setup animation frames with type-specific colors
	 */
	private void setupAnimationFrames() {
		// Get base sprites
		Sprite baseSprite = SpriteStore.get().getSprite("sprites/alien.gif");
		Sprite baseSprite2 = SpriteStore.get().getSprite("sprites/alien2.gif");
		Sprite baseSprite3 = SpriteStore.get().getSprite("sprites/alien3.gif");

		// Apply color tinting based on alien type
		Color tintColor = getTintColor();
		if (tintColor != null) {
			frames[0] = baseSprite.createTintedSprite(tintColor);
			frames[1] = baseSprite2.createTintedSprite(tintColor);
			frames[2] = frames[0];
			frames[3] = baseSprite3.createTintedSprite(tintColor);
			sprite = frames[0];
		} else {
			// Default setup for BASIC type
			frames[0] = baseSprite;
			frames[1] = baseSprite2;
			frames[2] = frames[0];
			frames[3] = baseSprite3;
			sprite = frames[0];
		}
	}

	/**
	 * Get tint color based on alien type
	 */
	private Color getTintColor() {
		switch (alienType) {
			case BASIC:
				return null; // No tint (original green)
			case FAST:
				return new Color(0, 100, 255, 180); // Blue tint
			case HEAVY:
				return new Color(255, 50, 50, 180); // Red tint
			case SPECIAL:
				return new Color(200, 0, 255, 180); // Purple tint
			case BOSS:
				return new Color(255, 215, 0, 200); // Gold tint
			default:
				return null;
		}
	}

	/**
	 * Set stage multiplier for increased difficulty
	 * @param multiplier Stage-based speed/health multiplier
	 */
	public void setStageMultiplier(double multiplier) {
		this.stageMultiplier = multiplier;

		// Apply stage multiplier to movement speed
		this.moveSpeed = (alienType == AlienType.FAST ? 120 :
						  alienType == AlienType.HEAVY ? 50 :
						  alienType == AlienType.SPECIAL ? 60 :
						  alienType == AlienType.BOSS ? 90 : 75) * multiplier;

		// Apply stage multiplier to health
		this.health = (int)(baseHealth * multiplier);

		// Update movement speed
		dx = dx > 0 ? moveSpeed : -moveSpeed;
	}

	/**
	 * Request that this alien moved based on time elapsed
	 *
	 * @param delta The time that has elapsed since last move
	 */
	public void move(long delta) {
		// since the move tells us how much time has passed
		// by we can use it to drive the animation, however
		// its the not the prettiest solution
		lastFrameChange += delta;

		// if we need to change the frame, update the frame number
		// and flip over the sprite in use
		if (lastFrameChange > frameDuration) {
			// reset our frame change time counter
			lastFrameChange = 0;

			// update the frame
			frameNumber++;
			if (frameNumber >= frames.length) {
				frameNumber = 0;
			}

			sprite = frames[frameNumber];
		}

		// Apply movement strategy (Strategy Pattern - delegates to strategy object)
		if (movementStrategy != null) {
			movementStrategy.move(this, delta);
		}

		// if we have reached the left hand side of the screen and
		// are moving left then request a logic update
		if ((dx < 0) && (x < GameConstants.PLAYER_LEFT_BOUND)) {
			game.updateLogic();
		}
		// and vice vesa, if we have reached the right hand side of
		// the screen and are moving right, request a logic update
		if ((dx > 0) && (x > GameConstants.ALIEN_RIGHT_BOUND)) {
			game.updateLogic();
		}

		// proceed with normal move
		super.move(delta);
	}

	/**
	 * Update the game logic related to aliens
	 */
	public void doLogic() {
		// swap over horizontal movement and move down the
		// screen a bit
		dx = -dx;

		if (isBoss) {
			// Boss moves down more slowly
			y += 5;
		} else {
			y += 10;
		}

		// Update original Y for wave patterns after moving down
		originalY = y;

		// if we've reached the bottom of the screen then the player
		// dies
		if (y > GameConstants.ALIEN_DEATH_LINE) {
			game.notifyDeath();
		}
	}
	
	/**
	 * Notification that this alien has collided with another entity
	 *
	 * @param other The other entity
	 */
	public void collidedWith(Entity other) {
		// Check if collided with a shot
		if (other instanceof ShotEntity) {
			// Remove the shot
			game.removeEntity(other);

			// Reduce health and show damage effect
			health--;
			lastHitTime = System.currentTimeMillis();
			showDamageEffect = true;

			if (health <= 0) {
				// Remove this alien
				game.removeEntity(this);

				// Notify game with appropriate score based on alien type
				if (isBoss) {
					game.notifyBossKilled();
				} else {
					game.notifyAlienKilled(scoreValue);
				}
			}
		}
	}

	/**
	 * Get the alien type
	 */
	public AlienType getAlienType() {
		return alienType;
	}

	/**
	 * Get the score value for this alien
	 */
	public int getScoreValue() {
		return scoreValue;
	}

	/**
	 * Get the firing probability multiplier for this alien
	 */
	public double getFiringProbability() {
		return firingProbability;
	}

	/**
	 * Get the number of shots this alien fires at once
	 */
	public int getShotCount() {
		return shotCount;
	}

	/**
	 * Get the spread angle for multi-shot attacks
	 */
	public double getShotSpreadAngle() {
		return shotSpreadAngle;
	}

	/**
	 * Set movement strategy (Strategy Pattern - allows runtime strategy changes)
	 */
	public void setMovementStrategy(MovementStrategy strategy) {
		this.movementStrategy = strategy;
	}

	/**
	 * Get movement strategy
	 */
	public MovementStrategy getMovementStrategy() {
		return movementStrategy;
	}

	/**
	 * Get initial X position (for MovementStrategy implementations)
	 */
	public double getInitialX() {
		return x;
	}

	/**
	 * Get Y position as double (for MovementStrategy implementations)
	 */
	public double getYDouble() {
		return y;
	}

	/**
	 * Set X position (for MovementStrategy implementations like teleport)
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * Set Y position (for MovementStrategy implementations like teleport)
	 */
	public void setY(double y) {
		this.y = y;
	}

	/**
	 * Draw this alien and its health bar
	 */
	@Override
	public void draw(Graphics g) {
		// Draw the alien sprite first
		super.draw(g);

		// Only draw health bar if alien has more than 1 max health or is damaged
		if (baseHealth > 1 || health < baseHealth) {
			drawHealthBar(g);
		}
	}

	/**
	 * Draw health bar above the alien
	 */
	private void drawHealthBar(Graphics g) {
		Graphics2D g2d = (Graphics2D) g.create();

		// Health bar dimensions
		int barWidth = 30;
		int barHeight = 4;
		int barX = (int) x + (sprite.getWidth() - barWidth) / 2;
		int barY = (int) y - 8;

		// Check for damage effect (flash effect for 200ms after hit)
		long currentTime = System.currentTimeMillis();
		boolean flashingRed = showDamageEffect && (currentTime - lastHitTime) < 200;
		if (currentTime - lastHitTime > 200) {
			showDamageEffect = false;
		}

		// Background (dark red, or bright red when flashing)
		if (flashingRed) {
			g2d.setColor(new Color(150, 0, 0)); // Brighter red when hit
		} else {
			g2d.setColor(new Color(60, 0, 0)); // Normal dark background
		}
		g2d.fillRect(barX, barY, barWidth, barHeight);

		// Health bar (color based on health percentage)
		if (health > 0) {
			double healthPercentage = (double) health / baseHealth;
			int healthBarWidth = (int) (barWidth * healthPercentage);

			// Color changes based on health percentage (with flash effect)
			Color healthColor;
			if (flashingRed) {
				healthColor = new Color(255, 255, 255); // White flash when hit
			} else if (healthPercentage > 0.6) {
				healthColor = new Color(0, 200, 0); // Green
			} else if (healthPercentage > 0.3) {
				healthColor = new Color(255, 200, 0); // Orange
			} else {
				healthColor = new Color(255, 50, 50); // Red
			}

			g2d.setColor(healthColor);
			g2d.fillRect(barX, barY, healthBarWidth, barHeight);
		}

		// Border (white, or bright yellow when flashing)
		if (flashingRed) {
			g2d.setColor(new Color(255, 255, 0)); // Yellow border when hit
		} else {
			g2d.setColor(Color.WHITE); // Normal white border
		}
		g2d.drawRect(barX, barY, barWidth, barHeight);

		g2d.dispose();
	}
}