package org.newdawn.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.awt.Image;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.newdawn.spaceinvaders.entity.AlienEntity;
import org.newdawn.spaceinvaders.entity.AlienFactory;
import org.newdawn.spaceinvaders.entity.Entity;
import org.newdawn.spaceinvaders.entity.ShipEntity;
import org.newdawn.spaceinvaders.entity.ShotEntity;
import org.newdawn.spaceinvaders.settings.SettingsDialog;
import org.newdawn.spaceinvaders.settings.SettingsManager;
import org.newdawn.spaceinvaders.firebase.FirebaseManager;
import org.newdawn.spaceinvaders.items.ItemManager;
import org.newdawn.spaceinvaders.rendering.ResourceLoader;
import org.newdawn.spaceinvaders.rendering.ItemPanelRenderer;
import org.newdawn.spaceinvaders.rendering.HPBarRenderer;
import org.newdawn.spaceinvaders.rendering.ScreenRenderer;
import org.newdawn.spaceinvaders.game.GameStateManager;
import org.newdawn.spaceinvaders.game.EntityManager;

import java.util.logging.Logger;
import java.util.logging.Level;
/**
 * The main hook of our game. This class with both act as a manager
 * for the display and central mediator for the game logic.
 *
 * Display management will consist of a loop that cycles round all
 * entities in the game asking them to move and then drawing them
 * in the appropriate place. With the help of an inner class it
 * will also allow the player to control the main ship.
 *
 * As a mediator it will be informed when entities within our game
 * detect events (e.g. alient killed, played died) and will take
 * appropriate game actions.
 *
 * @author Kevin Glass
 */
public class Game extends Canvas {
	private static final Logger logger = Logger.getLogger(Game.class.getName());

	/** The stragey that allows us to use accelerate page flipping */
	private transient BufferStrategy strategy;
	/** True if the game is currently "running", i.e. the game loop is looping */
	private boolean gameRunning = true;
	/** The list of all the entities that exist in our game */
	private transient ArrayList<Entity> entities = new ArrayList<>();
	/** The list of entities that need to be removed from the game this loop */
	private transient ArrayList<Entity> removeList = new ArrayList<>();
	// [2P 수정] 플레이어 변수를 P1, P2로 명확히 구분
	private transient ShipEntity ship;
	private transient ShipEntity ship2;
	private boolean isTwoPlayerGame = false;
	/** The speed at which the player's ship should move (pixels/sec) */
	private double moveSpeed = 300;
	/** The time at which last fired a shot */

	// [2P 수정] 발사 시간과 간격을 플레이어별로 관리하기 위한 배열
	private final long[] fireStamps = new long[]{0L, 0L};
	private long lastFire = 0;
	/** The interval between our players shot (ms) */
	private long firingInterval = 500;
	/** Player health */
	private int playerMaxHealth = GameConstants.DEFAULT_PLAYER_MAX_HEALTH;
	private int playerHealth = playerMaxHealth;
	// =================================================================
	// === 2P FEATURE: Added separate health for the second player ===
	// =================================================================
	private int player2MaxHealth = GameConstants.DEFAULT_PLAYER_MAX_HEALTH;
	private int player2Health = player2MaxHealth;
	/** Enemy firing control */
	private long enemyLastFire = 0;
	private long enemyFiringInterval = 1200; // ms
	/** The number of aliens left on the screen */
	private int alienCount;
	/** Total score */
	private int score = 0;
	private int finalScore = 0;
	/** Points per alien kill */
	private int alienKillPoints = 10;
	private boolean newHighScoreAchieved = false;

	/** Simple left-panel item list to display vertically */
	private java.util.List<String> itemUIList = java.util.Arrays.asList(
			"item_ammo_boost.png",
			"item_double_score.png",
			"item_invincibility.png",
			"item_plusLife.png"
	);
	/** Optional: item counts matching itemUIList order (null = no count shown) */
	private int[] itemUICounts = new int[] {0, 0, 0, 0};
	/** Icons for items, aligned with itemUIList order */
	private transient java.util.List<Image> itemUIIcons = new java.util.ArrayList<>();

	/** The message to display which waiting for a key press */
	private String message = "";
	/** True if we're holding up game play until a key has been pressed */
	private boolean waitingForKeyPress = true;
	/** True if the left cursor key is currently pressed */
	// [2P 수정] 키 입력 상태 변수 P1, P2로 명확히 구분
	private boolean leftPressed = false;
	private boolean rightPressed = false;
	private boolean firePressed = false;
	private boolean leftPressed2 = false;
	private boolean rightPressed2 = false;
	private boolean firePressed2 = false;

	/** True if game logic needs to be applied this loop, normally as a result of a game event */
	private boolean logicRequiredThisLoop = false;
	/** The last time at which we recorded the frame rate */
	private long lastFpsTime;
	/** The current number of frames recorded */
	private int fps;
	/** The normal title of the game window */
	private String windowTitle = "Space Invaders 102";
	/** The game window that we'll update with the frame count */
	private JFrame container;

	private boolean twoPlayerEnabled = false;






	/** Firebase manager for user data */
	private transient FirebaseManager firebaseManager;
	/** User's purchased items */
	private transient java.util.List<String> purchasedItems;
	/** Item manager for usage, counts and buffs */
	private transient ItemManager itemManager;
	/** Alien factory for creating aliens (Factory Pattern - OCP, DIP) */
	private transient AlienFactory alienFactory;
	/** Rendering components (SRP - Single Responsibility) */
	private transient HPBarRenderer hpBarRenderer;
	private transient ItemPanelRenderer itemPanelRenderer;
	private transient ScreenRenderer screenRenderer;
	/** Game managers (SRP - Single Responsibility) */
	private transient GameStateManager stateManager;
	private transient EntityManager entityManager;
	/** True if pause-confirm overlay is active (ESC during gameplay) */
	private boolean pausePromptActive = false;
	private boolean stageSelectActive = false; // 스테이지 선택 화면 활성화 상태
	private int selectedStage = 1;             // 현재 선택된 스테이지 번호 (화면 UI에서 사용)
	// 파이어베이스에 저장된 최대 클리어 스테이지
	private int maxClearedStage = 0;

	/**
	 * Construct our game and set it running.
	 */
	public Game() {

		firebaseManager = FirebaseManager.getInstance();
		purchasedItems = new java.util.ArrayList<>();

		// 로그인한 경우 구매한 아이템 불러오기
		if (firebaseManager.isLoggedIn()) {
			purchasedItems = firebaseManager.getPurchasedItems();
		}
		// ItemManager 초기화 및 카운트 반영
		itemManager = new ItemManager(firebaseManager);
		itemManager.setCountsFromPurchased(purchasedItems);
		syncItemCountsFromManager();

		// Initialize AlienFactory (Factory Pattern)
		alienFactory = new AlienFactory(this);

		// Initialize rendering components (SRP - Single Responsibility)
		hpBarRenderer = new HPBarRenderer();
		screenRenderer = new ScreenRenderer();

		// Initialize game managers (SRP - Single Responsibility)
		stateManager = new GameStateManager(firebaseManager);
		entityManager = new EntityManager(this, alienFactory, new EntityManager.EntityEventListener() {
			@Override
			public void onAlienKilled(int score) {
				notifyAlienKilled(score);
			}

			@Override
			public void onBossKilled() {
				notifyBossKilled();
			}

			@Override
			public void onPlayerHit(ShipEntity player, int damage) {
				notifyPlayerHit(player, damage);
			}

			@Override
			public void onAllAliensKilled() {
				notifyWin();
			}

			@Override
			public void updateLogic() {
				logicRequiredThisLoop = true;
			}
		});

		// create a frame to contain our game
		container = new JFrame("Space Invaders 102");

		// get hold the content of the frame and set up the resolution of the game
		JPanel panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT));
		panel.setLayout(null);

		// setup our canvas size and put it into the content of the frame
		setBounds(0, 0, GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT);
		panel.add(this);

		// Tell AWT not to bother repainting our canvas since we're
		// going to do that our self in accelerated mode
		setIgnoreRepaint(true);

		// finally make the window visible
		container.pack();
		container.setResizable(false);

		// 💡 --- [핵심 수정] 포커스 문제 해결 ---
		// 1. KeyListener를 먼저 추가합니다.
		addKeyListener(new KeyInputHandler());

		// 2. 이 Canvas가 키보드 입력을 받을 수 있도록 명시적으로 설정합니다.
		setFocusable(true);

		// 3. 창을 화면에 표시합니다.
		container.setVisible(true);

		// 4. 화면에 표시된 *후에* 포커스를 요청하는 것이 더 안정적이며,
		//    requestFocus() 보다 requestFocusInWindow()가 더 권장됩니다.
		java.awt.EventQueue.invokeLater(() -> requestFocusInWindow());
		// --- 여기까지 수정 ---

		container.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		// load item icons from resources using ResourceLoader
		itemUIIcons = ResourceLoader.loadItemUIIcons(itemUIList);

		// Initialize ItemPanelRenderer after icons are loaded
		updateItemPanelRenderer();


		// create the buffering strategy which will allow AWT
		// to manage our accelerated graphics
		createBufferStrategy(2);
		strategy = getBufferStrategy();

		// 최대 클리어 스테이지 로드
		if (firebaseManager != null && firebaseManager.isLoggedIn()) {
			maxClearedStage = firebaseManager.getMaxClearedStage();
		} else {
			maxClearedStage = 0;
		}

		// 게임 시작 시 바로 스테이지 선택 화면으로 전환
		waitingForKeyPress = false;
		stageSelectActive = true;
		selectedStage = (maxClearedStage > 0) ? (maxClearedStage + 1) : 1;
	}


	/**
	 * Start a fresh game, this should clear out any old data and
	 * create a new set.
	 */
	private void startGame() {
		// Use GameStateManager for state reset
		stateManager.resetForNewGame();
		stateManager.applyPermanentUpgrades();
		pausePromptActive = false;

		if (firebaseManager != null && firebaseManager.isLoggedIn()) {
			purchasedItems = firebaseManager.getPurchasedItems();
			if (itemManager == null) itemManager = new ItemManager(firebaseManager);
			itemManager.setCountsFromPurchased(purchasedItems);
			syncItemCountsFromManager();
		}

		// Use EntityManager to initialize entities
		isTwoPlayerGame = SettingsManager.isTwoPlayerEnabled();
		entities.clear();
		entityManager.initEntities(stateManager.getCurrentStage());

		// Sync entities list (for compatibility during migration)
		entities.addAll(entityManager.getEntities());
		ship = entityManager.getShip();
		ship2 = entityManager.getShip2();
		alienCount = entityManager.getAlienCount();

		// blank out any keyboard settings we might currently have
		leftPressed= false;
		rightPressed = false;
		firePressed = false;
		leftPressed2 = false;
		rightPressed2 = false;
		firePressed2 = false;

		// Get move speed and firing interval from state manager
		moveSpeed = stateManager.getMoveSpeed();
		firingInterval = stateManager.getFiringInterval();
	}



	/**
	 * Notification from a game entity that the logic of the game
	 * should be run at the next opportunity (normally as a result of some
	 * game event)
	 */
	public void updateLogic() {
		logicRequiredThisLoop = true;
	}

	/**
	 * Remove an entity from the game. The entity removed will
	 * no longer move or be drawn.
	 *
	 * @param entity The entity that should be removed
	 */
	public void removeEntity(Entity entity) {
		removeList.add(entity);
	}


	// Game.java: notifyDeath() 메서드 전체 (수정)
	public void notifyDeath() {
		pausePromptActive = false;

		// 1. 최고 점수 등극 메시지 출력을 위해 score 초기화 전에 finalScore에 저장
		finalScore = score;

		// 2. 게임 종료 시 점수를 포인트로 저장 및 최고 점수 갱신
		saveScoreAsPoints();

		// 💡 [핵심 수정] maxClearedStage 변수는 건드리지 않고, Firebase에 저장만 시도합니다.
		// 현재 플레이 중인 스테이지(currentStage)가 maxClearedStage보다 높을 경우에만 저장 시도
		if (stateManager.getCurrentStage() > maxClearedStage) {
			if (firebaseManager != null && firebaseManager.isLoggedIn()) {
				// ✅ 성공적으로 깬 마지막 스테이지 (현재 진행 중인 스테이지의 직전)를 저장
				//    Stage 3에서 죽었다면 (3-1=2) Stage 2를 저장
				firebaseManager.saveMaxClearedStage(stateManager.getCurrentStage() - 1);
				// {0}: 저장된 스테이지 번호 (현재 스테이지 - 1)
				logger.log(Level.WARNING, "DEATH: Saved *previous* stage {0} as max.", (stateManager.getCurrentStage() - 1));
			}
		}

		// 3. 사망 후 Stage Select 화면으로 전환
		message = "Oh no! They got you, try again?";
		waitingForKeyPress = false;
		stageSelectActive = true;

		// ✅ 커서 위치 설정: 화면에는 마지막으로 저장된 maxClearedStage를 로드하여 표시합니다.
		//    (로그아웃 없이 바로 선택 창이 뜨므로 maxClearedStage는 2를 유지해야 함)
		selectedStage = maxClearedStage + 1; // Stage 2 클리어 후 Stage 3을 선택하도록 유도

		// 4. 점수/체력 초기화
		score = 0;
		playerHealth = playerMaxHealth;
		// =================================================================
		// === 2P FEATURE: Reset P2 health on game over ===
		// =================================================================
		if (SettingsManager.isTwoPlayerEnabled()) {
			player2Health = player2MaxHealth;
		}
	}

	public void notifyWin() {
		pausePromptActive = false;
		finalScore = score;

		if (isFinalStageCompleted()) {
			handleFinalStageCompletion();
		} else {
			handleIntermediateStageCompletion();
		}
	}

	private boolean isFinalStageCompleted() {
		return stateManager.getCurrentStage() >= 5;
	}

	private void handleFinalStageCompletion() {
		saveScoreAsPoints();
		message = "Congratulations! All stages completed! Final Score: " + finalScore;
		waitingForKeyPress = true;
		score = 0;
		stateManager.setCurrentStage(1);
	}

	private void handleIntermediateStageCompletion() {
		updateMaxClearedStage();
		awardStageBonus();
		prepareStageSelection();
		clearGameEntities();
	}

	private void updateMaxClearedStage() {
		if (stateManager.getCurrentStage() > maxClearedStage) {
			maxClearedStage = stateManager.getCurrentStage();
			if (firebaseManager != null && firebaseManager.isLoggedIn()) {
				firebaseManager.saveMaxClearedStage(maxClearedStage);
			}
		}
	}

	private void awardStageBonus() {
		int stageBonus = stateManager.getCurrentStage() * 100;
		if (firebaseManager != null && firebaseManager.isLoggedIn()) {
			firebaseManager.addPoints(stageBonus);
		}
	}

	private void prepareStageSelection() {
		waitingForKeyPress = false;
		stageSelectActive = true;
		selectedStage = maxClearedStage + 1;
	}

	private void clearGameEntities() {
		entities.clear();
		removeList.clear();
	}


	/** 게임 종료 시 점수를 포인트로 저장하고 최고 점수를 갱신합니다. */
	private void saveScoreAsPoints() {
		if (firebaseManager.isLoggedIn() && score > 0) {
			// 1. 현재 최고 점수를 가져옵니다.
			int currentHighestScore = firebaseManager.getHighestScore();
			boolean newHigh = false;

			// 2. 최고 점수 비교 및 업데이트
			if (score > currentHighestScore) {
				// 지금까지의 최고 점수 < 현재 점수: 최고 점수 갱신
				if (firebaseManager.updateHighestScore(score)) {
					newHigh = true;
				}
			}

			// 3. 포인트 적립 (최고 점수와 별개로 적립)
			int currentPoints = firebaseManager.getUserPoints();
			int newPoints = currentPoints + score;
			firebaseManager.updateUserPoints(newPoints);

			// 4. 최고 점수 달성 플래그 설정 (게임 종료 화면 표시용)
			this.newHighScoreAchieved = newHigh;

			// {0}: 이번 판 점수, {1}: 누적된 총 포인트
			logger.log(Level.INFO, "Score: {0} saved as points. Total Points: {1}", new Object[]{score, newPoints});
			if (newHigh) {
				// {0}: 신기록 점수
				logger.log(Level.INFO, "🎉 NEW HIGH SCORE ACHIEVED: {0}", score);
			}
		} else {
			// 로그인되어 있지 않거나 점수가 0인 경우
			this.newHighScoreAchieved = false;
		}
	}

	/**
	 * Award the current in-stage score as Firebase points immediately.
	 * @return true if points were awarded (logged-in and score>0), else false
	 */
	private boolean awardCurrentScoreAsPoints() {
		if (firebaseManager != null && firebaseManager.isLoggedIn() && score > 0) {
			// Prefer additive helper if available; fallback to update
			try {
				int currentPoints = firebaseManager.getUserPoints();
				return firebaseManager.updateUserPoints(currentPoints + score);
			} catch (Exception ignore) { /* no-op */ }
		}
		return false;
	}

	/**
	 * Get user's purchased items
	 * @return List of purchased item IDs
	 */
	public java.util.List<String> getPurchasedItems() {
		return purchasedItems;
	}

	/**
	 * Notification that an alien has been killed
	 */
	public void notifyAlienKilled() {
		notifyAlienKilled(alienKillPoints);
	}

	/**
	 * Notification that an alien has been killed with specific score value
	 * @param alienScore The score value of the killed alien
	 */
	public void notifyAlienKilled(int alienScore) {
		// Add score based on alien type and current stage multiplier
		double mult = (itemManager != null) ? itemManager.currentScoreMultiplier() : 1.0;
		score += (int)Math.round(alienScore * stateManager.getCurrentStage() * mult);

		// reduce the alien count, if there are none left, the player has won!
		alienCount--;

		if (alienCount == 0) {
			notifyWin();
		}

		// if there are still some aliens left then they all need to get faster, so
		// speed up all the existing aliens
		for (int i=0;i<entities.size();i++) {
			Entity entity = (Entity) entities.get(i);

			if (entity instanceof AlienEntity) {
				// speed up by 2% (more aggressive on higher stages)
				double speedIncrease = 1.02 + (stateManager.getCurrentStage() * 0.005);
				entity.setHorizontalMovement(entity.getHorizontalMovement() * speedIncrease);
			}
		}
	}

	/**
	 * Notification that a boss alien has been killed (worth more points)
	 */
	public void notifyBossKilled() {
		// Boss is worth 10x regular alien points
		double mult = (itemManager != null) ? itemManager.currentScoreMultiplier() : 1.0;
		score += (int)Math.round(alienKillPoints * stateManager.getCurrentStage() * 10 * mult);
		alienCount--;

		if (alienCount == 0) {
			notifyWin();
		}
	}


	// 특정 플레이어로부터 사격시도
	private void tryToFireFrom(Entity shooter, int index) {
		// ItemManager의 발사 속도 배수 적용
		long effectiveInterval = firingInterval;
		if (itemManager != null) {
			effectiveInterval = (long) Math.max(1, Math.round(firingInterval * itemManager.currentFireRateMultiplier()));
		}

		long now = System.currentTimeMillis();
		if (now - fireStamps[index] < effectiveInterval) return;
		fireStamps[index] = now;

		// 발사
		ShotEntity shot = new ShotEntity(this, shooter.getX() + 10, shooter.getY() - 30);
		entities.add(shot);
	}


	// ===============================================================================================
	// === 2P FEATURE: REPLACED notifyPlayerHit(int) with notifyPlayerHit(ShipEntity, int) ===
	// This new method handles hits for a specific player and updates their individual health.
	// It also checks for the game over condition based on the number of active players.
	// ===============================================================================================
	/** Notification that a specific player has been hit */
	public void notifyPlayerHit(ShipEntity player, int damage) {
		if (shouldIgnoreHit()) {
			return;
		}

		applyDamageToPlayer(player, damage);
		checkGameOverCondition();
	}

	private boolean shouldIgnoreHit() {
		return waitingForKeyPress || (itemManager != null && itemManager.isInvincible());
	}

	private void applyDamageToPlayer(ShipEntity player, int damage) {
		if (player == ship) {
			applyDamageToPlayer1(damage);
		} else if (player == ship2) {
			applyDamageToPlayer2(damage);
		}
	}

	private void applyDamageToPlayer1(int damage) {
		if (playerHealth <= 0) return;

		playerHealth -= Math.max(1, damage);
		if (playerHealth <= 0) {
			playerHealth = 0;
			removeEntity(ship);
		}
	}

	private void applyDamageToPlayer2(int damage) {
		if (player2Health <= 0) return;

		player2Health -= Math.max(1, damage);
		if (player2Health <= 0) {
			player2Health = 0;
			removeEntity(ship2);
		}
	}

	private void checkGameOverCondition() {
		if (SettingsManager.isTwoPlayerEnabled()) {
			checkTwoPlayerGameOver();
		} else {
			checkSinglePlayerGameOver();
		}
	}

	private void checkTwoPlayerGameOver() {
		if (playerHealth <= 0 && player2Health <= 0) {
			notifyDeath();
		}
	}

	private void checkSinglePlayerGameOver() {
		if (playerHealth <= 0) {
			notifyDeath();
		}
	}


	/**
	 * Attempt to fire a shot from the player. Its called "try"
	 * since we must first check that the player can fire at this
	 * point, i.e. has he/she waited long enough between shots
	 */
	public void tryToFire() {
		// check that we have waiting long enough to fire
		long effectiveInterval = firingInterval;
		if (itemManager != null) {
			effectiveInterval = (long) Math.max(1, Math.round(firingInterval * itemManager.currentFireRateMultiplier()));
		}
		if (System.currentTimeMillis() - lastFire < effectiveInterval) {
			return;
		}

	}

	/**
	 * Return to main menu by closing the game window and stopping the game loop
	 */

	// 확실한지 모르겠음 - 수영
	private void returnToMainMenu() {
		gameRunning = false;
		container.setVisible(false);
		container.dispose();
	}


	/** Map a purchased itemId (from Firestore) to the UI slot index */
	private int matchItemIndexForId(String itemId) {
		if (itemId == null) return -1;
		String id = itemId.toLowerCase();
		// Heuristic mapping based on id keywords
		if (id.contains("ammo")) return 0;                      // item_ammo_boost.png
		if (id.contains("double") || id.contains("score")) return 1; // item_double_score.png
		if (id.contains("invinc") || id.contains("shield")) return 2; // item_invincibility.png
		if (id.contains("life")) return 3;                      // item_plusLife.png
		return -1;
	}

	/** Recompute item counts from Firestore purchases list */
	private void refreshItemCountsFromFirestore() {
		// reset counts
		for (int i = 0; i < itemUICounts.length; i++) itemUICounts[i] = 0;
		if (firebaseManager == null || !firebaseManager.isLoggedIn()) return;

		if (purchasedItems == null) {
			purchasedItems = new java.util.ArrayList<>();
		}
		// purchasedItems는 FirebaseManager.getPurchasedItems()에서 온 itemId 문자열 목록
		for (String itemId : purchasedItems) {
			int idx = matchItemIndexForId(itemId);
			if (idx >= 0 && idx < itemUICounts.length) {
				itemUICounts[idx]++;
			}
		}
	}




	/**
	 * The main game loop. This loop is running during all game
	 * play as is responsible for the following activities:
	 * <p>
	 * - Working out the speed of the game loop to update moves
	 * - Moving the game entities
	 * - Drawing the screen contents (entities, text)
	 * - Updating game events
	 * - Checking Input
	 * <p>
	 */
	public void gameLoop() {
		long lastLoopTime = SystemTimer.getTime();

		// keep looping round til the game ends
		while (gameRunning) {
			long delta = SystemTimer.getTime() - lastLoopTime;
			lastLoopTime = SystemTimer.getTime();

			updateFrameCounter(delta);
			updateGameEntities(delta);

			Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
			drawGame(g);

			handlePlayerInput();

			// we want each frame to take 10 milliseconds, to do this
			// we've recorded when we started the frame. We add 10 milliseconds
			// to this and then factor in the current time to give
			// us our final value to wait for
			SystemTimer.sleep(lastLoopTime + 10 - SystemTimer.getTime());
		}
	}

	/**
	 * Update FPS counter and handle enemy firing
	 */
	private void updateFrameCounter(long delta) {
		lastFpsTime += delta;
		fps++;

		if (lastFpsTime >= 1000) {
			container.setTitle(windowTitle + " (FPS: " + fps + ")");
			lastFpsTime = 0;
			fps = 0;
			if (!waitingForKeyPress && !stageSelectActive) {
				handleEnemyFiring();
			}
		}
	}

	/**
	 * Update and process all game entities
	 */
	private void updateGameEntities(long delta) {
		moveEntitiesIfActive(delta);
		checkEntityCollisions();
		removeDeadEntities();
		processEntityLogic();
	}

	private void moveEntitiesIfActive(long delta) {
		if (isGamePaused()) return;

		for (Entity entity : entities) {
			entity.move(delta);
		}
	}

	private boolean isGamePaused() {
		return waitingForKeyPress || pausePromptActive || stageSelectActive;
	}

	private void checkEntityCollisions() {
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

	private void removeDeadEntities() {
		entities.removeAll(removeList);
		removeList.clear();
	}

	private void processEntityLogic() {
		if (!logicRequiredThisLoop) return;

		for (Entity entity : entities) {
			entity.doLogic();
		}
		logicRequiredThisLoop = false;
	}

	/**
	 * Draw all game graphics
	 */
	private void drawGame(Graphics2D g) {
		// Clear screen
		g.setColor(Color.black);
		g.fillRect(0, 0, 1200, 900);

		// Draw all entities
		for (Entity entity : entities) {
			entity.draw(g);
		}

		// Draw HUD
		if (screenRenderer != null) {
			screenRenderer.drawHUD(g, stateManager.getCurrentStage(), score);
		}

		// Draw overlays using ScreenRenderer
		if (screenRenderer != null) {
			if (stageSelectActive) {
				screenRenderer.drawStageSelectScreen(g, selectedStage, maxClearedStage);
			} else if (pausePromptActive) {
				screenRenderer.drawPausePrompt(g, score);
			} else if (waitingForKeyPress) {
				screenRenderer.drawGameOverScreen(g, message, newHighScoreAchieved, finalScore);
			}
		}

		// Draw UI panels using renderers
		if (itemPanelRenderer != null) {
			itemPanelRenderer.drawLeftItemsPanel(g, getWidth(), getHeight());
		}
		if (hpBarRenderer != null) {
			boolean twoPlayer = SettingsManager.isTwoPlayerEnabled() && ship2 != null;
			hpBarRenderer.drawPlayerHPBars(g, getWidth(), getHeight(), twoPlayer,
				playerHealth, playerMaxHealth, player2Health, player2MaxHealth);
		}

		// Flip buffer
		g.dispose();
		strategy.show();
	}


	/**
	 * Handle player input during gameplay
	 */
	private void handlePlayerInput() {
		handlePlayer1Input();
		handlePlayer2Input();
	}

	/**
	 * Handle player 1 input (movement and firing)
	 */
	private void handlePlayer1Input() {
		if (!canPlayerControlShip(ship, playerHealth)) {
			return;
		}

		updateShipMovement(ship, leftPressed, rightPressed);

		if (firePressed) {
			tryToFireFrom(ship, 0);
		}
	}

	/**
	 * Handle player 2 input (movement and firing)
	 */
	private void handlePlayer2Input() {
		if (!canPlayerControlShip(ship2, player2Health)) {
			return;
		}

		updateShipMovement(ship2, leftPressed2, rightPressed2);

		if (firePressed2) {
			tryToFireFrom(ship2, 1);
		}
	}

	/**
	 * Check if player can control the ship
	 */
	private boolean canPlayerControlShip(ShipEntity ship, int health) {
		return ship != null && health > 0 && !waitingForKeyPress && !pausePromptActive && !stageSelectActive;
	}

	/**
	 * Update ship movement based on key presses
	 */
	private void updateShipMovement(ShipEntity ship, boolean leftPressed, boolean rightPressed) {
		ship.setHorizontalMovement(0);
		if (leftPressed && !rightPressed) {
			ship.setHorizontalMovement(-moveSpeed);
		} else if (rightPressed && !leftPressed) {
			ship.setHorizontalMovement(moveSpeed);
		}
	}

	/**
	 * Handle enemy firing with type-specific behaviors and difficulty scaling
	 */
	private void handleEnemyFiring() {
		long adjustedInterval = calculateEnemyFiringInterval();

		if (!canEnemiesFire(adjustedInterval)) {
			return;
		}

		java.util.List<AlienEntity> shooters = collectAliveAliens();
		if (shooters.isEmpty()) return;

		java.util.List<AlienEntity> selectedShooters = selectShooters(shooters);
		fireFromSelectedShooters(selectedShooters);

		enemyLastFire = System.currentTimeMillis();
	}

	private long calculateEnemyFiringInterval() {
		double stageDifficultyMultiplier = 1.0 - (stateManager.getCurrentStage() * 0.1);
		double alienCountMultiplier = Math.max(0.5, alienCount / 10.0);
		long adjustedInterval = (long) (enemyFiringInterval * stageDifficultyMultiplier * alienCountMultiplier);
		return Math.max(400, adjustedInterval);
	}

	private boolean canEnemiesFire(long adjustedInterval) {
		return System.currentTimeMillis() - enemyLastFire >= adjustedInterval;
	}

	private java.util.List<AlienEntity> collectAliveAliens() {
		java.util.List<AlienEntity> shooters = new java.util.ArrayList<>();
		for (int i = 0; i < entities.size(); i++) {
			Entity e = (Entity) entities.get(i);
			if (e instanceof AlienEntity) {
				shooters.add((AlienEntity) e);
			}
		}
		return shooters;
	}

	private java.util.List<AlienEntity> selectShooters(java.util.List<AlienEntity> shooters) {
		java.util.List<AlienEntity> selectedShooters = new java.util.ArrayList<>();
		for (AlienEntity alien : shooters) {
			if (Math.random() < (alien.getFiringProbability() / shooters.size())) {
				selectedShooters.add(alien);
			}
		}

		if (selectedShooters.isEmpty()) {
			selectedShooters.add(shooters.get((int) (Math.random() * shooters.size())));
		}
		return selectedShooters;
	}

	private void fireFromSelectedShooters(java.util.List<AlienEntity> selectedShooters) {
		for (AlienEntity shooter : selectedShooters) {
			fireAlienShots(shooter);
		}
	}

	/**
	 * Fire shots from a specific alien based on its type
	 */
	private void fireAlienShots (AlienEntity shooter){
		int shotCount = shooter.getShotCount();
		double spreadAngle = shooter.getShotSpreadAngle();
		double baseX = shooter.getX() + 10;
		double baseY = shooter.getY() + 20;

		if (shotCount == 1) {
			// Single shot straight down
			entities.add(new EnemyShotEntity((int) baseX, (int) baseY, 0, 250));
		} else {
			// Multi-shot with spread
			for (int i = 0; i < shotCount; i++) {
				// Calculate angle for this shot
				double angle = 0;
				if (shotCount > 1) {
					// Spread shots evenly across the spread angle
					double startAngle = -spreadAngle / 2;
					double angleStep = spreadAngle / (shotCount - 1);
					angle = startAngle + (angleStep * i);
				}

				// Calculate velocity components
				double speed = 250;
				double vx = speed * Math.sin(angle);
				double vy = speed * Math.cos(angle);

				entities.add(new EnemyShotEntity((int) baseX, (int) baseY, vx, vy));
			}
		}
	}

	/** Enemy shot that travels downward (or at an angle) and damages the player on hit */
	private class EnemyShotEntity extends Entity {
		private double vx; // horizontal velocity
		private double vy; // vertical velocity

		public EnemyShotEntity(int x, int y, double vx, double vy) {
			super("sprites/shot.gif", x, y);
			this.vx = vx;
			this.vy = vy;
			setHorizontalMovement(vx);
			setVerticalMovement(vy);
		}

		@Override
		public void move(long delta) {
			super.move(delta);
			// Remove if off screen
			if (getY() > 900 || getX() < 0 || getX() > 1200) {
				removeList.add(this);
			}
		}

		@Override
		public void collidedWith(Entity other) {
			// =================================================================
			// === 2P FEATURE: Modified to call the new player hit method ===
			// =================================================================
			if (other instanceof ShipEntity) {
				removeList.add(this);
				notifyPlayerHit((ShipEntity) other, 1);
			}
		}
	}


	/**
	 * A class to handle keyboard input from the user. The class
	 * handles both dynamic input during game play, i.e. left/right
	 * and shoot, and more static type input (i.e. press any key to
	 * continue)
	 *
	 * This has been implemented as an inner class more through
	 * habbit then anything else. Its perfectly normal to implement
	 * this as seperate class if slight less convienient.
	 *
	 * @author Kevin Glass
	 */
	private class KeyInputHandler extends KeyAdapter {
	    private int pressCount = 1;

	    @Override
	    public void keyPressed(KeyEvent e) {
	        int keyCode = e.getKeyCode();

	        if (stageSelectActive) {
	            handleStageSelection(keyCode);
	            return;
	        }

	        // Pause keys should be handled even when paused
	        if (handlePauseKeys(keyCode)) return;

	        if (waitingForKeyPress || pausePromptActive) return;

	        handlePlayerControls(keyCode);
	        handleItemKeys(keyCode);
	    }

	    private void handleStageSelection(int keyCode) {
	        switch (keyCode) {
	            case KeyEvent.VK_LEFT -> selectedStage = Math.max(1, selectedStage - 1);
	            case KeyEvent.VK_RIGHT -> selectedStage = Math.min(5, maxClearedStage + 1);
	            case KeyEvent.VK_ENTER -> {
	                stateManager.setCurrentStage(selectedStage);
	                stageSelectActive = false;
	                enemyLastFire = SystemTimer.getTime();
	                startGame();
	            }
	            case KeyEvent.VK_ESCAPE -> {
	                stageSelectActive = false;
	                returnToMainMenu();
	            }
	            default -> {
	                // No action for other keys
	            }
	        }
	    }

	    private boolean handlePauseKeys(int keyCode) {
	        if (keyCode == KeyEvent.VK_ESCAPE && !waitingForKeyPress) {
	            if (!pausePromptActive) pausePromptActive = true;
	            else {
	                awardCurrentScoreAsPoints();
	                returnToMainMenu();
	            }
	            return true;
	        }
	        if (keyCode == KeyEvent.VK_SPACE && pausePromptActive) {
	            pausePromptActive = false;
	            return true;
	        }
	        return false;
	    }

	    private void handlePlayerControls(int keyCode) {
	        switch (keyCode) {
	            // Player 1
	            case KeyEvent.VK_LEFT -> leftPressed = true;
	            case KeyEvent.VK_RIGHT -> rightPressed = true;
	            case KeyEvent.VK_SPACE -> firePressed = true;
	            // Player 2
	            case KeyEvent.VK_A -> leftPressed2 = true;
	            case KeyEvent.VK_D -> rightPressed2 = true;
	            case KeyEvent.VK_W -> firePressed2 = true;
	            default -> {
	                // No action for other keys
	            }
	        }
	    }

	    private void handleItemKeys(int keyCode) {
	        if (itemManager == null) return;
	        String itemId = switch (keyCode) {
	            case KeyEvent.VK_1 -> ItemManager.ID_AMMO;
	            case KeyEvent.VK_2 -> ItemManager.ID_DOUBLE_SCORE;
	            case KeyEvent.VK_3 -> ItemManager.ID_INVINCIBILITY;
	            case KeyEvent.VK_4 -> ItemManager.ID_PLUS_LIFE;
	            default -> null;
	        };
	        if (itemId != null) {
	            ItemManager.Effect eff = itemManager.use(itemId);
	            syncItemCountsFromManager();
	            if (eff == ItemManager.Effect.PLUS_LIFE) applyHealthBoost();
	        }
	    }

	    private void applyHealthBoost() {
	        if (playerHealth > 0)
	            playerHealth = Math.min(playerMaxHealth, playerHealth + 1);
	        if (SettingsManager.isTwoPlayerEnabled() && player2Health > 0)
	            player2Health = Math.min(player2MaxHealth, player2Health + 1);
	    }

	    @Override
	    public void keyReleased(KeyEvent e) {
	        int keyCode = e.getKeyCode();
	        if (waitingForKeyPress || pausePromptActive) return;

	        switch (keyCode) {
	            // Player 1
	            case KeyEvent.VK_LEFT -> leftPressed = false;
	            case KeyEvent.VK_RIGHT -> rightPressed = false;
	            case KeyEvent.VK_SPACE -> firePressed = false;
	            // Player 2
	            case KeyEvent.VK_A -> leftPressed2 = false;
	            case KeyEvent.VK_D -> rightPressed2 = false;
	            case KeyEvent.VK_W -> firePressed2 = false;
	            default -> {
	                // No action for other keys
	            }
	        }
	    }

	    @Override
	    public void keyTyped(KeyEvent e) {
	        char keyChar = Character.toLowerCase(e.getKeyChar());
	        if (waitingForKeyPress && "wasd ".indexOf(keyChar) == -1) {
	            if (pressCount == 1) {
	                waitingForKeyPress = false;
	                enemyLastFire = System.currentTimeMillis();
	                startGame();
	                pressCount = 0;
	            } else pressCount++;
	        }

	        if (waitingForKeyPress && e.getKeyChar() == 27) {
	            returnToMainMenu();
	        }
	    }
	}




	/**
	 * The entry point into the game. We'll simply create an
	 * instance of class which will start the display and game
	 * loop.
	 *
	 * @param argv The arguments that are passed into our game
	 */
	public static void main(String argv[]) {
		// Enable DPI scaling support for high-resolution displays
		// This makes the window appear at the correct size on high-DPI monitors
		System.setProperty("sun.java2d.uiScale", "1.0");

		Game g = new Game();

		// Start the main game loop, note: this method will not
		// return until the game has finished running. Hence we are
		// using the actual main thread to run the game.
		g.gameLoop();
	}


	/** Copy counts from ItemManager into itemUICounts for left panel drawing */
	private void syncItemCountsFromManager() {
		if (itemManager == null) return;
		int[] arr = itemManager.getCountsArray();
		if (arr != null && arr.length == itemUICounts.length) {
			for (int i = 0; i < itemUICounts.length; i++) itemUICounts[i] = arr[i];
		}
		updateItemPanelRenderer();
	}

	/** Update ItemPanelRenderer with current state */
	private void updateItemPanelRenderer() {
		boolean isLoggedIn = (firebaseManager != null && firebaseManager.isLoggedIn());
		itemPanelRenderer = new ItemPanelRenderer(itemUIList, itemUICounts, itemUIIcons, isLoggedIn);
	}
}