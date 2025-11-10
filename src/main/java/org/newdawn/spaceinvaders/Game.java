package org.newdawn.spaceinvaders;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.awt.Image;
import javax.swing.ImageIcon;
import java.net.URL;
import java.awt.Toolkit;
import java.awt.Font;

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
	private int playerMaxHealth = 3;
	private int playerHealth = playerMaxHealth;
	// =================================================================
	// === 2P FEATURE: Added separate health for the second player ===
	// =================================================================
	private int player2MaxHealth = 3;
	private int player2Health = player2MaxHealth;
	/** Enemy firing control */
	private long enemyLastFire = 0;
	private long enemyFiringInterval = 1200; // ms
	/** The number of aliens left on the screen */
	private int alienCount;
	/** Current stage/level (1-5) */
	private int currentStage = 1;
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

		// create a frame to contain our game
		container = new JFrame("Space Invaders 102");

		// get hold the content of the frame and set up the resolution of the game
		JPanel panel = (JPanel) container.getContentPane();
		panel.setPreferredSize(new Dimension(800,600));
		panel.setLayout(null);

		// setup our canvas size and put it into the content of the frame
		setBounds(0,0,800,600);
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
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});

		// load item icons from resources
		loadItemUIIcons();


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
		pausePromptActive = false;

		// 게임 시작 시 영구 업그레이드 적용
		applyPermanentUpgrades();

		if (firebaseManager != null && firebaseManager.isLoggedIn()) {
			purchasedItems = firebaseManager.getPurchasedItems();
			if (itemManager == null) itemManager = new ItemManager(firebaseManager);
			itemManager.setCountsFromPurchased(purchasedItems);
			syncItemCountsFromManager(); // UI 표시용 배열을 갱신
		}
		// refresh purchased items & counts from Firestore at stage start
		if (firebaseManager != null && firebaseManager.isLoggedIn()) {
			purchasedItems = firebaseManager.getPurchasedItems();
			if (itemManager == null) itemManager = new ItemManager(firebaseManager);
			itemManager.setCountsFromPurchased(purchasedItems);
			syncItemCountsFromManager();
		}
		// Don't reset stage/score here - they're set in notifyDeath() or notifyWin()
		// This method just initializes a new level

		// clear out any existing entities and intialise a new set
		isTwoPlayerGame = SettingsManager.isTwoPlayerEnabled();


		entities.clear();
		initEntities();

		// blank out any keyboard settings we might currently have
		leftPressed= false;
		rightPressed = false;
		firePressed = false;

		//2p기능
		leftPressed2 = false;
		rightPressed2 = false;
		firePressed2 = false;

	}

	/**
	 * Apply permanent upgrades from Firestore to game stats
	 */
	private void applyPermanentUpgrades() {
		if (firebaseManager == null || !firebaseManager.isLoggedIn()) {
			// Not logged in - use default values
			moveSpeed = 300;
			firingInterval = 500;
			playerMaxHealth = 3;
			playerHealth = playerMaxHealth;
			// =================================================================
			// === 2P FEATURE: Set default health for P2 as well ===
			// =================================================================
			player2MaxHealth = 3;
			player2Health = player2MaxHealth;
			return;
		}

		// Get upgrade levels from Firestore
		int attackLevel = firebaseManager.getUpgradeLevel("attack");
		int healthLevel = firebaseManager.getUpgradeLevel("health");
		int speedLevel = firebaseManager.getUpgradeLevel("speed");

		// Apply attack upgrade (연사속도 증가)
		// Each level: -15% cooldown (faster shooting)
		firingInterval = (long)(500 * Math.pow(0.85, attackLevel));

		// Apply health upgrade (최대 HP 증가)
		// Each level: +1 HP
		playerMaxHealth = 3 + healthLevel;
		playerHealth = playerMaxHealth;

		// =================================================================
		// === 2P FEATURE: Apply health upgrade to P2 if active ===
		// =================================================================
		if (SettingsManager.isTwoPlayerEnabled()) {
			player2MaxHealth = 3 + healthLevel;
			player2Health = player2MaxHealth;
		}


		// Apply speed upgrade (이동속도 증가)
		// Each level: +12% movement speed
		moveSpeed = 300 * Math.pow(1.12, speedLevel);

		System.out.println("[Permanent Upgrades Applied]");
		System.out.println("  Attack Level " + attackLevel + ": Fire Interval = " + firingInterval + "ms");
		System.out.println("  Health Level " + healthLevel + ": Max HP = " + playerMaxHealth);
		System.out.println("  Speed Level " + speedLevel + ": Move Speed = " + moveSpeed);
	}

	/**
	 * Initialise the starting state of the entities (ship and aliens). Each
	 * entitiy will be added to the overall list of entities in the game.
	 */
	private void initEntities() {
		// 1P
		ship = new ShipEntity(this,"sprites/ship.gif",370,550);
		entities.add(ship);

// SettingsManager에서 2P 모드가 활성화되어 있는지 확인합니다.
		boolean twoPlayerEnabled = SettingsManager.isTwoPlayerEnabled();

		// 2P(옵션)
		if (twoPlayerEnabled) {
			// 1P와 약간 떨어뜨려 배치
			ship2 = new ShipEntity(this,"sprites/ship.gif",370 + 80, 550);
			entities.add(ship2);
		} else {
			ship2 = null; // 안전
		}

		// 적 생성
		initAliensForStage(currentStage);
	}

	/**
	 * Initialize aliens based on the current stage
	 * @param stage The current stage (1-5)
	 */
	private void initAliensForStage(int stage) {
		alienCount = 0;

		switch(stage) {
			case 1:
				// Stage 1: Basic formation - 1 BASIC alien for testing
				createAlienFormation(1, 1, 350, 100, 50, 30, "normal");
				break;
			case 2:
				// Stage 2: 1 BASIC + 1 FAST alien for testing
				createAlienFormation(1, 2, 300, 100, 100, 30, "normal");
				break;
			case 3:
				// Stage 3: 3 different types for testing
				createAlienFormation(2, 2, 250, 80, 150, 40, "zigzag");
				break;
			case 4:
				// Stage 4: All 4 types for testing (includes SPECIAL teleport)
				createAlienFormation(2, 2, 250, 80, 150, 40, "wave");
				break;
			case 5:
				// Stage 5: 2 random aliens + boss for testing
				createAlienFormation(1, 2, 200, 120, 200, 35, "normal");
				// Boss will be added separately
				createBossAlien();
				break;
			default:
				// Default to Stage 1 formation for any unexpected stage value
				createAlienFormation(1, 1, 350, 100, 50, 30, "normal");
				break;
		}
	}

	/**
	 * Create a formation of aliens
	 * @param rows Number of rows
	 * @param cols Number of columns
	 * @param startX Starting X position
	 * @param startY Starting Y position
	 * @param spacingX Horizontal spacing
	 * @param spacingY Vertical spacing
	 * @param movementType Movement pattern type (deprecated, now determined by alien type)
	 */
	private void createAlienFormation(int rows, int cols, int startX, int startY, int spacingX, int spacingY, String movementType) {
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				// Use AlienFactory to create aliens (Factory Pattern - OCP compliance)
				AlienEntity alien = alienFactory.createAlien(
					currentStage,
					row,
					col,
					startX + (col * spacingX),
					startY + (row * spacingY)
				);
				entities.add(alien);
				alienCount++;
			}
		}
	}

	/**
	 * Determine alien type based on stage, row, and column
	 * @param stage Current stage
	 * @param row Row position
	 * @param col Column position
	 * @return AlienType to create
	 */
	private AlienEntity.AlienType determineAlienType(int stage, int row, int col) {
		switch (stage) {
			case 1:
				return getStage1AlienType();
			case 2:
				return getStage2AlienType(row);
			case 3:
				return getStage3AlienType(row);
			case 4:
				return getStage4AlienType(row, col);
			case 5:
				return getStage5AlienType();
			default:
				return AlienEntity.AlienType.BASIC;
		}
	}

	private AlienEntity.AlienType getStage1AlienType() {
		return AlienEntity.AlienType.BASIC;
	}

	private AlienEntity.AlienType getStage2AlienType(int row) {
		if (row == 0) {
			return AlienEntity.AlienType.FAST; // Fast aliens in front row
		}
		return AlienEntity.AlienType.BASIC;
	}

	private AlienEntity.AlienType getStage3AlienType(int row) {
		if (row == 0) {
			return AlienEntity.AlienType.FAST;
		} else if (row == 1) {
			return AlienEntity.AlienType.HEAVY;
		}
		return AlienEntity.AlienType.BASIC;
	}

	private AlienEntity.AlienType getStage4AlienType(int row, int col) {
		if (row == 0) {
			return (col % 2 == 0) ? AlienEntity.AlienType.FAST : AlienEntity.AlienType.SPECIAL;
		} else if (row == 1) {
			return AlienEntity.AlienType.HEAVY;
		}
		return AlienEntity.AlienType.BASIC;
	}

	private AlienEntity.AlienType getStage5AlienType() {
		double random = Math.random();
		if (random < 0.3) return AlienEntity.AlienType.FAST;
		else if (random < 0.6) return AlienEntity.AlienType.HEAVY;
		else if (random < 0.8) return AlienEntity.AlienType.SPECIAL;
		return AlienEntity.AlienType.BASIC;
	}

	/**
	 * Create boss alien for stage 5
	 */
	private void createBossAlien() {
		// Use AlienFactory to create boss (Factory Pattern - OCP compliance)
		AlienEntity boss = alienFactory.createBoss(350, 100, currentStage);
		entities.add(boss);
		alienCount++;
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

	// public void notifyDeath() {
	// 	pausePromptActive = false;

	// 	// 1. 최고 점수 등극 메시지 출력을 위해 score 초기화 전에 finalScore에 저장
	// 	finalScore = score;

	// 	// 2. 게임 종료 시 점수를 포인트로 저장 및 최고 점수 갱신
	// 	saveScoreAsPoints();

	// 	// 💡 [핵심 추가] 현재 플레이 중이던 스테이지를 maxClearedStage로 저장
	// 	if (currentStage > maxClearedStage) {
	// 		maxClearedStage = currentStage;
	// 		if (firebaseManager != null && firebaseManager.isLoggedIn()) {
	// 			// ✅ 플레이 중이던 스테이지를 최고 기록으로 저장 (사망했더라도 진행 기록 유지)
	// 			firebaseManager.saveMaxClearedStage(maxClearedStage-1);
	// 			System.out.println("DEATH: Saved progress up to Stage " + maxClearedStage);
	// 		}
	// 	}

	// 	// 3. 사망 후 Stage Select 화면으로 전환
	// 	message = "Oh no! They got you, try again?";
	// 	waitingForKeyPress = false; // 일반 대기 상태 비활성화
	// 	stageSelectActive = true;

	// 	// 사망 후 커서 위치는 마지막으로 플레이했던 스테이지에 위치
	// 	selectedStage = maxClearedStage > 0 ? maxClearedStage : 1;

	// 	// 4. 점수/체력 초기화
	// 	score = 0;
	// 	playerHealth = playerMaxHealth;
	// }

	// Game.java: notifyDeath() 메서드 전체 (수정)
	public void notifyDeath() {
		pausePromptActive = false;

		// 1. 최고 점수 등극 메시지 출력을 위해 score 초기화 전에 finalScore에 저장
		finalScore = score;

		// 2. 게임 종료 시 점수를 포인트로 저장 및 최고 점수 갱신
		saveScoreAsPoints();

		// 💡 [핵심 수정] maxClearedStage 변수는 건드리지 않고, Firebase에 저장만 시도합니다.
		// 현재 플레이 중인 스테이지(currentStage)가 maxClearedStage보다 높을 경우에만 저장 시도
		if (currentStage > maxClearedStage) {
			if (firebaseManager != null && firebaseManager.isLoggedIn()) {
				// ✅ 성공적으로 깬 마지막 스테이지 (현재 진행 중인 스테이지의 직전)를 저장
				//    Stage 3에서 죽었다면 (3-1=2) Stage 2를 저장
				firebaseManager.saveMaxClearedStage(currentStage - 1);
				System.out.println("DEATH: Saved *previous* stage " + (currentStage - 1) + " as max.");
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
		return currentStage >= 5;
	}

	private void handleFinalStageCompletion() {
		saveScoreAsPoints();
		message = "Congratulations! All stages completed! Final Score: " + finalScore;
		waitingForKeyPress = true;
		score = 0;
		currentStage = 1;
	}

	private void handleIntermediateStageCompletion() {
		updateMaxClearedStage();
		awardStageBonus();
		prepareStageSelection();
		clearGameEntities();
	}

	private void updateMaxClearedStage() {
		if (currentStage > maxClearedStage) {
			maxClearedStage = currentStage;
			if (firebaseManager != null && firebaseManager.isLoggedIn()) {
				firebaseManager.saveMaxClearedStage(maxClearedStage);
			}
		}
	}

	private void awardStageBonus() {
		int stageBonus = currentStage * 100;
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

			System.out.println("Score: " + score + " saved as points. Total Points: " + newPoints);
			if (newHigh) {
				System.out.println("🎉 NEW HIGH SCORE ACHIEVED: " + score);
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
		score += (int)Math.round(alienScore * currentStage * mult);

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
				double speedIncrease = 1.02 + (currentStage * 0.005);
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
		score += (int)Math.round(alienKillPoints * currentStage * 10 * mult);
		alienCount--;

		if (alienCount == 0) {
			notifyWin();
		}
	}


	// 특정 플레이어로부터 사격시도
	private void tryToFireFrom(Entity shooter, int index) {
		long now = System.currentTimeMillis();
		if (now - fireStamps[index] < firingInterval) return;
		fireStamps[index] = now;


		ShotEntity shot = new ShotEntity(this, "sprites/shot.gif", shooter.getX() + 10, shooter.getY() - 30);
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

	/** Try multiple classpath variants to load an image resource; logs if not found */
	private Image loadImageResource(String... candidates) {
		for (String raw : candidates) {
			if (raw == null || raw.isEmpty()) continue;
			String[] probes = new String[] { raw, "/" + raw };
			for (String p : probes) {
				URL url = Game.class.getResource(p);
				if (url == null) {
					url = Thread.currentThread().getContextClassLoader().getResource(p.startsWith("/") ? p.substring(1) : p);
				}
				if (url != null) {
					return new ImageIcon(url).getImage();
				}
			}
		}
		System.out.println("[WARN] Image resource not found: " + java.util.Arrays.toString(candidates));
		return null;
	}

	/** Load item icons from resources matching itemUIList order */
	private void loadItemUIIcons() {
		itemUIIcons.clear();
		for (String name : itemUIList) {
			String baseFilename = resolveItemIconFilename(name);
			Image img = loadItemIconImage(baseFilename);
			itemUIIcons.add(img);
		}
	}

	private String resolveItemIconFilename(String name) {
		String lower = name == null ? "" : name.toLowerCase();
		if (isImageFilename(lower)) {
			return name;
		}
		return mapNameToIconFilename(lower);
	}

	private boolean isImageFilename(String lower) {
		return lower.endsWith(".png") || lower.endsWith(".gif") ||
		       lower.endsWith(".jpg") || lower.endsWith(".jpeg");
	}

	private String mapNameToIconFilename(String lower) {
		switch (lower) {
			case "ammo":          return "item_ammo_boost.png";
			case "score":         return "item_double_score.png";
			case "invincibility": return "item_invincibility.png";
			case "life":          return "item_plusLife.png";
			default:              return "item_unknown.png";
		}
	}

	private Image loadItemIconImage(String base) {
		return loadImageResource(
				"sprites/" + base,
				"org/newdawn/spaceinvaders/sprites/" + base,
				"resources/sprites/" + base
		);
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

	/** Draw a vertical items UI along the far-left edge */
	private void drawLeftItemsPanel(Graphics2D g2) {
		int rows = (itemUIList != null) ? itemUIList.size() : 0;
		if (rows <= 0) return;

		ItemPanelLayout layout = calculateItemPanelLayout(rows);
		drawItemPanelBackground(g2, layout);
		drawItemSlots(g2, layout);
	}

	/**
	 * Calculate layout metrics for item panel
	 */
	private ItemPanelLayout calculateItemPanelLayout(int rows) {
		int canvasW = this.getWidth();
		int canvasH = this.getHeight();
		int pad = 8, gap = 6, startY = 70, innerPad = 6;

		int[] drawWArr = new int[rows];
		int[] drawHArr = new int[rows];
		int[] slotWArr = new int[rows];
		int[] slotHArr = new int[rows];

		int maxPanelW = 0;
		int y = startY;

		for (int i = 0; i < rows; i++) {
			Image icon = (i < itemUIIcons.size()) ? itemUIIcons.get(i) : null;
			int[] sizes = calculateIconSize(icon, innerPad);

			drawWArr[i] = sizes[0];
			drawHArr[i] = sizes[1];
			slotWArr[i] = sizes[2];
			slotHArr[i] = sizes[3];

			maxPanelW = Math.max(maxPanelW, sizes[2]);
			y += sizes[3] + gap;
		}

		int totalPanelH = y - startY - gap + pad;
		if (startY + totalPanelH > canvasH - pad) {
			totalPanelH = Math.max(0, (canvasH - pad) - startY);
		}

		return new ItemPanelLayout(pad, gap, startY, innerPad, maxPanelW, totalPanelH,
		                           drawWArr, drawHArr, slotWArr, slotHArr, canvasH);
	}

	/**
	 * Calculate icon and slot sizes
	 * @return [drawW, drawH, slotW, slotH]
	 */
	private int[] calculateIconSize(Image icon, int innerPad) {
		int baseMaxW = 48, baseMaxH = 48;
		int imgW = (icon != null) ? icon.getWidth(null) : baseMaxW;
		int imgH = (icon != null) ? icon.getHeight(null) : baseMaxH;
		if (imgW <= 0 || imgH <= 0) { imgW = baseMaxW; imgH = baseMaxH; }

		double fitScale = Math.min((double) baseMaxW / imgW, (double) baseMaxH / imgH);
		int fitW = (int) Math.max(1, Math.round(imgW * fitScale));
		int fitH = (int) Math.max(1, Math.round(imgH * fitScale));
		int drawW = (int) Math.max(1, Math.round(fitW * (2.0 / 3.0)));
		int drawH = (int) Math.max(1, Math.round(fitH * (2.0 / 3.0)));

		int slotW = Math.max(drawW + innerPad * 2, 28);
		int slotH = Math.max(drawH + innerPad * 2, 28);

		return new int[]{drawW, drawH, slotW, slotH};
	}

	/**
	 * Draw panel background
	 */
	private void drawItemPanelBackground(Graphics2D g2, ItemPanelLayout layout) {
		g2.setColor(new Color(20, 20, 20, 150));
		g2.fillRect(layout.pad - 2, layout.startY - 2, layout.maxPanelW + 4, layout.totalPanelH + 4);
	}

	/**
	 * Draw all item slots
	 */
	private void drawItemSlots(Graphics2D g2, ItemPanelLayout layout) {
		int rowY = layout.startY;
		int rows = layout.slotWArr.length;

		for (int i = 0; i < rows; i++) {
			if (rowY + layout.slotHArr[i] > layout.canvasH - layout.pad) break;

			drawSingleItemSlot(g2, layout, i, rowY);
			rowY += layout.slotHArr[i] + layout.gap;
		}
	}

	/**
	 * Draw a single item slot
	 */
	private void drawSingleItemSlot(Graphics2D g2, ItemPanelLayout layout, int index, int rowY) {
		int slotW = layout.slotWArr[index];
		int slotH = layout.slotHArr[index];
		int drawW = layout.drawWArr[index];
		int drawH = layout.drawHArr[index];

		// Slot background
		g2.setColor(new Color(45, 45, 45));
		g2.fillRect(layout.pad, rowY, slotW, slotH);
		g2.setColor(Color.WHITE);
		g2.drawRect(layout.pad, rowY, slotW, slotH);

		// Draw icon or placeholder
		Image icon = (index < itemUIIcons.size()) ? itemUIIcons.get(index) : null;
		int dx = layout.pad + (slotW - drawW) / 2;
		int dy = rowY + (slotH - drawH) / 2;

		if (icon != null) {
			g2.drawImage(icon, dx, dy, drawW, drawH, null);
		} else {
			drawIconPlaceholder(g2, layout, slotW, slotH, rowY);
		}

		// Draw count badge
		drawItemCountBadge(g2, index, dx, dy, drawW, drawH);
	}

	/**
	 * Draw placeholder when icon is missing
	 */
	private void drawIconPlaceholder(Graphics2D g2, ItemPanelLayout layout, int slotW, int slotH, int rowY) {
		g2.setColor(new Color(80, 80, 80));
		g2.fillRect(layout.pad + layout.innerPad, rowY + layout.innerPad,
		            slotW - layout.innerPad * 2, slotH - layout.innerPad * 2);
		g2.setColor(Color.WHITE);
		g2.drawString("?", layout.pad + slotW / 2 - 3, rowY + slotH / 2 + 4);
	}

	/**
	 * Draw item count badge
	 */
	private void drawItemCountBadge(Graphics2D g2, int index, int dx, int dy, int drawW, int drawH) {
		int count = (itemUICounts != null && index < itemUICounts.length) ? itemUICounts[index] : 0;
		if (firebaseManager == null || !firebaseManager.isLoggedIn()) {
			count = 0;
		}

		String label = "x" + count;
		FontMetrics fm = g2.getFontMetrics();
		int bw = fm.stringWidth(label) + 10;
		int bh = fm.getAscent() + fm.getDescent();
		int bx = dx + drawW - bw - 2;
		int by = dy + drawH - bh - 2;

		g2.setColor(new Color(0, 0, 0, 190));
		g2.fillRoundRect(bx, by, bw, bh, 8, 8);
		g2.setColor(Color.WHITE);
		g2.drawString(label, bx + 5, by + fm.getAscent());
	}

	/**
	 * Layout data for item panel
	 */
	private static class ItemPanelLayout {
		final int pad, gap, startY, innerPad, maxPanelW, totalPanelH, canvasH;
		final int[] drawWArr, drawHArr, slotWArr, slotHArr;

		ItemPanelLayout(int pad, int gap, int startY, int innerPad, int maxPanelW, int totalPanelH,
		                int[] drawWArr, int[] drawHArr, int[] slotWArr, int[] slotHArr, int canvasH) {
			this.pad = pad;
			this.gap = gap;
			this.startY = startY;
			this.innerPad = innerPad;
			this.maxPanelW = maxPanelW;
			this.totalPanelH = totalPanelH;
			this.drawWArr = drawWArr;
			this.drawHArr = drawHArr;
			this.slotWArr = slotWArr;
			this.slotHArr = slotHArr;
			this.canvasH = canvasH;
		}
	}

	// =================================================================================================
	// === 2P FEATURE: REPLACED drawPlayerHPBar with drawPlayerHPBars and a helper method ===
	// This new function checks if 2P mode is active. If so, it draws two separate, labeled HP bars.
	// Otherwise, it draws the original single, centered HP bar.
	// =================================================================================================
	/**
	 * Draws HP bars for all active players.
	 */
	private void drawPlayerHPBars(Graphics2D g2) {
		boolean twoPlayer = SettingsManager.isTwoPlayerEnabled() && ship2 != null;

		if (twoPlayer) {
			// Draw P1's HP Bar on the bottom-left
			drawSingleHPBar(g2, "P1", playerHealth, playerMaxHealth, "left");
			// Draw P2's HP Bar on the bottom-right
			drawSingleHPBar(g2, "P2", player2Health, player2MaxHealth, "right");
		} else {
			// Default 1P behavior: a single bar in the center
			drawSingleHPBar(g2, null, playerHealth, playerMaxHealth, "center");
		}
	}

	/**
	 * Helper method to draw a single segmented HP bar.
	 * @param g2 The graphics context
	 * @param label The label for the bar (e.g., "P1") or null for none
	 * @param currentHP The current health points
	 * @param maxHP The maximum health points
	 * @param position Where to draw the bar ("left", "right", or "center")
	 */
	private void drawSingleHPBar(Graphics2D g2, String label, int currentHP, int maxHP, String position) {
		int canvasW = this.getWidth();
		int canvasH = this.getHeight();

		int segments = Math.max(1, maxHP);
		int segWidth = 30;   // Width of each HP segment
		int segHeight = 8;   // Height of each HP segment
		int gap = 6;         // Gap between segments

		int totalW = segments * segWidth + (segments - 1) * gap;
		int y0 = canvasH - 28; // Position from the bottom edge
		int x0;

		// Determine horizontal position based on the 'position' parameter
		switch (position) {
			case "left":
				x0 = 40; // Margin from the left edge
				break;
			case "right":
				x0 = canvasW - totalW - 40; // Margin from the right edge
				break;
			default: // "center"
				x0 = (canvasW - totalW) / 2;
				break;
		}

		// Draw the player label (e.g., "P1") above the bar if provided
		if (label != null) {
			g2.setColor(Color.WHITE);
			g2.setFont(new Font("Arial", Font.BOLD, 14));
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(label, x0, y0 - fm.getHeight() / 2);
		}

		// Draw each segment of the HP bar
		for (int i = 0; i < segments; i++) {
			int x = x0 + i * (segWidth + gap);
			int y = y0;

			g2.setColor(new Color(20, 20, 20, 180));
			g2.fillRect(x - 2, y - 2, segWidth + 4, segHeight + 4);

			g2.setColor(Color.DARK_GRAY); // Background for an empty segment
			g2.fillRect(x, y, segWidth, segHeight);

			if (i < currentHP) {
				g2.setColor(Color.GREEN); // Fill for a full health segment
				g2.fillRect(x, y, segWidth, segHeight);
			}

			g2.setColor(Color.WHITE); // Border for the segment
			g2.drawRect(x, y, segWidth, segHeight);
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
		g.fillRect(0, 0, 800, 600);

		// Draw all entities
		for (Entity entity : entities) {
			entity.draw(g);
		}

		// Draw HUD
		drawHUD(g);

		// Draw overlays
		if (stageSelectActive) {
			drawStageSelectScreen(g);
		} else if (pausePromptActive) {
			drawPausePrompt(g);
		} else if (waitingForKeyPress) {
			drawGameOverScreen(g);
		}

		// Draw UI panels
		drawLeftItemsPanel(g);
		drawPlayerHPBars(g);

		// Flip buffer
		g.dispose();
		strategy.show();
	}

	/**
	 * Draw HUD (score, stage)
	 */
	private void drawHUD(Graphics2D g) {
		g.setColor(Color.white);
		g.drawString("Stage: " + currentStage, 10, 30);
		g.drawString("Score: " + score, 10, 50);
	}

	/**
	 * Draw stage selection screen
	 */
	private void drawStageSelectScreen(Graphics2D g) {
		// 1. 배경 어둡게 처리
		g.setColor(new Color(0, 0, 0, 200));
		g.fillRect(0, 0, 800, 600);

		// 2. 제목 그리기
		String title = "SELECT NEXT STAGE";
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.BOLD, 36));
		FontMetrics fmTitle = g.getFontMetrics();
		g.drawString(title, (800 - fmTitle.stringWidth(title)) / 2, 100);

		// 3. 스테이지 버튼 그리기 (1단계 ~ 5단계)
		int btnSize = 60;
		int gap = 20;
		int totalStages = 5;
		int totalW = totalStages * btnSize + (totalStages - 1) * gap;
		int startX = (800 - totalW) / 2;
		int startY = 200;

		for (int stage = 1; stage <= totalStages; stage++) {
			int x = startX + (stage - 1) * (btnSize + gap);

			// 선택된 스테이지에 따라 색상 변경
			if (stage == selectedStage) {
				// 1. 선택된 스테이지: 노란색
				g.setColor(Color.YELLOW);
			} else if (stage <= maxClearedStage || stage == maxClearedStage + 1) {
				// 2. 클리어했거나, 현재 선택 가능한 스테이지 (하늘색 -> 초록색으로 통일)
				//    stage <= currentStage: 이미 클리어한 스테이지
				//    stage == currentStage + 1: 현재 클리어 가능한 다음 스테이지
				g.setColor(Color.GREEN);
			} else {
				// 3. 잠긴 스테이지: 회색
				g.setColor(Color.LIGHT_GRAY);
			}

			// 버튼 사각형
			g.fillRect(x, startY, btnSize, btnSize);
			g.setColor(Color.BLACK);
			g.drawRect(x, startY, btnSize, btnSize);

			// 버튼 텍스트 (스테이지 번호)
			String stageNum = String.valueOf(stage);
			g.setColor(Color.BLACK);
			g.setFont(new Font("Arial", Font.BOLD, 24));
			FontMetrics fmBtn = g.getFontMetrics();
			g.drawString(stageNum, x + (btnSize - fmBtn.stringWidth(stageNum)) / 2, startY + fmBtn.getAscent() + 10);

			// "Hard" 또는 잠금 상태 표시 (선택 사항)
			// 💡 [필수 수정] 잠금 조건도 maxClearedStage 기준으로 변경
			if (stage > maxClearedStage + 1) {
				g.setColor(new Color(0, 0, 0, 150));
				g.fillRect(x, startY, btnSize, btnSize);
				g.setColor(Color.RED);
				g.drawString("LOCK", x + 5, startY + 40);
			}

		}

		// 안내 메시지
		String info = "Use Left/Right Arrows to select, Enter to start.";
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.PLAIN, 18));
		FontMetrics fmInfo = g.getFontMetrics();
		g.drawString(info, (800 - fmInfo.stringWidth(info)) / 2, 500);

		// 폰트와 색상 복구 (안전성)
		g.setColor(Color.white);
		g.setFont(new Font("Arial", Font.PLAIN, 12));
	}

	/**
	 * Draw pause prompt overlay
	 */
	private void drawPausePrompt(Graphics2D g) {
		// dim background
		g.setColor(new Color(0, 0, 0, 160));
		g.fillRect(0, 0, 800, 600);
		g.setColor(Color.white);
		String pts = String.format("%03d", Math.max(0, score));
		String l1 = "여기서 멈춘다면 " + pts + " 포인트를 얻습니다.";
		String l2 = "메인메뉴로 나가려면 ESC, 계속 플레이하려면 SPACE를 누르십시오.";
		FontMetrics fm = g.getFontMetrics();
		g.drawString(l1, (800 - fm.stringWidth(l1)) / 2, 260);
		g.drawString(l2, (800 - fm.stringWidth(l2)) / 2, 300);
	}

	/**
	 * Draw game over screen
	 */
	private void drawGameOverScreen(Graphics2D g) {
		g.setColor(Color.white);
		String mainMessage = message; // "Oh no..." 또는 "Congratulations!"
		FontMetrics fm = g.getFontMetrics();

		// 1. 주 메시지 출력
		g.drawString(mainMessage, (800 - fm.stringWidth(mainMessage)) / 2, 250);
		g.drawString("Press any key", (800 - fm.stringWidth("Press any key")) / 2, 300);

		// 2. 최고 점수 안내문 표시
		if (newHighScoreAchieved) {
			g.setColor(Color.YELLOW);
			g.setFont(new Font("Arial", Font.BOLD, 30));

			// message 변수가 이미 설정된 상태이므로, 'score' 변수는 아직 초기화되지 않은
			// 최종 점수 값을 가지고 있습니다. (notifyDeath/Win에서 score=0 전에 호출됨)
			String highMsg = "🎉 New High Score! (" + finalScore + ")";

			FontMetrics fm30 = g.getFontMetrics();
			// Y 좌표 400에 출력 (기존 메시지 아래)
			g.drawString(highMsg, (800 - fm30.stringWidth(highMsg)) / 2, 400);
		}

		// 폰트와 색상 복구 (선택 사항이지만 안전합니다)
		g.setColor(Color.white);
		g.setFont(new Font("Arial", Font.PLAIN, 12)); // 원래 폰트로 복구 (Game.java에서 기본 폰트 설정이 필요할 수 있음)
	}

	/**
	 * Handle player input during gameplay
	 */
	private void handlePlayerInput() {
		// 💡 1P 조작 (움직임과 발사)
		// ship 객체가 null이 아니고, 게임 플레이 상태이며 플레이어가 살아있을 때만 조작을 처리합니다.
		if (ship != null && playerHealth > 0 && !waitingForKeyPress && !pausePromptActive && !stageSelectActive) {
			// 움직임 처리
			ship.setHorizontalMovement(0);
			if (leftPressed && !rightPressed) {
				ship.setHorizontalMovement(-moveSpeed);
			} else if (rightPressed && !leftPressed) {
				ship.setHorizontalMovement(moveSpeed);
			}

			// 발사 처리
			if (firePressed) {
				tryToFireFrom(ship, 0);
			}
		}

		// 💡 2P 조작 (움직임과 발사)
		// ship2 객체가 null이 아니고, 게임 플레이 상태이며 플레이어가 살아있을 때만 조작을 처리합니다.
		if (ship2 != null && player2Health > 0 && !waitingForKeyPress && !pausePromptActive && !stageSelectActive) {
			// 움직임 처리
			ship2.setHorizontalMovement(0);
			if (leftPressed2 && !rightPressed2) {
				ship2.setHorizontalMovement(-moveSpeed);
			} else if (rightPressed2 && !leftPressed2) {
				ship2.setHorizontalMovement(moveSpeed);
			}

			// 발사 처리
			if (firePressed2) {
				tryToFireFrom(ship2, 1);
			}
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
		double stageDifficultyMultiplier = 1.0 - (currentStage * 0.1);
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
			if (getY() > 600 || getX() < 0 || getX() > 800) {
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
			if (stageSelectActive) {
				handleStageSelectInput(e);
				return;
			}

			if (waitingForKeyPress) {
				return;
			}

			if (handlePauseInput(e)) {
				return;
			}

			if (pausePromptActive) {
				return;
			}

			handlePlayerMovementInput(e);
			handleItemUsageInput(e);
		}

		/**
		 * Handle input during stage selection
		 */
		private void handleStageSelectInput(KeyEvent e) {
			int keyCode = e.getKeyCode();

			if (keyCode == KeyEvent.VK_LEFT) {
				selectedStage = Math.max(1, selectedStage - 1);
			} else if (keyCode == KeyEvent.VK_RIGHT) {
				int maxSelectableStage = Math.min(5, maxClearedStage + 1);
				selectedStage = Math.min(maxSelectableStage, selectedStage + 1);
			} else if (keyCode == KeyEvent.VK_ENTER) {
				currentStage = selectedStage;
				stageSelectActive = false;
				enemyLastFire = SystemTimer.getTime();
				startGame();
			} else if (keyCode == KeyEvent.VK_ESCAPE) {
				stageSelectActive = false;
				returnToMainMenu();
			}
		}

		/**
		 * Handle pause/resume input
		 * @return true if pause state was handled
		 */
		private boolean handlePauseInput(KeyEvent e) {
			int keyCode = e.getKeyCode();

			if (keyCode == KeyEvent.VK_ESCAPE && !waitingForKeyPress) {
				if (!pausePromptActive) {
					pausePromptActive = true;
				} else {
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

		/**
		 * Handle player movement and fire input
		 */
		private void handlePlayerMovementInput(KeyEvent e) {
			int keyCode = e.getKeyCode();

			// Player 1 controls
			if (keyCode == KeyEvent.VK_LEFT) leftPressed = true;
			else if (keyCode == KeyEvent.VK_RIGHT) rightPressed = true;
			else if (keyCode == KeyEvent.VK_SPACE) firePressed = true;

			// Player 2 controls
			else if (keyCode == KeyEvent.VK_A) leftPressed2 = true;
			else if (keyCode == KeyEvent.VK_D) rightPressed2 = true;
			else if (keyCode == KeyEvent.VK_W) firePressed2 = true;
		}

		/**
		 * Handle item usage input (keys 1-4)
		 */
		private void handleItemUsageInput(KeyEvent e) {
			if (itemManager == null) return;

			String itemIdToUse = getItemIdFromKey(e.getKeyCode());
			if (itemIdToUse == null) return;

			ItemManager.Effect eff = itemManager.use(itemIdToUse);
			syncItemCountsFromManager();

			if (eff == ItemManager.Effect.PLUS_LIFE) {
				applyHealthBoost();
			}
		}

		/**
		 * Map key code to item ID
		 */
		private String getItemIdFromKey(int keyCode) {
			switch (keyCode) {
				case KeyEvent.VK_1: return ItemManager.ID_AMMO;
				case KeyEvent.VK_2: return ItemManager.ID_DOUBLE_SCORE;
				case KeyEvent.VK_3: return ItemManager.ID_INVINCIBILITY;
				case KeyEvent.VK_4: return ItemManager.ID_PLUS_LIFE;
				default: return null;
			}
		}

		/**
		 * Apply health boost to players
		 */
		private void applyHealthBoost() {
			if (playerHealth > 0) {
				playerHealth = Math.min(playerMaxHealth, playerHealth + 1);
			}

			if (SettingsManager.isTwoPlayerEnabled() && player2Health > 0) {
				player2Health = Math.min(player2MaxHealth, player2Health + 1);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// if we're waiting for an "any key" typed then we don't
			// want to do anything with just a "released"
			if (waitingForKeyPress) {
				return;
			}
			// Don't process movement/fire if paused
			if (pausePromptActive) {
				return;
			}
			if (e.getKeyCode() == KeyEvent.VK_LEFT) {
				leftPressed = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
				rightPressed = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				firePressed = false;
			}
			//2p
			if (e.getKeyCode() == KeyEvent.VK_A) {
				leftPressed2 = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_D) {
				rightPressed2 = false;
			}
			if (e.getKeyCode() == KeyEvent.VK_W) {
				firePressed2 = false;
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {

			if (waitingForKeyPress) {
				char keyChar = Character.toLowerCase(e.getKeyChar());
				// 게임 조작 키는 "아무 키"에서 제외
				if (keyChar == 'w' || keyChar == 'a' || keyChar == 'd' || keyChar == ' ') {
					return;
				}

				if (waitingForKeyPress) {
					if (pressCount == 1) {
						// since we've now recieved our key typed
						// event we can mark it as such and start
						// our new game
						waitingForKeyPress = false;
						enemyLastFire = System.currentTimeMillis();
						startGame();
						pressCount = 0;
					} else {
						pressCount++;
					}
			}

			// ESC 키로 메뉴 복귀 (게임오버/승리 화면에서만)
			if (e.getKeyChar() == 27 && waitingForKeyPress) {
				returnToMainMenu();
			}
		}

		// --- Helper Methods for keyPressed ---

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
	}
}